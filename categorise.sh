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

  # Split on "_" and take 2nd field as LANGUAGE
  IFS=_ read -r part1 language rest <<< "$filename"

  # Skip files not matching expected pattern
  [[ -n "$language" && -n "$rest" ]] || continue

  mkdir -p -- "$target_dir/$language"
  mv -- "$f" "$target_dir/$language/"
done
