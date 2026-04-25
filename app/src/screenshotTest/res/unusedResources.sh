#!/bin/bash

# --- Configuration ---
input_file="unused_resources.txt"  # The file to read lines from
target_script="./commentOutResourceString.sh" # The script to call with each line

# --- Script Logic ---

# Check if the input file exists and is readable
if [ ! -f "$input_file" ]; then
  echo "Error: Input file '$input_file' not found."
  exit 1
fi

if [ ! -r "$input_file" ]; then
  echo "Error: Input file '$input_file' is not readable."
  exit 1
fi

# Check if the target script exists and is executable
if [ ! -f "$target_script" ]; then
  echo "Error: Target script '$target_script' not found."
  exit 1
fi

if [ ! -x "$target_script" ]; then
  echo "Error: Target script '$target_script' is not executable. Please use 'chmod +x $target_script'."
  exit 1
fi

echo "Reading lines from '$input_file' and passing them to '$target_script'..."

# Read the input file line by line
while IFS= read -r line; do
  echo "Processing line: '$line'"
  # Call the target script, passing the current line as an argument
  # Use double quotes around "$line" to handle lines with spaces correctly
  "$target_script" "$line"

  # Optional: Add a small delay if needed
  # sleep 1

  # Optional: Check the exit status of the target script
  # if [ $? -ne 0 ]; then
  #   echo "Warning: Target script exited with an error for line: '$line'"
  # fi

done < "$input_file"

echo "Finished processing all lines from '$input_file'."