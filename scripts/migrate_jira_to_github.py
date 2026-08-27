#!/usr/bin/env python3
"""One-time migration of Jira issues to GitHub Issues.

Reads issues (with comments and attachment metadata) from a Jira Cloud
project via ACLI (~/ACLI/acli jira) and recreates them as GitHub issues via
`gh`. Jira is treated as read-only: nothing is written back to it.

Usage:
    export JIRA_SERVER=https://sta2020.atlassian.net
    export JIRA_USER=you@example.com
    export JIRA_TOKEN=...        # https://id.atlassian.com/manage-profile/security/api-tokens

    # Sanity-check the JSON shape ACLI returns for one issue before a real run:
    python3 scripts/migrate_jira_to_github.py --inspect SA-1

    # Dry run over the whole project (no GitHub writes):
    python3 scripts/migrate_jira_to_github.py --dry-run

    # Real run:
    python3 scripts/migrate_jira_to_github.py
"""
import argparse
import csv
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

ACLI_PATH = os.environ.get("ACLI_PATH", os.path.expanduser("~/ACLI/acli"))
DEFAULT_REPO = "Scottish-Tech-Army/Soundscape-Android"
DEFAULT_PROJECT = "SA"
FALLBACK_LABEL = "jira-import"

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
ATTACHMENTS_DIR = SCRIPT_DIR / "migration_attachments"
MAPPING_CSV = SCRIPT_DIR / "migration_mapping.csv"
GITHUB_ISSUE_URL_RE = re.compile(r"https?://github\.com/([\w.\-]+)/([\w.\-]+)/issues/(\d+)")


def _read_token_file():
    candidates = [os.environ.get("JIRA_TOKEN_FILE")] if os.environ.get("JIRA_TOKEN_FILE") else []
    candidates += [REPO_ROOT / "acli-token.txt", REPO_ROOT / "acli-tokenl.txt"]
    for path in candidates:
        if path and Path(path).is_file():
            return Path(path).read_text().strip()
    return None


def jira_env():
    server = os.environ.get("JIRA_SERVER")
    user = os.environ.get("JIRA_USER")
    token = os.environ.get("JIRA_TOKEN") or _read_token_file()
    missing = [n for n, v in (("JIRA_SERVER", server), ("JIRA_USER", user), ("JIRA_TOKEN (or a token file)", token)) if not v]
    if missing:
        sys.exit(f"Missing required credential(s): {', '.join(missing)}")
    return server, user, token


def acli_jira(action, extra_args=None, server=None, user=None, token=None):
    cmd = [
        ACLI_PATH, "jira",
        "-a", action,
        "--server", server,
        "--user", user,
        "--token", token,
        "--quiet",
    ]
    if extra_args:
        cmd.extend(extra_args)
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        safe_cmd = [c if c != token else "***" for c in cmd]
        raise RuntimeError(f"acli {action} failed (exit {result.returncode}): {result.stderr.strip()}\ncommand: {' '.join(safe_cmd)}")
    return result.stdout


def parse_json(text, action):
    starts = [i for i in (text.find("{"), text.find("[")) if i != -1]
    if not starts:
        raise ValueError(f"No JSON found in acli {action} output:\n{text[:1000]}")
    idx = min(starts)
    return json.loads(text[idx:])


def first_present(d, *keys, default=None):
    for k in keys:
        if isinstance(d, dict) and k in d and d[k] not in (None, ""):
            return d[k]
    return default


def adf_to_text(node):
    """Best-effort flatten of Atlassian Document Format into markdown-ish text."""
    if node is None:
        return ""
    if isinstance(node, str):
        return node
    if isinstance(node, list):
        return "".join(adf_to_text(n) for n in node)
    if not isinstance(node, dict):
        return str(node)

    node_type = node.get("type")
    content = node.get("content", [])

    if node_type == "text":
        text = node.get("text", "")
        marks = node.get("marks", [])
        mark_types = {m.get("type") for m in marks}
        if "strong" in mark_types:
            text = f"**{text}**"
        if "em" in mark_types:
            text = f"*{text}*"
        if "code" in mark_types:
            text = f"`{text}`"
        link_mark = next((m for m in marks if m.get("type") == "link"), None)
        if link_mark:
            href = link_mark.get("attrs", {}).get("href", "")
            text = f"[{text}]({href})"
        return text
    if node_type in ("inlineCard", "blockCard", "embedCard"):
        return node.get("attrs", {}).get("url", "") + "\n\n"
    if node_type == "codeBlock":
        return "```\n" + adf_to_text(content) + "\n```\n\n"
    if node_type == "paragraph":
        return adf_to_text(content) + "\n\n"
    if node_type in ("heading",):
        level = node.get("attrs", {}).get("level", 1)
        return ("#" * level) + " " + adf_to_text(content) + "\n\n"
    if node_type == "bulletList":
        return "".join(f"- {adf_to_text(li.get('content'))}" for li in content) + "\n"
    if node_type == "orderedList":
        return "".join(f"{i+1}. {adf_to_text(li.get('content'))}" for i, li in enumerate(content)) + "\n"
    if node_type == "listItem":
        return adf_to_text(content)
    if node_type == "hardBreak":
        return "\n"
    return adf_to_text(content)


def jira_wiki_to_markdown(text):
    """Light conversion of common Jira Server/DC wiki markup to Markdown."""
    if not isinstance(text, str):
        return text
    text = re.sub(r"\{code(?::[^}]*)?\}", "```", text)
    text = re.sub(r"^h([1-6])\.\s*(.*)$", lambda m: "#" * int(m.group(1)) + " " + m.group(2), text, flags=re.MULTILINE)
    text = re.sub(r"\*([^\*\n]+)\*", r"**\1**", text)
    return text


def extract_description(fields):
    desc = first_present(fields, "description")
    if isinstance(desc, dict):
        return adf_to_text(desc).strip()
    return jira_wiki_to_markdown(desc or "")


def get_issue_keys(server, user, token, project, jql, limit):
    query = jql or f"project = {project} ORDER BY key ASC"
    args = ["--jql", query, "--outputType", "json"]
    if limit:
        args += ["--limit", str(limit)]
    data = parse_json(acli_jira("getIssueList", args, server, user, token), "getIssueList")
    if isinstance(data, dict):
        data = data.get("issues") or data.get("list") or []
    keys = []
    for row in data:
        if isinstance(row, str):
            keys.append(row)
        else:
            key = first_present(row, "key", "Key", "issueKey")
            if key:
                keys.append(key)
    return keys


ID_SUFFIX_RE = re.compile(r"\s*\([\w:.\-]+\)\s*$")


def strip_id_suffix(value):
    """ACLI renders users/statuses as e.g. 'Jane Doe (accountId)' or 'Done (10843)'."""
    if isinstance(value, str):
        return ID_SUFFIX_RE.sub("", value).strip()
    return value


def name_of(val):
    if isinstance(val, dict):
        return val.get("name") or val.get("displayName")
    return strip_id_suffix(val)


def to_name_list(value):
    """ACLI may return labels/components as a list of dicts/strings, or as a
    comma-separated string, or as an empty string when there are none."""
    if isinstance(value, list):
        return [n for n in (name_of(v) for v in value) if n]
    if isinstance(value, str) and value.strip():
        return [s.strip() for s in value.split(",") if s.strip()]
    return []


def get_issue(server, user, token, key):
    data = parse_json(acli_jira("getIssue", ["--issue", key, "--outputType", "json"], server, user, token), "getIssue")
    fields = data.get("fields", data)

    return {
        "key": first_present(data, "key", "issueKey", default=key),
        "summary": first_present(fields, "summary", default=key),
        "description": extract_description(fields),
        "status": name_of(first_present(fields, "status", default={})),
        "issuetype": name_of(first_present(fields, "issuetype", "issueType", default={})),
        "labels": to_name_list(first_present(fields, "labels", default=[])),
        "components": to_name_list(first_present(fields, "components", default=[])),
        "assignee": name_of(first_present(fields, "assignee", default=None)),
        "reporter": name_of(first_present(fields, "reporter", default=None)),
        "created": first_present(fields, "created"),
        "updated": first_present(fields, "updated"),
    }


def get_comments(server, user, token, key):
    data = parse_json(acli_jira("getCommentList", ["--issue", key, "--outputType", "json"], server, user, token), "getCommentList")
    if isinstance(data, dict):
        data = data.get("comments") or data.get("list") or []
    comments = []
    for c in data:
        author = first_present(c, "author", "authorDisplayName", "authorName")
        if isinstance(author, dict):
            author = author.get("displayName") or author.get("name")
        body = first_present(c, "body", "comment", default="")
        if isinstance(body, dict):
            body = adf_to_text(body).strip()
        comments.append({
            "author": author or "unknown",
            "created": first_present(c, "created", "createdDate", "date"),
            "body": jira_wiki_to_markdown(body),
        })
    return comments


def get_attachments(server, user, token, key):
    data = parse_json(acli_jira("getAttachmentList", ["--issue", key, "--outputType", "json"], server, user, token), "getAttachmentList")
    if isinstance(data, dict):
        data = data.get("attachments") or data.get("list") or []
    names = []
    for a in data:
        name = first_present(a, "filename", "fileName", "name")
        if name:
            names.append(name)
    return names


def download_attachments(server, user, token, key, filenames):
    if not filenames:
        return
    out_dir = ATTACHMENTS_DIR / key
    out_dir.mkdir(parents=True, exist_ok=True)
    for name in filenames:
        dest = out_dir / name
        try:
            acli_jira("getAttachment", ["--issue", key, "--name", name, "--file", str(dest)], server, user, token)
        except RuntimeError as e:
            print(f"  ! failed to download attachment {name} for {key}: {e}", file=sys.stderr)


DONE_STATUSES = {"done", "closed", "resolved"}


def gh(args, dry_run=False, capture=True):
    cmd = ["gh"] + args
    if dry_run:
        print(f"  [dry-run] gh {' '.join(args)}")
        return ""
    result = subprocess.run(cmd, capture_output=capture, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"gh {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def existing_labels(repo):
    out = subprocess.run(
        ["gh", "label", "list", "--repo", repo, "--json", "name", "--limit", "1000"],
        capture_output=True, text=True,
    )
    if out.returncode != 0:
        raise RuntimeError(f"gh label list failed: {out.stderr.strip()}")
    return {row["name"].lower(): row["name"] for row in json.loads(out.stdout)}


def ensure_fallback_label(repo, dry_run):
    labels = existing_labels(repo)
    if FALLBACK_LABEL.lower() not in labels:
        gh(["label", "create", FALLBACK_LABEL, "--repo", repo,
            "--color", "5319e7", "--description", "Imported from Jira", "--force"], dry_run=dry_run)
    return labels


def map_labels(issue, existing, repo):
    candidates = [issue["issuetype"]] + issue["labels"] + issue["components"]
    mapped = []
    for c in candidates:
        if not c:
            continue
        match = existing.get(c.lower())
        if match:
            mapped.append(match)
    if not mapped:
        mapped.append(FALLBACK_LABEL)
    return sorted(set(mapped))


def build_body(issue, attachment_names, server):
    lines = [issue["description"] or "_(no description)_", ""]
    if attachment_names:
        lines.append("**Original Jira attachments (not auto-uploaded, saved locally):** " + ", ".join(attachment_names))
        lines.append("")
    lines.append("---")
    lines.append(f"Migrated from Jira [{issue['key']}]({server}/browse/{issue['key']})")
    lines.append(f"- Reporter: {issue['reporter'] or 'unknown'}")
    lines.append(f"- Assignee: {issue['assignee'] or 'unassigned'}")
    lines.append(f"- Type: {issue['issuetype'] or 'unknown'}")
    lines.append(f"- Created: {issue['created'] or 'unknown'}")
    lines.append(f"- Updated: {issue['updated'] or 'unknown'}")
    return "\n".join(lines)


def find_linked_github_issue(description):
    """If a Jira issue's description is just a reference to an existing GitHub
    issue, return (owner/repo, number) for that issue, else None."""
    m = GITHUB_ISSUE_URL_RE.search(description or "")
    if not m:
        return None
    owner, repo_name, number = m.groups()
    return f"{owner}/{repo_name}", number


def post_comments(comments, target_repo, target_number, dry_run):
    for c in comments:
        comment_body = f"**{c['author']}** commented on {c['created']} (migrated from Jira):\n\n{c['body']}"
        gh(["issue", "comment", str(target_number), "--repo", target_repo, "--body", comment_body], dry_run=dry_run)
        time.sleep(0.3)


def migrate_issue(key, server, user, token, repo, existing_labels_map, dry_run):
    issue = get_issue(server, user, token, key)
    comments = get_comments(server, user, token, key)

    linked = find_linked_github_issue(issue["description"])
    if linked:
        target_repo, target_number = linked
        print(f"  {key} references existing {target_repo}#{target_number} -> adding {len(comments)} comment(s) only")
        post_comments(comments, target_repo, target_number, dry_run)
        return issue, f"{target_repo}#{target_number}", "linked-existing"

    attachment_names = get_attachments(server, user, token, key)
    download_attachments(server, user, token, key, attachment_names)

    labels = map_labels(issue, existing_labels_map, repo)
    body = build_body(issue, attachment_names, server)

    body_file = ATTACHMENTS_DIR / f"{key}.body.md"
    body_file.parent.mkdir(parents=True, exist_ok=True)
    body_file.write_text(body)

    create_args = ["issue", "create", "--repo", repo, "--title", issue["summary"],
                   "--body-file", str(body_file)]
    for label in labels:
        create_args += ["--label", label]

    out = gh(create_args, dry_run=dry_run)
    if dry_run:
        print(f"  body preview -> {body_file}")
        number = None
    else:
        url = out.strip().splitlines()[-1]
        number = url.rstrip("/").rsplit("/", 1)[-1]

    target = number if number else "<new-issue>"
    post_comments(comments, repo, target, dry_run)

    if (issue["status"] or "").lower() in DONE_STATUSES:
        gh(["issue", "close", str(target), "--repo", repo], dry_run=dry_run)

    return issue, number, "dry-run" if dry_run else "created"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--repo", default=DEFAULT_REPO)
    parser.add_argument("--project", default=DEFAULT_PROJECT)
    parser.add_argument("--jql", default=None, help="Override the JQL used to select issues")
    parser.add_argument("--limit", type=int, default=None, help="Cap number of issues fetched (testing)")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--exclude", default="", help="Comma-separated Jira keys to skip entirely (e.g. contain PII not fit for a public repo)")
    parser.add_argument("--inspect", metavar="ISSUE_KEY", help="Dump raw acli JSON for one issue and exit")
    args = parser.parse_args()

    server, user, token = jira_env()

    if args.inspect:
        key = args.inspect
        print("=== getIssue ===")
        print(acli_jira("getIssue", ["--issue", key, "--outputType", "json"], server, user, token))
        print("=== getCommentList ===")
        print(acli_jira("getCommentList", ["--issue", key, "--outputType", "json"], server, user, token))
        print("=== getAttachmentList ===")
        print(acli_jira("getAttachmentList", ["--issue", key, "--outputType", "json"], server, user, token))
        return

    keys = get_issue_keys(server, user, token, args.project, args.jql, args.limit)
    excluded = {k.strip() for k in args.exclude.split(",") if k.strip()}
    if excluded:
        skipped = [k for k in keys if k in excluded]
        keys = [k for k in keys if k not in excluded]
        print(f"Excluding {len(skipped)} issue(s) per --exclude: {', '.join(skipped)}")
    print(f"Found {len(keys)} issue(s) in Jira project {args.project} to migrate")

    existing_labels_map = ensure_fallback_label(args.repo, args.dry_run)
    if args.dry_run:
        existing_labels_map = existing_labels(args.repo)

    ATTACHMENTS_DIR.mkdir(parents=True, exist_ok=True)
    write_header = not MAPPING_CSV.exists()
    with open(MAPPING_CSV, "a", newline="") as f:
        writer = csv.writer(f)
        if write_header:
            writer.writerow(["jira_key", "github_issue_number", "status", "error"])

        for key in keys:
            print(f"Migrating {key}...")
            try:
                issue, number, status = migrate_issue(key, server, user, token, args.repo, existing_labels_map, args.dry_run)
                writer.writerow([key, number or "", status, ""])
                f.flush()
            except Exception as e:
                print(f"  ! failed: {e}", file=sys.stderr)
                writer.writerow([key, "", "error", str(e)])
                f.flush()
            if not args.dry_run:
                time.sleep(0.5)

    print(f"Done. Mapping written to {MAPPING_CSV}")


if __name__ == "__main__":
    main()
