convert \( -background transparent -fill white -weight Bold -pointsize 60 label:"Soundscape" \) \( -background transparent -fill white -weight 400 -pointsize 24 label:"with map data from" \) \( -background transparent -fill white -weight Bold -pointsize 40 label:"OpenStreetMap" \) -gravity Center -background transparent -append app/src/main/res/drawable/ic_licenses.png
convert app/src/main/res/drawable/ic_licenses.png -trim +repage  -resize 180x180  -gravity center  -background none -extent 288x288   PNG32:app/src/main/res/drawable/round_licenses.png

# iOS launch screen and in-app splash assets. Rendered at high pointsize then
# downsampled to each scale so the @1x/@2x/@3x triplet stays crisp; the inner
# content is sized to ~62% of the canvas to match the round_licenses padding
# ratio used on Android, so the two splashes look visually consistent. Target
# on-screen size is 200pt.
HIRES=$(mktemp -t soundscape_logo_hires).png
# ImageMagick 7 on macOS can no longer resolve fonts by family name (its
# `-list font` config is empty), so point at the system font file directly.
FONT=/System/Library/Fonts/HelveticaNeue.ttc
convert -font "$FONT" \( -background transparent -fill white -weight Bold -pointsize 180 label:"Soundscape" \) \( -background transparent -fill white -weight 400 -pointsize 72 label:"with map data from" \) \( -background transparent -fill white -weight Bold -pointsize 120 label:"OpenStreetMap" \) -gravity Center -background transparent -append "$HIRES"

ASSET_DIR=iosApp/iosApp/Assets.xcassets/SoundscapeLogo.imageset
rm -f "$ASSET_DIR/round_licenses.png" "$ASSET_DIR"/SoundscapeLogo*.png
for scale in 1 2 3; do
    canvas=$((200 * scale))
    content=$((canvas * 5 / 8))
    case $scale in
        1) suffix="";;
        *) suffix="@${scale}x";;
    esac
    convert "$HIRES" -trim +repage -resize "${content}x${content}" -gravity center -background none -extent "${canvas}x${canvas}" "PNG32:$ASSET_DIR/SoundscapeLogo${suffix}.png"
done
rm -f "$HIRES"