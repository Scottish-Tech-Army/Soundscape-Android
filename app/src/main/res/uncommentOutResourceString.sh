# Iterate over all directories starting 'values' and comment out the resource
# passed in as the argument in the strings.xml file.
for dir in ./values*; do (
  cd "$dir" &&
  if [ -f strings.xml ]; then
    sed -ri "s/<!--[\t ]*(<[\t ]*string[\t ]*name[\t ]*=[\t ]*\"$1\".*)[\t ]-->/  \1/" strings.xml
  fi
); done
