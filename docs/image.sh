for directory in */ ;
do
  class=${directory::-1}
  echo "<h1>Screenshots from $class</h1><p>" > $class.html
  cd $class
  for filename in *;
  do
    echo "<p class=\"image-name\">$filename</p><a href=\"$class/$filename\"><img src=\"$class/$filename\" alt=\"$filename\" width=\"100\"></a>">> ../$class.html
  done
  cd ..
done