#!/usr/bin/env bash

# Use first argument as target directory, default to current directory
target_dir="${1:-.}"

# Ensure it's a valid directory
if [[ ! -d "$target_dir" ]]; then
  echo "Error: '$target_dir' is not a directory"
  exit 1
fi

shopt -s nullglob

for f in "$target_dir"/*; do
  [[ -f "$f" ]] || continue

  filename=$(basename -- "$f")

  # Filenames are <Test>_<Preview name>_<hash>_<n>.png. The preview name is a
  # language code that can itself contain "_" (pt_BR, zh_Hans), so take
  # everything between the test name and the hash rather than one field.
  [[ "$filename" =~ ^[^_]+_(.+)_[0-9a-f]+_[0-9]+\.png$ ]] || continue
  language="${BASH_REMATCH[1]}"

  mkdir -p -- "$target_dir/$language"
  mv -- "$f" "$target_dir/$language/"
done
