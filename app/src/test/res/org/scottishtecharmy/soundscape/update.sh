for x in $(seq 15990 15992);
do
  for y in $(seq 10212 10214);
  do
    wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/$x/$y.pbf -O ${x}x${y}.mvt
  done
done

wget https://d1wzlzgah5gfol.cloudfront.net/protomaps/15/16093/10211.pbf -O 16093x10211.mvt
