Here are some ideas on how best to debug GeoJSON. This was inspired by Adam's use of GeoJSON.io, and my laziness with copy and paste!

My workflow is as follows:

1. Generate a geojson output file from a unit test (for example see `testVectorToGeoJsonMilngavie` which generates `milngavie.geojson`). The filename must have the suffix `.geojson`.
2. Upload the file as a github gist. This is how I do it, first of the following 3 steps are done once, and then the 4th one can be repeated each time the GeoJSON is updated.
   1. Install the github client program `gh`
   2. Upload the gist e.g. `gh gist create app/milngavie.geojson`
   3. Get the ID for the new gist with `gh gist list` (referred to as `XXX_GIST_ID_XXX` in the following examples)
   4. When the file changes, upload a new version of it e.g. `gh gist edit XXX_GIST_ID_XXX app/milngavie.geojson`
3. Open the gist either
   1. In github (click on the user avatar and select _Your Gists_) which overlays the GeoJSON on Bing Maps, or
   2. In geojson.io by opening the URL `https://geojson.io/#id=gist:USERNAME/XXX_GIST_ID_XXX

The only limitation of this method is that the gist must smaller than 1MByte. Otherwise, it's a very quick way of viewing GeoJSON without having to copy or paste any text.