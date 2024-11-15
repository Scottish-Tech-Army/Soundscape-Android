protomaps_server=https://d1wzlzgah5gfol.cloudfront.net/protomaps
# Tiles for testVectorToGeoJsonGrid
for x in $(seq 15990 15992);
do
  for y in $(seq 10212 10214);
  do
    wget $protomaps_server/15/$x/$y.pbf -O ${x}x${y}.mvt
  done
done

# Tiles for intersectionsStraightAheadType (-2.6577997643930757, 51.43041390383118)
# and for intersectionsSideRoadRight (-2.656109007812404,51.43079699441145)
# and for intersectionsSideRoadLeft (-2.656530323429564,51.43065207103919)
# and for intersectionsT1Test (-2.656540700657672,51.430978147982785)
for x in $(seq 16141 16142);
do
  for y in $(seq 10906 10907);
  do
    wget $protomaps_server/15/$x/$y.pbf -O ${x}x${y}.mvt
  done
done

# Tiles for intersectionsStraightAheadType (-2.6577997643930757,51.43041390383118)
# and for intersectionsT2Test (-2.637514213827643,51.472589063821175)
for x in $(seq 16143 16144);
do
  for y in $(seq 10900 10901);
  do
    wget $protomaps_server/15/$x/$y.pbf -O ${x}x${y}.mvt
  done
done


# Tiles for intersectionsRightTurn (-2.615585745757045,51.457957257918395)
# and for intersectionsLeftTurn (-2.6159411752634583,51.45799104056931)
# and for intersectionsCross1Test (-2.61850147329568,51.456953686378085)
# and for intersectionCross2Test (-2.6176822011131833,51.457104175295484)
for x in $(seq 16145 16146);
do
  for y in $(seq 10902 10904);
  do
    wget $protomaps_server/15/$x/$y.pbf -O ${x}x${y}.mvt
  done
done

# Tiles for intersectionsCross3Test (-0.9752549546655587,51.4553843453491)
for x in $(seq 16294 16295);
do
  for y in $(seq 10903 10904);
  do
    wget $protomaps_server/15/$x/$y.pbf -O ${x}x${y}.mvt
  done
done

# Tiles for intersectionsLoopBackTest (-122.03856292573965,37.33916628666543)
# Enable once we have a planet wide map
#for x in $(seq 5275 5276);
#do
#  for y in $(seq 12715 12716);
#  do
#    wget $protomaps_server/15/$x/$y.pbf -O ${x}x${y}.mvt
#  done
#done

wget $protomaps_server/15/16093/10211.pbf -O 16093x10211.mvt
