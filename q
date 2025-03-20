[33mcommit c3fdf932e5079401bd68494c525333ae32102d09[m[33m ([m[1;36mHEAD[m[33m -> [m[1;32mmain[m[33m, [m[1;33mtag: [m[1;33msoundscape-0.0.65[m[33m, [m[1;31mupstream/main[m[33m, [m[1;31morigin/main[m[33m, [m[1;31morigin/HEAD[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Thu Mar 20 10:42:41 2025 +0000

    Bump version to 0.0.65, version code 66

[33mcommit 4e73e0be51735033435f4921e3fd3053841b6ad9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 20 10:18:24 2025 +0000

    Fix StreetPreview behaviour to work with NearestRoad changes
    
    The NearestRoadFilter uses the direction of travel along with the location
    to determine the nearest road. If we provide the direction of travel to the
    StaticLocationProvider from the StreetPreview go function then we can indicate
    which road we arrived at the intersection on. The direction of travel is
    always the heading of the road that was travelled along.

[33mcommit ee4e399bb56d856d3f2c8a375fe904a913a6a2a6[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.64[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Wed Mar 19 17:14:39 2025 +0000

    Bump version to 0.0.64, version code 65

[33mcommit fcd6ca0f0ffacf551d0db4748ed0037213ae985d[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 17:00:03 2025 +0000

    Remove spurious logging in MvtTileTest

[33mcommit 91f502667ea603ee903a8ef493787922cf79c4fe[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 16:56:53 2025 +0000

    Remove Logging during OSM style extraction

[33mcommit f365485d4434c9a27971ab15a46ebf78a954e2d4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 16:49:50 2025 +0000

    Update MVT unit test tiles with new protomaps server build

[33mcommit 642c94410b0194b476a028cfeb2ec9ddf5402849[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 3 16:22:23 2025 +0000

    Add osm-liberty-accessible directory for new Map style
    
    - Remove 4x scaleup in our map view code as the style should scale itself
    - Removed relief layer from osm-liberty style
    - Reduced number of Roboto fonts used
    - Disable tilt gesture so as to keep the map 2D
    - Remove osm-bright-gl style

[33mcommit 00d4fe58684f64ee8447873f5f957c2a5f861159[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 13:11:02 2025 +0000

    Handle interior rings when merging polygons
    
    The polygon merging code was failing when the merge resulted in a new interior ring.
    It also wasn't handling inner rings on the polygons to be merged. I've fixed that
    and added a test for it.

[33mcommit 2dd613f6b6f8fb678ec6c5cc807bee2a62a2afc7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 11:41:26 2025 +0000

    Extend bus stop logic to include other types of public transport
    
    Include tram stops, subway, train stations and ferry terminals.

[33mcommit 50bbb8eff022da0e92c79c2aa2486f1a0c82eb57[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 11:01:17 2025 +0000

    Revert "Auto-generate markdown document from Help strings in app"
    
    This reverts commit b1e0f7d0aea65a9b895e65cdda92e9efea4826ca.
    
    Adding robolectric seemed to break much of the syntax checking in Android
    Studio :-(

[33mcommit 969ca7a5ed19195a38a4b840aae5632eb8f5b629[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 19 09:30:14 2025 +0000

    Add intersections folder to PlacesNearby
    
    The aim here is to make it very easy to add markers at intersections. This may
    require some tweaking, possiblities include
    
    - Alter the format of the naming of intersections
    - Always start the intersection name with a road name, rather than a generic
    path/service etc.
    - Filter out intersections with no named road?

[33mcommit b1e0f7d0aea65a9b895e65cdda92e9efea4826ca[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Mar 18 20:52:59 2025 +0000

    Auto-generate markdown document from Help strings in app
    
    The new HelpGenerationTest unit test generates help.md in docs. The
    nightly build has been altered so that it commits any changes to help.md.
    That will then end up in the standard docs build.

[33mcommit 940530be10f33b9a591c434d1a8dd248d9e40a7b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Mar 18 19:32:55 2025 +0000

    Add "Marker Created" speech when marker is created in database
    
    The localized string is past from the @Composable down into the ViewModel
    where it is spoken when the database access happens.

[33mcommit 632964f3a1ea492032669e6b698eddfe958b798e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Mar 18 11:38:18 2025 +0000

    Add route end callout and improve marker callouts
    
    The callouts for RoutePlayer have been changed to match those in iOS, and
    when the last Marker is reached the RoutePlayer is stopped.
    Also, features which are at the same location as a Marker are no longer called
    out by auto-callout.

[33mcommit c562d0ad03c0643c9cadef903476eb036e2d9c92[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 24 14:45:18 2025 +0000

    Add nearest road filter to better track the current road
    
    This change adds a simple algorithm which calculates the nearest road on every
    location update. It uses the current direction of travel as well as the current
    location along with a bit of stickiness to the previous nearest road. The main
    issue this aims to resolve is calling out the wrong nearest road when crossing
    a side road, and also to ensure that intersections are called out from the correct
    perspective.
    The amount of Kalman filtering on the location has been increased slightly. This
    increases the location lag, but with the benefit of more accurate location.

[33mcommit be86d1ee95f0b8bf186b24c86792a2e80736bd12[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 17 11:23:26 2025 +0000

    Skip importing any un-named gardens from MVT tile
    
    This commit simply skips adding any POI that is an un-named garden. There's no
    value in calling out the garden of every house. See #448.

[33mcommit ba065406d74a30e1e8dde53850a0e4b420aa51d5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 17 11:00:35 2025 +0000

    Add a tile of Glasgow West End to aid debugging
    
    This tile has lots of private gardens mapped along with footways marked as
    sidewalk running next to roads. Both of these will require handling in our
    code.

[33mcommit 7abc0f009b0dd0a9c9ac23f318dd24b3534f210f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 17 10:12:07 2025 +0000

    Don't callout "Unknown Place" from reverseGeocode
    
    It's not useful to be told that we're in an "Unknown Place", so don't
    call it out.

[33mcommit fa03dda1572cb136c9fa7552d8277689939d5183[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.63[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Fri Mar 14 17:37:08 2025 +0000

    Bump version to 0.0.63, version code 64

[33mcommit 2032c68fd816c773dc172968fe2061137c0637ae[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 17:26:22 2025 +0000

    Fix crash in callouts when travelling by train...
    
    When travelling at over 5m/s the callouts were crashing. Just a lack of
    error handling during localReverseGeocode.

[33mcommit 5a39e446c9e9c1ddfd56bb8e7fba1c7923294dec[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 17:17:14 2025 +0000

    Use country as well as language when matching locale
    
    The code to show the default language in the onboarding screen was only
    using the phone language and not the country. As a result on UK English
    phones it was showing English (US) as the selected language. This change
    adds the country into the mix. If the country isn't supported, the language
    selected will be the first in the list i.e. French from France, non-UK English
    and Portuguese from Portugal.
    If the system language isn't supported, then indexOfLanguageMatchingDeviceLanguage
    returns -1 which forces the user to select a language in the UI.

[33mcommit 8ac6b5d5820f37bc0d1a9dc296e262519d352b34[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 12:14:52 2025 +0000

    Update link in testing doc

[33mcommit 37cac90936ab3914581ff40b58a1174b7e1a11e6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 12:02:33 2025 +0000

    Update release notes

[33mcommit cd2259175c01d6aa33c6f79fba27ab4a428f5f50[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 11:45:14 2025 +0000

    Update STA testing doc with Slack channel link

[33mcommit 6b73bb29026cc16f3abc3876344e5b858fbb5e6f[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.62[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Fri Mar 14 10:49:27 2025 +0000

    Bump version to 0.0.62, version code 63

[33mcommit 7a3d4b334504b28262ef057b13a89577a0799541[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 10:47:59 2025 +0000

    Fix crash introduced by change in generateNearestFeatureCollection

[33mcommit ce44c79de3e4e5c089341651bb9343ca80e23e24[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.61[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Fri Mar 14 10:24:21 2025 +0000

    Bump version to 0.0.61, version code 62

[33mcommit 816dc08acf5d2ae6391c8ed32ef08e72a177ee32[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 10:10:42 2025 +0000

    Move markerTree into GridState and use it in Auto Callouts
    
    The markerTree which is updated from the Realm database is now inside the
    GridState class. This means that it can be easily used by callouts. Markers
    in the tree now have their category set to "marker" which allows them to be
    identified during callouts. The auto callout adds Markers into it's search
    which should behave similarly to the iOS app.

[33mcommit 5d49b888d7488a73b742bf8fe1bbd4e52e987e9e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 14 10:09:11 2025 +0000

    Extend generateNearestFeatureCollection to add in initial collection
    
    generateNearestFeatureCollection can now take a FeatureCollection as an
    argument which is sorted in with the results from the search. This makes
    it easier to combine searches on separate trees e.g. Markers and POIs.

[33mcommit ef1bcb3e23fa98b5e69f639fc18c1b1a8cd1e7a0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 16:55:51 2025 +0000

    Fix intersection callouts to have audio in correct direction
    
    This change means that intersection callouts are now closer to the iOS
    behaviour. Roads are called out with audio located in their rough direction.

[33mcommit c9d121868380af2024d8e285c3ace8f49f14d001[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 16:54:23 2025 +0000

    Fix break to StreetPreview in commit 7b13f2cbc90831e3e2a149043100e5cd7a4bb4eb
    
    Accidentally made StreetPreview automatically jump forwards...oops.

[33mcommit ca89f2f4b346d52bf28a83efa38d2267f477cfa3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 14:13:57 2025 +0000

    Remove bogus Boolean return values in Previews

[33mcommit 446430fa5041781ec38f3c78bfcca695450d5fad[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.60[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Thu Mar 13 14:07:38 2025 +0000

    Bump version to 0.0.60, version code 61

[33mcommit 0df91729b0474772b1a2fa1a47d61c05bc1a06fb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 13:15:22 2025 +0000

    Make AheadOfMe behave like iOS
    
    AheadOfMe calls out first 5 POI in the direction that the user is heading.

[33mcommit a0d5012658bccaae6d33343877d0f7cb83be71ac[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 12:16:03 2025 +0000

    Make buttons on Location Details screen meet minimum height
    
    The minimum size of any UI should be spacing.targetSize.

[33mcommit e086fbdfd542b2e41a5a081b40271b344c644cf2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 12:03:26 2025 +0000

    Fix crash when adding marker from Places Nearby
    
    With no markers in the database, creating a route and adding a marker
    from within AddWaypoints was causing a crash. The problem was that the
    code in createAndAddMarker was assuming that createMarker had update the
    objectId in locationDescription, but that update was happening on a
    different thread.
    The change means that the markers from the database are constantly
    monitored. If a new marker appears during the lifetime of the ViewModel
    it can only have been added via Add Waypoints and so it's added to the
    route. This makes the update asynchronous which is as it should be.

[33mcommit d9b260f7baaeaed7edfe24c08acfe7d5b8bb2ce4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 11:22:45 2025 +0000

    Start refactor of getCurrentUserGeometry
    
    Some of the user geometry creation logic was only happening in the geometry
    created for the audio engine. This change shares that code so that we have
    a common set of logic. In future, the aim is to have a single collect working
    on the Location/Orientation which would result in a flow for UserGeometry
    which could be consumed elsewhere in the system. Currently, the Location flow
    makes it all the way into the UI.

[33mcommit 3a4785f930c2de97711ddc16c5ea1ee59d843ae7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 09:51:30 2025 +0000

    Remove ActivityRecognition code as it was unreliable
    
    The Android ActivityRecognition API was very delayed in detecting an
    activity and appeared unreliable. It also required us to meet different
    app requirements as using that API makes the app a "Health" app.
    This change removes its use along with the Otto library which we were
    using to pass around its events. In its place we just check if the
    speed is over 5m/s. Speed will require some smoothing as it's
    currently a bit jumpy and we don't use the speed error provided by
    the location provider.

[33mcommit 7b13f2cbc90831e3e2a149043100e5cd7a4bb4eb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 09:13:05 2025 +0000

    Fix race in StreetPreview startup
    
    The StreetPreview state was being set after the GeoEngine was started. This
    meant there was a race between the state being set and the call to
    tileGridUpdated which used the state. A simple fix, plus adjust the UI
    buttons to be more sensibly sized.

[33mcommit d5c4af6590da4501634fb081f1e7c7692489d560[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 13 08:47:29 2025 +0000

    Move StreetPreview exit button so that it appears whilst "Getting ready"
    
    This makes it easier to exit StreetPreview if there's a problem in
    initialization.

[33mcommit 79bc4f95b95b7442e758338580a8386c691b37ae[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 12 12:59:21 2025 +0000

    Update release notes

[33mcommit 0ec041acf32d137da1ffba58089a7cd05247fefb[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.59[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Wed Mar 12 12:39:08 2025 +0000

    Bump version to 0.0.59, version code 60

[33mcommit 733f07189e18315136d8af82bf4b15f3c8f325c3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 12 12:38:23 2025 +0000

    Add Places Nearby hierarchy to Add Waypoints screen (#424)
    
    * Add instructions document for initial testing
    
    * Fix one time crash in emulator on textToSpeech startup
    
    In updateSpeech, textToSpeech.voices was null. It was called from onInit and so
    the TextToSpeech had been initialized successfully. Protect the use of voice by
    checking for null.
    
    * Remove de-duplication in PlacesNearby now that polygons are merged
    
    There should no longer be any duplication, so we don't need a special
    case to handle it.
    
    * Move PlacesNearby viewmodel logic into separate file for sharing
    
    We need to implement PlacesNearby in AddWaypoints, so share the code
    from the viewmodel.
    
    * Add PlacesNearby to Add Waypoints screen
    
    I'm not sure about how tidy this is, but it does seem to do what we want.
    The PlacesNearby hierarchy now appears in AddWaypoints, and selecting any
    POI will pop up the SaveMarker screen. Saving the marker will save the
    marker and automatically add it to the route.
    
    * Fix URL to be a link in testing doc and add requirements section

[33mcommit 8932677e7b64b8062601da475a6ef53c4f075bd1[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.58[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Tue Mar 11 13:00:26 2025 +0000

    Bump version to 0.0.58, version code 59

[33mcommit 9684a4bca68ca8e7e34c9b35ac8a0207e3f6c07a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Mar 11 12:59:10 2025 +0000

    Reinstate Adam's polygon merging code with a bit of refactoring (#423)
    
    * Reinstate Adam's polygon merging code with a bit of refactoring
    
    I refactored mergeAllPolygonsInFeatureCollection a little so that it would
    merge all overlapping duplicates of a polygon into a single one. It seems to
    work well, so I've re-enabled it to see what happens.
    
    * Fix up using speed accuracy in GeoEngine
    
    Having gone to the trouble of using the speed accuracy, I was then
    throwing it away.
    
    * Reduce splash screen display timeout from 3 to 2 seconds.

[33mcommit 77556ba8f6986fbd2ad1a1903a7cac21f318756c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Mar 11 11:29:01 2025 +0000

    Improve audio beacon dimming (#422)
    
    * Fix dimming audio so that it only affects beacons
    
    This change creates two FMOD ChannelGroups, one for beacons and one for
    speech and earcons. The audio dimming due to unknown heading is only done
    for beacons.
    
    * Reduce false readings of travelHeading
    
    This needs more work! With my phone sitting in my office locked and on its
    side a playing beacon will un-dim every so often when a Location comes in
    that suggests that it has a speed and a heading. Can we Kalman filter this
    away? I would have expected the FusedProvider to know that there's no movement
    taking place as it's looking at the accelerometers as well as the GPS, but
    perhaps I'm missing something?
    
    * Discard polygons from MVT where all points are outwith the tile
    
    This is a simple de-duplication which discards polygons where all of the
    points are outwith the tile. PoiTest updated to reflect the fewer POIs.

[33mcommit 7c21182fa683bcf94e4c0203043ee1b0fd05442f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 10 14:47:09 2025 +0000

    List colors didn't match the theme, so switch to onSurface on surface

[33mcommit 33cbaafeac39fe60e73bb143a1075185c4756c62[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 10 13:40:11 2025 +0000

    Add filter folders to PlacesNearbyList

[33mcommit b15399becee94574fbf4f75928d8286c16b7ae5f[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.57[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Fri Mar 7 12:59:46 2025 +0000

    Bump version to 0.0.57, version code 58

[33mcommit 7d973511624c9ac135fe7cb078b594de85efc085[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 7 12:57:51 2025 +0000

    Remove licenses from Talkback
    
    The license text is verbose and hard to navigate, it's better to skip it
    for Talkback users.

[33mcommit bb95c1456e8bf933cbad498002fead996543d325[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 7 12:38:13 2025 +0000

    Fix Talkback for route skip/previous/details/mute buttons

[33mcommit a4c7d2de854f8de35035cdaa5f849880bece0fec[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 7 12:22:34 2025 +0000

    Refactor Markers and Routes screens to reuse list and state code
    
    A LocationDescription can be created that represents a route, so the list
    code can be made the same for both Routes and Markers. This removes a lot
    of duplication leaving only the view models being different.

[33mcommit 52c578dc40e6759dfb2c6e38d2c2f7d8c0c4ce14[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 7 09:40:13 2025 +0000

    Marker and Routes list color improvements

[33mcommit 6a7b3efaa0af72f472882e9317ddf2fe7349edfc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Mar 7 09:02:18 2025 +0000

    Add Done and Add buttons to app bar
    
    The main aim of this change is to move the Done and Add buttons in the
    Route screens into the app bar. This involves some changes to the app bar
    as well as the call sites.

[33mcommit 7fcc14d93f3cf8eb53b1e77563fd18c3117ee871[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 16:10:06 2025 +0000

    Add talkbackHidden to make composable hidden from Talkback

[33mcommit ad41716993eff0ba1cfc2b3df15e3cd0a00413d0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 12:11:36 2025 +0000

    Add route reordering via Talkback interface
    
    This uses the suggested logic from https://github.com/Calvin-LL/Reorderable

[33mcommit d1a9144dfb475b695df6513386b3fe6ef110b9ca[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 11:47:09 2025 +0000

    Add waypoint index to description in list

[33mcommit 9f83356412fa7365cb67406a6bf7b0943e73604a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 11:29:20 2025 +0000

    Improve AddWaypoints Talkback and GUI
    
    This change moves the click action out of the Switch and into the Row. It
    updates the contentDescription and hint to reflect the current state of the
    Switch to match the behaviour in iOS.

[33mcommit 2907dcd71237cc9b4625cfcabcc6169b1cbc8e0a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 11:28:17 2025 +0000

    Update bitmap creation based on AndroidStudio suggestions

[33mcommit 4026e6f1b6530c06fd245ba3285a2898ca4615f3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 10:04:56 2025 +0000

    Material target size guidance for accessibility is 48dp

[33mcommit c3d592fad0b6f6a0303095e319979317d94ee291[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 09:49:21 2025 +0000

    Enable gestures for menu drawer
    
    Without gestures enabled the behaviour is confusing - especially with
    Talkback.

[33mcommit db0d2eac9eb0d8a3d9551d924aaa9d5d20ed19e2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Mar 6 09:48:44 2025 +0000

    Use same text as iOS for search bar description

[33mcommit b6ea8176f6e1c7007dde77aa48dcf1f6935efb1f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 5 11:03:17 2025 +0000

    Improve TalkBack support of listPreferences in Settings screen

[33mcommit f7d06a1d9210f2d02b2a7442824e205dd9c23b89[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 5 09:49:37 2025 +0000

    Make beacon quieter when there's no heading available
    
    In the case of no heading - standing still with the phone locked and
    not held flat - the beacon is lowered in volume and set to the sound
    as if it were behind.

[33mcommit d975cede06128e1f3d7117b5a8ad4ae3c4cb51ee[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Mar 5 09:15:20 2025 +0000

    Move OSM attribution to splash screen
    
    Attribution is now provided in the splash screen and the About screen now
    has the attribution as the main content instead of being in a sub-menu.
    The FMOD logo is also included in the splash screen as per their attribution
    guidelines.

[33mcommit 88d1ecf456f7c7a569df02d6bd8aba9996a962c6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 3 15:03:01 2025 +0000

    Match up list looks across UI

[33mcommit 838753df3c72419e63c68f47a4b8b1867db79c2a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 3 14:47:10 2025 +0000

    Finish up remote media button controls
    
    These now work as for iOS, other than repeating the last callout.

[33mcommit 93f971567a73a1ca46e60dfd6e16b8df4d0be19c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 3 13:20:27 2025 +0000

    Add more talkback hints and refactor IconWithTextButton
    
    This commit has way too many different changes in it, but they are all
    linked.
    
    - There's a new talkbackHint Modifier which makes setting of a hint easier
    to do.
    - As part of that I added a setting to disable hints. However, all that did
    was to revert to the Android default of "Doublt tap to activate" which wasn't
    useful. However, I've left the code that uses the setting in place just in case
    it's useful later.
    - IconWithTextButton was duplicated and in general has way too many parameters.
    When adding the hint, I got rid of the duplication and rationalised the call sites.
    - Whilst adding a setting for Hint control I also added two to control the Theme
    that's in use. Light\Dark and some contrast control. The contrast control is less
    useful, but it's useful code if we come to extend the Theming.

[33mcommit 42b7f0abc8834caa441e49d1c5924b915d74a9cc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 3 10:40:58 2025 +0000

    Add Share to location details screen
    
    We share with a geo: URL and some text which is slightly different from
    iOS. However, it can be opened straight back into Soundscape so it's
    trivial to use and an RFC.

[33mcommit f5b3145991a77f596f03f9c26120ae4985239ea6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Mar 3 10:10:25 2025 +0000

    Add beacon mute control
    
    Clicking the mute clears the current speech queue and mutes any audio
    beacon.

[33mcommit a89ff96518a6023ccc7dfb5e5242a59f9d8910e9[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Mar 1 15:43:30 2025 +0000

    Update release notes and add smoke_test.md

[33mcommit e2c4ec7f5bd752a4b2371574df87e829f3d43ccb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 28 15:56:55 2025 +0000

    Fix minor lint warnings

[33mcommit 99696ca0336f5742474883cb019317467d2f2877[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.56[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Fri Feb 28 15:44:14 2025 +0000

    Bump version to 0.0.56, version code 57

[33mcommit 608705f422639438d0a9b61ebe0321c569abdd10[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 28 15:20:20 2025 +0000

    Use new theme and ensure the correct colors are used
    
    The initial requirement for this app was that it worked the same as the
    iOS app. This meant matching colors which resulted in a bit of a mess of
    different color usage (at last in part my fault!). This change uses a newly
    generated set of Material3 theme colors using:
    
       https://material-foundation.github.io/material-theme-builder/
    
    Then the aim was to only use colors from the theme and to always use the
    correct pairings e.g. onPrimary on top of primary, and onSurface on top of
    surface. The screenshot test code was extended to enable a visual check of
    this. In PreviewTest setting testTheme to true makes the screenshots use a
    custom test theme where the pairs of colours (Xxxx and onXxxx) are the same
    Color. If any of the screenshots have visible text or a visible icon, then
    there's a mismatch in the theme color usage that could cause a problem with
    some themes.
    With these changes in place, it should now be straightforward to create and
    use a new theme, or to allow custom ones. Note that currently, the app will
    always use the medium contrast Light theme.
    
    Obviously this does change how the app looks from a color perspective, but
    I'm hoping that it's seen as an improvement for usability and accessibility.

[33mcommit 4f6aa9b73267dfb376d6f82a3b6c1d429b6d9565[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 16:58:19 2025 +0000

    Fix up colors which should have been within Theme
    
    Some other UI fixes as I've tested

[33mcommit c6e7ce72e3e34fd2b416a927f6b270019c866495[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 16:05:39 2025 +0000

    Standardize list layout and use TextStyles from theme

[33mcommit 25b4fd7ea3fa9f92e7b5cd1335ebd7e176aebfcb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 15:21:59 2025 +0000

    More tweaks for small screens

[33mcommit 8b20f5ea5229486b995eea442dbd7aa14af567e6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 15:13:45 2025 +0000

    Move all .dp numbers into Theme as spacing.*
    
    This is just a way of centralizing formatting across the app as with
    lots of contributors it's harder to maintain a consistent look.
    There's room for a few more constants e.g. perhaps a single "rounded
    corner" sizing, and certainly font sizing.

[33mcommit 755ff234d0eca9ec89a7f84984d34ccf6a68ee38[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 13:20:06 2025 +0000

    Fixups for small screens
    
    Mostly adding in vertical scrolling and adding in better sizing for
    MapContainerLibre components.

[33mcommit 58aaf57b851dc42d5219de93ca5861e8ac9065bd[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 11:05:05 2025 +0000

    Add clear button to text entry and capitalize first letter
    
    This simplifies creating routes and markers.

[33mcommit ec7362d145508c95ea9313418b616f5f55a26ace[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 27 10:37:21 2025 +0000

    Fix missing viewmodel in RouteFunctions initialization
    
    Remove default initialization to null which caused the problem. Also,
    remember the functions so that they are only initialized when viewModel
    changes.

[33mcommit cefae757b14deb924c742a1a920dfab82412239d[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.55[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Wed Feb 26 19:57:27 2025 +0000

    Bump version to 0.0.55, version code 56

[33mcommit d38f8668eaa54690697ceae7c38b0350b6c10afb[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.54[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Wed Feb 26 19:17:14 2025 +0000

    Bump version to 0.0.54, version code 55

[33mcommit 41d3961d05d16955e5b2471b336c6e981acc684a[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.53[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Wed Feb 26 19:13:23 2025 +0000

    Bump version to 0.0.53, version code 54

[33mcommit c2c40eb2953eb49d00e3279753c88ed3b276454f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 14:19:38 2025 +0000

    Reduce number of arguments passed through HomeScreen components
    
    The @Composable functions were getting a bit unweildly so move the
    arguments into data classes.

[33mcommit 5bb3906735f7d84d08fe8320278ee47471cc27db[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 13:27:04 2025 +0000

    Add TalkBack hints to Home screen
    
    These hints match those found on iOS and the commit is to demonstrate
    a similar behaviour on Android. A couple of things still to do:
    
     * Update all translations to remove 'Double tap to' as that's emitted
       automatically by TalkBack. We need to check that all of the languages
       format the string in the same way.
     * Consider adding a user setting to disable the hints?

[33mcommit b11d459d7accc46973c5268706b58c72b2c92830[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 12:29:25 2025 +0000

    Fix map marker location drawing
    
    This change alters the icon so that the text can be drawn exactly in the
    center without padding required. The center of the bottom of the icon can
    then be used accurately as the anchor for the location to be marked.

[33mcommit 4e880f9ec1a34986e2bec2acde76a9953a703938[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 11:41:27 2025 +0000

    Move heading to Menu icon on Home page
    
    The heading was on the Soundscape title, but that had no function, so
    move it to the Menu icon instead, and skip the Soundscape title.

[33mcommit 8d81d8cb94d4a73d078c794f0e02f28332740b01[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 11:07:20 2025 +0000

    Fix Marker and Routes list sorting and start on accessibility

[33mcommit 39bcb74b598e2c55f53f922c02aa331da9000454[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 10:25:28 2025 +0000

    Add FAQ question as heading in answer page

[33mcommit 4c14619d62c73d5fef1e39b0dc17556e08b28665[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 10:03:58 2025 +0000

    Add semantic headings to Help screen section titles

[33mcommit 86e15c07c31aa05635dff9d294609b7a86a8e6c5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 08:55:17 2025 +0000

    Disable beacon Intro until we have intro/beacon/outro behaviour sorted
    
    Onboarding was playing the intro prior to each beacon style, so disable
    until the work to complete the intro/beacon/outro feature is done.

[33mcommit 7070cc8aa980c200feb63e5a08ff474c6b946b2c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 26 08:52:46 2025 +0000

    Small tweaks to route and marker lists

[33mcommit b06e7f8edda9c028fd0c53821c6d10438285a9f8[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.52[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Tue Feb 25 17:27:30 2025 +0000

    Bump version to 0.0.52, version code 53

[33mcommit a89b7a2085eb207f3d2a8961deae348e541639dd[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 25 17:17:17 2025 +0000

    Fix crash in maplibre due to passing in null location to SymbolOptions
    
    This crash was seen twice by Firebase where there's far more likely to
    be no valid location on the device.

[33mcommit 1c93844a3ffe49ba30207e07e3371fce9f2eea8f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 25 17:08:58 2025 +0000

    Enable autoBackup in manifest

[33mcommit 502ba19637712b784362900abc4496b2724dc0e2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 25 17:07:33 2025 +0000

    Fix proguard rules to allow route data import
    
    The gson code needed protection from obfuscation when SimpleMarkerData
    was being generated and parsed. Currently, this is only used when
    importing a route via an intent.

[33mcommit 5f1fd352d9d54aa939c3f2be92b7415b15c99ae9[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.51[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Tue Feb 25 12:36:29 2025 +0000

    Bump version to 0.0.51, version code 52

[33mcommit e29fd5b91bb70f97483b2da1348d56c6c9025833[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 25 12:19:08 2025 +0000

    Enable minify and shrink on release builds
    
    Two new proguard rules were required. One to preserve FMOD library and
    the other for protobuf generated files. With these in place the shrunk
    app now runs successfully.

[33mcommit 359667427fe2a37e1a04e1b105e64a1266e69d78[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.50[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Mon Feb 24 17:36:26 2025 +0000

    Bump version to 0.0.50, version code 51

[33mcommit 361f02379a7e3220dde98228eb9ebf807eb3af3a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 24 17:35:02 2025 +0000

    Revert "Enable minify and shrink on release builds"
    
    This reverts commit 2afb2726b356c7c14d91186f4449de01681ebcc6.

[33mcommit 2c7f3e0814d84ee79d1c210acd0b2dd67fa1e281[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.49[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Mon Feb 24 17:03:04 2025 +0000

    Bump version to 0.0.49, version code 50

[33mcommit 2afb2726b356c7c14d91186f4449de01681ebcc6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 24 16:44:13 2025 +0000

    Enable minify and shrink on release builds

[33mcommit dbfbc2b36fcfab99e14f95afd677b514292d1133[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.48[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Mon Feb 24 16:22:22 2025 +0000

    Bump version to 0.0.48, version code 49

[33mcommit cbe1c689458eb8ede5bb721d93dcc51f0de620e3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 24 11:14:13 2025 +0000

    Add initial Places Nearby screen
    
    Initial list of POI gleaned from the GridState. There's no filtering yet,
    and the POIs appear in distance order. Each entry is clickable to a
    LocationDetails screen where it can be saved as a Marker etc.

[33mcommit d1d45f5ccea5b0756b91d2a69c648aecac8b6af7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 24 11:30:41 2025 +0000

    URL encode JSON that's passed in navigation routes
    
    We pass JSON in navigation routes for LocationDetails and for adding routes
    from Intents. In both cases we need to be URL encoding the JSON so that any
    forward slashes etc. in the JSON don't cause problems for the URL parsing.

[33mcommit ee4af9658f162fd5e47ff6c31143c59017b38681[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 24 09:17:18 2025 +0000

    Make opening route via intent more robust
    
    * Limit the size of routes to 1MB to prevent arbitrary files causing out
    of memory issues.
    * Ensure that only one instance of the app is opened when opening from a
    route file using Android File explorer.

[33mcommit af126c9e11510bbb07cd66214e82d90faad8898f[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.47[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Sat Feb 22 14:23:19 2025 +0000

    Bump version to 0.0.47, version code 48

[33mcommit e1d0e399f5f7212c689aa1c3a5adc98ae39d5190[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Feb 22 12:24:45 2025 +0000

    Switch to using Calvin-LL/Reorderable for re-ordering list
    
    The previous list code didn't quite work properly. Calvin-LL/Reorderable
    makes it easy to have a drag handle and to scroll LazyColummns via the
    drag.

[33mcommit b871a69ef7fceda18d01e2b7e09ccc83782a9b77[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 21 15:06:07 2025 +0000

    Fix GpxTest to use ObjectId as unique identifier

[33mcommit 9dd8c5b95a64a02502c3f18d46b0e3ba372e81de[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 21 14:48:42 2025 +0000

    Improve screen layouts (a little)

[33mcommit 73b40ed07259607fba88f31d3fe3d59c327554f3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 21 14:15:58 2025 +0000

    Remove MarkersAndRoutesScreen from screenshot test
    
    ViewModel is a bit entangled and so it won't render. The latest screenshot
    library fails as a result.

[33mcommit abe25fcbb22d176721be2819e2cb1afa6173e200[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 17:07:46 2025 +0000

    Add route playback control to home screen
    
    When a route has been started, the home page switches away from the map
    to having a Card which contains information about the current route,
    a map of it, and some buttons to control it.

[33mcommit f9ec8cdac921e583493fb3f900dfa2b55bd27f27[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 21 11:11:04 2025 +0000

    More small changes to address various issues reported by lint
    
    From the terminal run:
    
        gradlew lint
    
    and then open the HTML report. There are still a number of issues,
    though few of them look important. It would be good to address some
    of the image ones.

[33mcommit 773fe12c98d5c770b2b861540e6ff5146cbd0596[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 17:06:44 2025 +0000

    Add ability to play INTRO audio when starting route playback
    
    We need to be able to play an OUTRO too...that's TODO.

[33mcommit 867ee1830b281872d4cd3899de839b9327c92638[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 12:54:52 2025 +0000

    Fixup some missing translations - some were only in Preview

[33mcommit 5ca5c8827ab85c43e3dab1654ae1a50e82d3197b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 12:30:53 2025 +0000

    Add translations of beacon types to UI - #175
    
    The beacon type strings are returned from C++. This change adds in a
    mapping function to resource ids which are then used in the UI.

[33mcommit 0e06dbaa0f2560857839e3a1357c407066c3de73[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 11:43:47 2025 +0000

    Flow RoutePlayer markers into Home screen map

[33mcommit 562f99e016442226da8cf062d05e21d1dcc77836[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 11:25:29 2025 +0000

    Add Route marker display to map GUI
    
    Passing in a RouteData to MapContainerLibre will display numbered markers
    on the map GUI at the location of each waypoint.

[33mcommit 45f87b5f51dca737af8aedeca48569a686ceb837[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 10:58:13 2025 +0000

    Remove display of Tile Grid from Map GUI
    
    This is no longer used so remove the code.

[33mcommit d03d732889d0c55d47527ff27be3b650cb6545ce[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 10:49:53 2025 +0000

    Fix TRACE logging in AudioEngine.cpp when no heading is available

[33mcommit 52507293e1c320c286eec2a693a128f7cff312e3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 20 10:33:39 2025 +0000

    Improve marker text centering
    
    The text was offset within the marker, using an ImageView and not setting
    the image as a background resolves this.

[33mcommit 01d891cc9b05205c5e493235b124532190155e4f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 19 12:55:09 2025 +0000

    Upgrade JVM version and all catalogue entries other than firebaseBom
    
    Upgrading firebaseBom results in an error, so putting that off for now.
    The github recipes were updated inline with the JVM version change.
    A couple of new lint errors were fixes.

[33mcommit 1e1bd6b6c1c86edc2a1ca43262fa088e31b35e25[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 19 11:29:41 2025 +0000

    Fixup last of the lint complaints during build

[33mcommit fe273babacaefeec8ef6542a97f042bec4e89fe2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 19 11:03:50 2025 +0000

    <wip> Initial addition of numbered location markers for map GUI
    
    These are slightly clunky looking, but can be finessed later. The code
    also switches away from the deprecated Marker code and uses SymbolManager
    instead.

[33mcommit 017944acfa7bafa919bdb7f9bd2c7b216f2fe42d[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 15:07:34 2025 +0000

    Fixup lint complaint about non-positional format string in resources
    
    First warning was:
    
    Multiple substitutions specified in non-positional format of string
    resource string/search_results_found_first_result
    
    The problem existed in all languages. Fix is to replace the two %s
    with %1$s and %2$s.
    
    The second warning was:
    Multiple substitutions specified in non-positional format of string
    resource string/callouts_hardware_low_battery
    
    In this case, the issue was a lack of escaping of a % symbol i.e. %%

[33mcommit fa778f78809e92619b85d9888cf2f4ae5255a831[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 14:54:02 2025 +0000

    Update location description with object id when saving a marker

[33mcommit 45493b43cf0774009dee2e288b317635724cb68c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 14:40:57 2025 +0000

    Fix navigation within Location Details screen
    
    Long presses on the map had been broken, and the navigation was more
    complex than it needed to be. Long presses need to pop off the current
    screen and replace it with a new one centered on the press. Other
    navigations just need to pop up the stack.

[33mcommit f5572d216fa7073721afee79dc25f9fcef6f37f2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 12:42:46 2025 +0000

    Implement "Nearby Markers" callout
    
    GeoEngine now collects the list of Markers from the Realm database so
    as to keep an up to date FeatureTree. This is then used when searching
    for Markers in the callout.

[33mcommit dce68a245ed3138187ac6f2917c81c48ccdb28fa[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 12:02:53 2025 +0000

    Debug route import from Soundscape community

[33mcommit 7a68a65e94808fcb8268f41311d6615e42bfc4d3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 11:44:27 2025 +0000

    Fix RoutePlayer beacon creation longitude/latitude flip
    
    The longitude and latitude were being flipped in RoutePlayer when switching
    between Location and LngLatAlt.

[33mcommit d8feb94066c459ff72e3db081a00d252ab6b1ab9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 18 11:25:23 2025 +0000

    Import route now goes via Add Route Screen and some UI tweaking
    
    Importing a route via an intent now goes via the add route screen. This
    passes JSON describing the route from the intent into the screen which
    then displays it and allows adding it to the database.
    
    There are also numerous amateur tweaks to improve the click sites within
    the markers/routes UI. Plenty still to do.

[33mcommit b2e2d9b20e2a73dc8bc24290e8190f0850cd1cc5[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.46[m[33m, [m[1;31morigin/dave-ui-work[m[33m)[m
Author: davecraig <8530624+davecraig@users.noreply.github.com>
Date:   Fri Feb 14 16:06:55 2025 +0000

    Bump version to 0.0.46, version code 47

[33mcommit 780848cb7f5ffa34687cabba7adade3d3a943650[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Feb 14 15:19:50 2025 +0000

    Rationalize LocationDescription by removing distance string
    
    Instead of storing the distance string, we calculate as and when it's
    required by the UI.

[33mcommit aee30e6e0c090ec3cf608c142e9480b4ed75a025[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Feb 8 12:10:56 2025 +0000

    UI additions to allow waypoints to be added to routes and reordered
    
    This change allows the selection of Markers to be added to Routes. It's
    not refined from a UI point of view, but I think the code is at least
    reasonably structured. Amonst the highlights:
    
    * ReorderableLocationList adds a list of LocationItems which can be
    dragged to reorder. Currently the whole item can be clicked to drag, but
    really we just want a drag handle target on the end.
    
    * Addition of ObjectIds to MarkerData and RouteData for unique identification.
    This means that we can have multiple routes and markers with the same name
    which matches iOS.
    
    * Add `decoration` to `LocationItem` to allow its reuse in different list.
    
    * Switch to using Flows from the Realm RouteRepository to enable lists to
    dynamically update when the contents of the database changes. Fix the sorting
    code so that it's closer to doing the right thing...
    
    * Merge AddRoute and EditRoute screens into a single screen.

[33mcommit 5f87b6c392a50feb9d2a469900e3d1cd6ff9409e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 13 08:54:25 2025 +0000

    Fix assert when playing route with no Waypoints
    
    The nightly build on 13/02/2025 at 05:46 ran a Robo test which hit an
    assert because it was starting playback of a route which had no waypoints.
    This commit removes the assert and simply ignores the request to start
    playback.

[33mcommit d68a1bfd046dcb4d02a5390a0f40ab8dfda488a6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 12 15:25:03 2025 +0000

    Fix Preview to skip MapContainerLibre
    
    I think MapContainerLibre is all a bit too non-standard for Preview to
    work, so I've wrapped it in `if(!LocalInspectionMode.current)` which
    means that it won't be used when in Preview. The result is that Home
    Preview and others that use maps will now work.

[33mcommit c653c24376207b3f7b31fb5d3fa9a675505cbe08[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 12 10:46:04 2025 +0000

    Disable photon reverse geocoding for now
    
    It's timing out constantly, and there's some re-design required to
    make the UI reponsive.

[33mcommit 666a19af2bea0bdf6827d7186d1f6ef472b454f6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 12 09:56:01 2025 +0000

    Add distance rounding to formatDistance as per iOS app

[33mcommit 1f057f19e3226f12a3825e553a2c68e4abf420d5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 11 15:17:16 2025 +0000

    Add head relative and compass modes for positioning audio
    
    One of the things to fallout of the iOS type heading management is that
    we need to be able to generate head relative callouts. This change adds:
    
    STANDARD - 2D audio
    LOCALIZED - 3D audio at a location
    RELATIVE - 3D audio at a heading relative to the user's direction
    COMPASS - 3D audio at an absolute heading i.e. North, West etc.
    
    This involves changes at all the layers from PositionedString down and
    into the C++ code. PositionedString now takes a type and a heading
    parameter to allow RELATIVE and COMPASS modes. Although I've tested the
    two modes, I haven't altered any call sites to use them yet.

[33mcommit dcfb8fdb560eefeca5288051b1caee9c2c59f7fa[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 11 12:22:51 2025 +0000

    Add presentationHeading support as per iOS app
    
    UserGeometry had already been changed to add support for multiple sources of
    headings - travel direction, phone and in future head tracking. However, the
    3D audio positioning was still always being done based on the phone heading.
    Most of the change here is changing that to use the presentationHeading which
    when moving is the travel heading and is the phone heading the rest of the
    time.
    This required moving the audio engine update out of the AndroidDirectionProvider
    and into a combined flow handler in GeoEngine. That is now responsible for
    updating the AudioEngine with the heading.
    Still to do is to figure out what the AudioEngine should be doing if the
    heading is null. We should at least move the audio beacons to 'dim' mode.

[33mcommit ac3bc28e89cc6572b460c4bf1f298a566304cb82[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 11 08:29:31 2025 +0000

    Catch timeout exceptions when contacting photon server
    
    Retrofit throws exceptions on a timeout, so catch them and return null.

[33mcommit 246c207e5098f8b9dec83f99885fb1b53f46f3e1[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 6 15:43:01 2025 +0000

    Small fix-ups to make callouts more like iOS
    
    - Remove distance from intersection description
    - Add POI category earcons

[33mcommit fdcdafe778e5bfc26edf0e2d0d44a49564bc393a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Feb 6 08:38:29 2025 +0000

    We only need the RoutesRepository as it holds markers too
    
    Because we link Markers to Routes they need to be in the same repository.
    Remove the duplication that occurred.

[33mcommit a0f10b0ad02ae9fba04c2575e0695967db54163e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 10 15:26:47 2025 +0000

    Limit playMode earcons to user initiated callouts
    
    iOS doesn't play the mode-enter/exit earcons for auto-callouts, this change
    matches that behaviour. Also don't play them if there were no results from
    a callout.

[33mcommit e4cb0944d39c6ccb330d2dd013dbb72eb454dc83[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 10 15:11:28 2025 +0000

    Monitor app state and use it in heading logic along with isFlat()
    
    This change is to try and match the iOS heading logic. The phone heading
    is used if the phone is held flat or if the app is running in the
    foreground (Soundscape UI is showing on the screen). Otherwise, the phone
    heading is set to null and the callouts need to deal with that.

[33mcommit 120c65dbc3c81d56794c5e803252b9cc7882fee4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 10 11:30:35 2025 +0000

    StreetPreview waits for updated TileGrid before continuing
    
    Prior to this change, trying to use StreetPreview to a far away location
    was broken and would jump to the edge of the current TileGrid instead. This
    change causes the UI to wait for the new TileGrid to be uploaded before
    jumping to the nearest road and providing a choice for the user.

[33mcommit 1f3fbe61662d607ae2a2c30806cf1295ec83502c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 10 10:58:10 2025 +0000

    Take more care in FeatureTree functions that tree is not null
    
    A couple of crashes appeared due to FeatureTree searches taking place
    before the TileGrid had initially been populated e.g. by pressing "Current
    Location" immediately at startup. This change ensures that tree is checked
    for being null before using it.

[33mcommit fd51320aad1e81d46bdbcbef08487e1e5d12ca24[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 10 09:36:38 2025 +0000

    Earcon audio positioning fixes
    
    The Earcon code was a bit confused and resulted in audio positioning not
    working. This change makes EarconSource operate in the same was as
    BeaconBuffer and TextToSpeechSource. It creates the sound in FMOD and
    then leaves the rest up to PositionedAudio::InitFmodSound.
    The only additional work that EarconSource has to do, is that it has to
    monitor for the playback completing so that it can mark the Earcon as
    EOF.
    The other minor change is to start all audio paused, then set its
    position, and then take it out of pause to start playing.

[33mcommit 396c1f7d35ba50aa851c009631b2446e414162dd[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 31 13:41:29 2025 +0000

    Move APIs with doubles for lng/lat to use LngLatAlt instead.

[33mcommit 832ceb914b14818d8029d99bb62dd47b6fa64ed4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 31 11:52:36 2025 +0000

    Add the start of reverse geocoding to describe current location
    
    When creating markers we want to be able to describe the location so
    that the marker name makes sense. The approach here is to:
    
    1. Generate a description based on the local tile data if we have it
    2. Request the photon server reverse geocodes the location
    
    The latter generally gives a better response, but is slower as it
    involves a roundtrip to the server.

[33mcommit 1241fd8f6b82b05fe8895d5b9a70a3c3959bd7d8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 5 14:14:15 2025 +0000

    Move RoutePoint to MarkerData
    
    This was a duplication of code, but MarkerData is more neatly written, so
    use it.

[33mcommit ec9b46f3ead724efe90c171a1fe28813c32febcb[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Jan 28 18:34:52 2025 +0100

    Reuse LocationItem from search to display marker

[33mcommit 66bc388f6070f1aaf1bd6d0cc4467daf8504394e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 5 11:38:54 2025 +0000

    Add sleep screen as per #137
    
    Add full screen sleep. Hitting wake up now or swiping back comes out
    of sleep.

[33mcommit dd74314e1c81fcd877a9644c64679ba7eb10611b[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sun Feb 2 11:35:27 2025 +0000

    Move towards better StreetPreview UI
    
    The direction choices are now provided as a flow to the UI which can
    change depending on the heading and choices. Simple buttons for now.

[33mcommit 32e332a338d38b660d51fca53109f3507a108fa0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 5 10:19:33 2025 +0000

    Fix another bug in LineString distance calculation
    
    The iterating over segments was off by one. Add centralized formatting
    for the distance strings.

[33mcommit 7c3ada70777c7fd633b4b7adb90e9969422f6933[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 5 09:58:23 2025 +0000

    Refactor UserGeometry into its own file
    
    Move UserGeometry out of GeoEngine.

[33mcommit ea12bc016a8b7311d6bd8e9c3fdda364174f065a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Feb 5 09:51:25 2025 +0000

    Initial addition of travel heading
    
    We need similar behaviour to the iOS app where it sometimes uses the phone
    heading, and sometimes the direction of travel as its heading. For example,
    auto callouts always use the direction of travel, but triggered callouts like
    AroundMe use the phone heading. This commit extends UserGeometry so that it
    stores both types of heading and selects between them.

[33mcommit 02bb1a262e4042d0c7a2483b18db6dec8d51236a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Feb 4 16:05:04 2025 +0000

    Add About information for third party licenses
    
    This is a reasonably comprehensive list of third party libraries that are
    used, though perhaps we need a Google acknowledgement too.

[33mcommit 279b05efb41a7b57dda5be855fd70086f315affe[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 3 17:37:12 2025 +0000

    Initial help topics screen from main menu
    
    Code to display the help text originating from the iOS resources. The
    Sections data structure is designed to make it easy to add in new
    Sections of help, FAQ and tips.
    
    The text for the resources has not been checked relative to how the
    app actually works, so this is just the first step in their addition.

[33mcommit ef09e62e7302c2e142d29bfb6d6e9adcf2fe8893[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 3 11:46:04 2025 +0000

    Wire "Route Start Play" up to RoutePlayer
    
    RoutePlayer is the initial code to playback routes, this change means
    that clicking "Start Play" from within the RouteDetailsScreen will now
    trigger starting playback.

[33mcommit 086c669dc4d8ea605964c923e7861b2da46d834e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Feb 3 09:35:31 2025 +0000

    Fix distanceToLineString to iterate segments
    
    During refactoring I broke distanceToLineString so that it only looked at the
    first segment in a LineString. This change ensures that it iterates over
    all of the segments.

[33mcommit f6e80f2ec43c2e2294dec03766aa59cbbad86143[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Feb 1 16:39:36 2025 +0000

    Initial decode of iOS exported route and import into database
    
    This is based on a sample file from when a route was exported from the
    iOS app. This commit registers application/json for the app and then
    tries to decode it with the assumption that it matches the format in the
    sample file. RouteData and RoutePoint don't have fields for all of those
    in the JSON, though we may add them in future.
    
    Sharing a marker from iOS results in a deep-link URL. Once we have a
    server set up we can uncomment the support for it and add parsing for
    that too.

[33mcommit 41a110d3761f79a31fc1c38bbaed439301f076a8[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Jan 31 11:05:01 2025 +0000

    Remove unused classes and test (#348)

[33mcommit 4871e5bad80fd65a2b7144af71f0223c7fffadfb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jan 30 14:25:36 2025 +0000

    Add some flow control for text-to-speech and callouts
    
    The immediately useful change here is that the auto callout code now calls
    isAudioEngineBusy before generating any more output. If there is already
    text-to-speech playing out, then it won't generate any more. This aims to
    prevent a backup of audio description.
    The other change is the start of a callback when audio playout completes,
    but so far this is only within the C++ code and doesn't make it back to the
    kotlin code.

[33mcommit c9d6da872974fe500051dbedb92f3305e3604410[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jan 30 10:42:59 2025 +0000

    Add places and landmarks tree and refine AroundMe
    
    The AroundMe callout generator was searching amonst too many super
    categories. This change limits it to just places and landmarks by creating
    yet another tree to search in. Once we have all the functionality that we
    need, we can remove unused trees.
    The other change is that in whatsAroundMe the code now requests more than
    1 features within each quadrant. This is so that de-duplication between
    quadrants can be done properly.

[33mcommit 251c9df01f0435ad43378da2e0642b7a6a2c0d78[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jan 30 08:30:49 2025 +0000

    Finessing of auto callout
    
    More work here to include some of the iOS app logic. UserGeometry is no
    longer just a data class, but includes functions to request various
    distances which are calculated from it's member variables. The main
    different with iOS auto callouts, is that on iOS lots of the callouts
    get discarded because there's already speech being played.

[33mcommit 6ccb334185293fb278938af294c3f9d3a4436e90[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jan 30 08:29:57 2025 +0000

    Return sorted list from generateNearestFeatureCollection
    
    The first returned entry is now the nearest.

[33mcommit 50c2b82d1d115dde7ca2947d70e0f3786903da7f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 29 11:15:02 2025 +0000

    Move AutoCallout code out of GeoEngine and into its own class

[33mcommit 14a224347a8242bf6248593dc38257914b6ac870[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 28 09:46:07 2025 +0000

    Fix coordinate system used within FMOD to preserve direction
    
    Prior to this change the code was just using longitude and latitude
    as x,y coordinates within FMOD. That's fine near the equator where
    1 degree is a similar distance in each direction, but moving away
    from the equator 1 degree of latitude shrinks (down to zero at the
    poles!). This change uses a very simple equirectangular projection
    to translate longitude and latitude into an x,y system for FMOD.
    The origin used is the first coordinate converted which will be
    either the current location, or the locatoin of a beacon. Ensuring
    that the origin is relatively close means that the projection is
    accurate enough for our purposes.
    To test the improvement, create an audio beacon and point directly
    at it on the UI map. The audio should be playing evenly out of left
    and right on your headphones.

[33mcommit 0fae4cded918c3752cec66c4e67302d2582d6a6f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 16:02:06 2025 +0000

    Simplify markers/routes tab navigation
    
    The desired behaviour is that the chosen tab is remembered, and swiping
    back treats the Markers and Routes screen as a single screen. To be honest
    I didn't fully understand the code before, and so I may have missed some
    subtlety, but the way it works now is that the HomeViewModel stores the
    selected tab and there's only a single NavController involved. As far as I
    can tell this gives us the behaviour that we want.

[33mcommit caee5f4bd353df90958257a2b9f7c414727cde04[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 13:40:44 2025 +0000

    Various fixups for markers/routes from Fanny's comments in #302
    
    More string localized, and some alterations in text size.

[33mcommit 2e9d189eb40528645d76edf41a0aba53a0c179af[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 12:52:27 2025 +0000

    Add previews to markers and routes screens as per comment on pr #302

[33mcommit a0d6957cac875b6ad3ad7041d33bca499521f0d2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 11:44:40 2025 +0000

    Attempt at tidying up routes navigation
    
    There were a few crashes due to HomeRoutes being called on the routes
    screen navigator. This tidies that up and everything is back working,
    though the routes/markers selection doesn't persist when going back to
    the home screen and back into the route screen. Not sure if that should
    be expected?

[33mcommit 9353e9a82a34cbb60338b2262d870c3f0a54c7a2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 10:43:00 2025 +0000

    Add code to insert marker into database
    
    A few changes here which add the 'Save as marker' option to the LocationDetails
    screen. The resulting marker then appears in the Markers page. Nothing can be
    done with it yet, but just figuring out whether or not this is a sensible
    approach.

[33mcommit 0a3fae02ffef3b682a63af3dcc26dc057016d23b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 10:30:13 2025 +0000

    Fixup spelling in addressName

[33mcommit d5d8abe6e4f490b019971e12924e17615e5e0870[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 10:27:21 2025 +0000

    Remove auto-play of route in database
    
    Prior to this change, the app would try and replay the first route found
    in the database. Now it's left up to the UI to select a route to playback.

[33mcommit 6ff3bc3a4c31ce53ab04fd2eaf31d6c410cb4b55[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 27 10:26:21 2025 +0000

    Make markers and routes database persist between runs
    
    For initial development the realm database for markers and routes was
    inMemory, this change persists it in storage.

[33mcommit bbc1ce48627d32ea83d16ba105362960eda5f31f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 22 16:54:20 2025 +0000

    Fixup screenshot tests with routes squashed code

[33mcommit ebbf4066408670d8f558ba61f11dad78fd3c4a06[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 16 13:47:30 2024 +0000

    Land addition of Routes viewmodel code
    
    This is a squashed and rebased version of the code here:
    
    https://github.com/Thow76/Soundscape-Android/tree/markers-and-routes-sharedPreferences
    
    I found it too hard to merge without squashing.

[33mcommit 6df6a83ab15f38e7745763c0452fd5375e3f6b14[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 22 10:45:00 2025 +0000

    Improve callout text for bus stops and train stations
    
    The naming of bus stops and stations doesn't include the fact that they
    are stations. Add that to getNameForFeature and rename that function as
    getTextForFeature as that's closer to what it is.

[33mcommit affffa94a36b4aff7d58e8da7f8fe07a11d182de[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 22 09:48:15 2025 +0000

    More squashed refactoring of GeoEngine code
    
    * Triangle class added. Much of the code was using raw coordinates, and
      it wasn't always clear which one was the origin which is important when
      measuring distance. getFovTriangle (formerly GetFovTrianglePoints) now
      returns a Triangle. createPolygonFromTriangle can be used to turn a
      Triangle into a Polygon.
    
    * Distance functions to lines moved within LatLngAlt. This makes the
      calling format clearer.
    
    * FeatureTree search improved so that distance to polygon is always to
      the nearest point/edge. By using Triangle it's harder to accidentally
      use the wrong point to measure distance from. Sorting efficiency for
      the list of nearest features is improved.
    
    * Super categories are now their own FeatureTrees. The only one currently
      used is for the currently selected super categories, though this may
      change in future. At the cost of a little bit of memory, this simplifies
      callout code as it just has to use the FeatureTree it needs and doesn't
      have to check settings flags and construct a list of the Features.
    
    * GetQuadrants removed. It was only used in one place, and could be much
      simplified.
    
    * UserGeometry now contains speed from the location provider too. It's also
      used by more and more functions in place of location.
    
    * getDistanceToFeature returns a data class PointAndDistance which has the
      distance to the feature as well as the nearest point on the feature.
    
    * Disable polygon merging for now. The merged polygons weren't closed and
      this was leading to errors in polygonContainsCoordinates.
    
    * Added PoiTest which tests FeatureTree searching

[33mcommit b4363ee76c974b572f5ae7293e708f2ab7454c96[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 20 15:51:22 2025 +0000

    Refactor whatsAroundMe
    
    The code that calculates selected super categories was being run each time
    whatsAroundMe was called. This commit adds some new FeatureTrees for the
    different super categories including one that is for those that are
    currently selected. This simplifies the caller code significantly and moves
    the cost of calculation to the tile update for a one off cost.
    
    By using the FeatureTrees to search the super categories, we can remove
    findFeaturesInPolygons which is the same code as already is in the
    FeatureTree.
    
    A couple of the distance functions have been moved into LatLngAlt for the
    same reason as the original distance function was moved there. It makes
    for a cleaner API for calling that has a less confusing argument list.

[33mcommit c0871d2a3778a428ef65fa194716b67271b5555e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 20 15:41:09 2025 +0000

    Reduce tracing from audio engine

[33mcommit 2fb3bee07aad49cb99e1dc475623b74a74bae0df[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 17 16:55:17 2025 +0000

    Move Feature classification functions into GridState
    
    This is the only place in the main app that calls the feature classification
    code so it makes sense to be in that class. The main fallout is switching more
    unit tests to use GridState so that they can still get hold of the
    classifications. An interesting piece of fallout from this is that the
    FeatureTree code wasn't dealing with MultiPolygons and so some unit tests
    failed now that they were roundtripping FeatureCollections through the
    FeatureTree. Adding support for MultiPolygon into FeatureTree fixes those
    tests.

[33mcommit bf73a08c24bf57f7d76c27ddd29cc15687c11d38[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 17 14:52:08 2025 +0000

    Rework FOV triangle calculations and extend scope of UserGeometry
    
    The FOV triangle calculations were a bit beyond me, so here they are
    reworked to share more common code. I've pinched the comments from the
    iOS code to describe the different FOV modes.
    Whilst doing that UserGeometry crept further into the code, so there
    are a lot of changes to the unit tests to add that in.

[33mcommit f27dc772f824aad18933f566440c190f78d4a07f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 17 12:03:19 2025 +0000

    Introduce UserGeometry data class which includes location and heading
    
    This is simply a convenience function to reduce the number of arguments
    passed around. UserGeometry includes the location, heading, activity
    recognition based flags and the FOV distance. Basically anything that's
    required to describe the geometry of the user.

[33mcommit 6a020ece19f5f16ccab4c4e73d3756e58ff056a7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jan 16 17:22:26 2025 +0000

    Create GridState class to hold tile grid data and functionality
    
    The GeoEngine creates either a ProtomapsGridState or a SoundscapeBackendGridState
    which hide away creating the FeatureTrees from data downloaded from those
    servers. The FeatureTrees can be accessed via getFeatureTree or via various
    search functions which have been moved into the GridState class. These all now
    call checkContext which ensures that the code is being called on the single thread
    allowed to access the FeatureTrees.
    The unit tests can create a GridState from either a FeatureCollection, or from a
    GeoJSON string via the helper functions in its companion. Because those functions
    create unchanging FeatureTrees, the unit tests don't need to run in the single
    thread context and contextCheck does nothing.

[33mcommit 81ef86cf0b54e1d5a7728512336b7b397c48ab2a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Jan 16 20:17:58 2025 +0000

    Make whatsAroundMe behave more like iOS (#339)

[33mcommit 37eb09e468b207de26d6ce8f39c086bc88f6f694[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 14 14:27:24 2025 +0000

    Add calloutHistory check for "Ahead road X" auto callouts
    
    When reinstating this part of the auto callout, I forgot to rate limit
    it with the callout history. This change does that.

[33mcommit 842effd7d7affd123bcc3ab307d960c26d3d8839[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 14 10:48:07 2025 +0000

    Add timeout to direction provider to update audio engine
    
    When the phone is still, or when running in the emulator, the orientation
    is not being updated as it's not changing. This was resulting in the audio
    engine not being updated which was causing its state machine to stall.
    Ideally that will be fixed inside the C++ code, but it's conceptually much
    simpler to change the calling code here. If there's no heading update within
    the timeout period of 100ms, then the audio engine is still called with the
    last heading and the current location. Note that when running on the emulator
    the location can change whilst the heading remains forever stuck at 0.0.

[33mcommit 8bdf8d6ae1334c577f39016ea459f26ab8bb9579[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 14 10:46:00 2025 +0000

    Reduce timeout for TTS data EOF
    
    The timeout was a count of 10 100ms buffers which was resulting in a 1
    second delay at the end of each speech output. Reduce this to 500ms as
    in testing I've never seen it timeout even once.

[33mcommit 7077044dc2c4f5dc691423e6a36713b2e97d90d4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 13 14:32:32 2025 +0000

    Reduce latency introduced by KalmanFilter
    
    As per #317 reduce the latency by increasing the sigma of the KalmanFilter
    to 9.0.

[33mcommit 9e4664b530a54bc12f30175f6c806452d59b631e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 13 14:10:43 2025 +0000

    Adjust coordinate calculations for MVT translation by 0.5 pixel
    
    The calculations that translate the integer relative tile coordinates into
    fractional tile coordinates now place the sample point in the middle of the
    sample range i.e. on the half pixel. This fixes the calculations made for the
    interpolated points where lines cross tile boundaries as the tile boundaries
    are no longer coincident with the sample points.

[33mcommit 668e75cd4fe60eec68ccfde9b9a363355b73577b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 13 11:19:15 2025 +0000

    When deduplicating point and Polygon POI allow for multiple Polygons
    
    We don't want to have point POIs if a polygon POI can be creates instead.
    However, often there are multiple polygons for a single POI and this change
    ensures that we deal with that by maintaining a list of polygons rather
    than just a single Polygon when checking for duplicates.

[33mcommit 041485d54d053ef2acad6ae3bda4c21a04384136[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Jan 13 11:14:45 2025 +0000

    Fix the Polygon parsing during MVT translation
    
    When parsing Polygons from MVT we have to calculate the winding order in order
    to know whether the geometry is a new Polygon, or an interior ring of the
    last Polygon. This fix means that we can remove the code in
    mergeAllPolygonsInFeatureCollection which was working around it.

[33mcommit 58f4e660cdf85af251efbde1b3085262a551af7a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 10 15:56:59 2025 +0000

    Increase Photon search timeout to 20 seconds and handle in unit test
    
    If the unit test fails due to a timeout, count it as a pass. This is
    until we are using our own photon server when we can rely on it more.

[33mcommit ff7b82ba014919c9dc92821c92536275102e726e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 10 15:27:00 2025 +0000

    Add back in nearest road for Ahead of Me
    
    I'd missed this by mistake when refactoring - this change restores the 'X
    road ahead' prior to describing an intersection. However, it does bring up
    a question:
      In the previous commit I switched nearestRoad to be the actual nearest
    road, and not the nearest within the FOV. For reporting 'X road ahead' I
    think that we do want the road in the FOV so I calculate both. It seems
    'no worse', but requires some more thought. However, the code is all in one
    place now so should be easier to consider.

[33mcommit 3f78e913071063955c0538cbebac35ebdb47fea8[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Jan 10 14:37:11 2025 +0000

    Adding translation strings for distance to POIs. (#326)

[33mcommit f4743ac06f7da591f8582e2e76e54fef6d41fb56[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 10 10:22:27 2025 +0000

    Centralize intersection description code
    
    The main change here is to de-duplicate the intersection description code
    from the unit tests and GeoEngine and have a single function in a new file
    IntersectionUtils. This should be the same as as that which was in the
    ComplexIntersections unit test. A new function in the same file can then
    take the IntersectionDescription and turn it into a callout.
    The other change in that area is the start of improving the callout code
    to use the results of the ActivityRecognition to switch between heading,
    facing and travelling in the callouts.
    The final change is that getNearestRoad now uses FeatureTree to perform
    it's searching. Not only does this improve its performance, but it allows
    us to search outside the FOV - the nearest road could be 0.5m behind the
    current FOV and searching only within the FOV would preclude that. The
    unit tests all still pass with this change.

[33mcommit efab8aedbf820342b8d2034015e4a4b938f90376[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Jan 9 19:23:57 2025 +0000

    Merge overlapping polygons for POI on boundary of gird tiles (#324)

[33mcommit 784acee6ec3dbce7cd98776af045027ede867b37[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Jan 9 15:07:10 2025 +0000

    Proof of concept for merging Polygons using JTS (#323)
    
    * Proof of concept for merging Polygons using JTS
    
    * Proof of concept for merging Polygons using JTS

[33mcommit 821b0361050526b2aff8297997c97c9dd75f22a6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jan 9 12:12:56 2025 +0000

    Add failing test for straightLineIntersect and fix the issue.
    
    Although the two lines share a vertex, maths precision in the calculation
    means that the intersection isn't detected. The fix is to check the
    vertices before going ahead with the maths.

[33mcommit aa0c4c80af94288e81fc5f9a7cfa803cae3dc7e0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 8 16:35:06 2025 +0000

    Reduce amount of back and forth between LngLatAlt and lon/lat Doubles
    
    This commit changes functions which took Double lon/lat to take LngLatAlt
    instead. By standardising, the calling code should eventually be much
    simpler. There are probably some more functions to root out, but at least
    GeoEngine is much more concise now.

[33mcommit e46b3a754140819038149cd2ec4628cb324a4a6b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 8 15:53:56 2025 +0000

    Instead of a PATHS collection, have a ROADS_AND_PATHS collection
    
    We always have a ROADS tree/collection and we did have a separate PATHS
    tree/collection. However, we never need just PATHS and most of the time
    we want both. For rtree searching ROADS_AND_PATHS is much simpler, so
    create that.

[33mcommit 678e18d36485fe0353b78d3a21c5f735f55febde[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 8 15:53:18 2025 +0000

    Fix getNearestFeature calls which were erroneously using NaN
    
    getNearestFeature using a distance of NaN results in no features. The correct default
    value is infinite, fixed in this commit.

[33mcommit 62244d5bba659fe60c6e6ffcc3d1950b149f55a7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 16 10:16:41 2024 +0000

    Prevent drawing behind status bar
    
    When an app is running on Android 15 edge-to-edge mode is enabled
    by default. As a result the Settings and LocationDetails screens on
    my Pixel 8 are currently drawing behind the system status bar. This
    change fixes that - though I'm not sure on what the convention is for
    setting and passing Modifier?

[33mcommit 1f22b4093dde83f932fea14f30f0bf2e3ff0640e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 8 14:38:32 2025 +0000

    Rename poorly named testRoad function and add loopback comment
    
    Whilst refactoring I renamed this very poorly, this is a better name:
    getFeaturesWithRoadDirection. Also add in a comment explaining why it
    returns a FeatureCollection and not just a Feature.

[33mcommit c03406085491dd821ddfe95cb9cf29d4b58611e8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jan 8 08:59:50 2025 +0000

    Move routing algorithm functions into separate file
    
    TileUtils.kt was getting a bit large, this change divides it up into
    files with more specific goals.

[33mcommit 879106906d1e084658183c2d3a050da92d3516a6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 7 17:35:49 2025 +0000

    Update unit test MVT tiles to current state of protomaps server

[33mcommit b3ddb1bb9018b883f832c5a4eaff33ba5a0bedaa[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 7 17:18:04 2025 +0000

    Refactor unit tests to use shared code for setup
    
    Changing APIs can be really time consuming if each test is calling
    the same API in duplicated code. Centralise shared code in each test
    to make this simpler going forwards. For IntersectionsTestMvt we now
    have a planet wide map, so enable the loopback test.

[33mcommit 6cb45ee03d66c980148c8b3aeadb45d4f54e4bce[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 7 16:39:09 2025 +0000

    Refactor some TileUtils functionality
    
    The aim is to simplify the calling code so that commonly used code is hidden
    away in shared functions:
    
    * Change functions which either take FeatureCollections containing a single
    Feature as arguments or return them have been changed to take/return Feature?
    This makes it clearer what the behaviour is. Updating all of the callers then
    makes it more obvious whether the FeatureCollection behaviour is required at
    all.
    
    * Switch to using the FeatureTree FOV functions which will search within the
    passed in FOV triangle. The RTree does very fast filtering based on the
    bounding rectangle of the FOV and then runs more fine grained searching on the
    much smaller number of filtered Features.
    
    * Start to move away from using `distance_to` within Feature. By using RTree
    we are massively shrinking the number of Features being handled and
    recalculating the distance/bounding box is low overhead for a few Features.
    
    * Move away from functions with Poi/Intersection/Crossing in the title where
    the function deals with generic Features.
    
    * The unit tests were all written to use FeatureCollections directly, and
    rather than change them too much, I switch to using the FeatureTree versions
    of functions by creating temporary FeatureTrees each time they were required.
    This isn't how the main application behaves, but there's a lot of duplicated code
    in the tests that could do with centralizing (in the tests at least).

[33mcommit f8ff46d7d1f8ef06d186a149c686be06e4fe59e0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Jan 7 16:24:57 2025 +0000

    Add FOV (field of view) searching to FeatureTree class
    
    Prior to this change we could get FeatureCollections by distance from
    a point, limiting the results returned to a maximum number if we desired.
    This change adds:
    
    generateNearestFeatureCollectionWithinTriangle - returns a FeatureCollection
    of the nearest features within the provided FOV triangle.
    
    generateFeatureCollectionWithinTriangle - returns a FeatureCollection
    of all the features within the provided FOV triangle.
    
    getNearestFeatureWithinTriangle - returns the nearest Feature within the
    FOV triangle.

[33mcommit bde533e10518702d908afce8aa4b44974ec457b6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jan 3 15:05:22 2025 +0000

    Update release notes for 0.0.45

[33mcommit 98daafa37e363b507795758551b282ab9d6df683[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.45[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Fri Jan 3 14:44:50 2025 +0000

    Bump version to 0.0.45, version code 46

[33mcommit fd442b96106390fa433c6815a10aa80a60c1bfd4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 30 16:07:18 2024 +0000

    Add localization of generic OSM tag names
    
    This adds support for the tags that the iOS app was already translating. For reasons
    best known to itself it was altering strings like bicycle_parking to bike_parking in
    the Swift JSON parsing code. We just use the stock OSM values in our lookup.

[33mcommit f928805e4da383de92e4cb76017309c95308b280[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 30 15:00:40 2024 +0000

    Initial StreetPreview random wanderer
    
    Currently it's not configurable, just turn it on at build time by making
    streetPreviewGo call the streetPreviewGoWander code instead of just
    streetPreviewGoInternal. It prefers to continue in the same direction as
    the road it comes into an intersection on, but doesn't have any memory
    so can switch direction randomly.

[33mcommit 61da318930f6c2dbbd08928786a351b95324a346[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 30 11:35:40 2024 +0000

    Move StreetPreview code out into its own class
    
    Also includes the correction of an assert which was bogus.

[33mcommit a81c4c550a87d82e96c4621e7c7367cd374ff060[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 16 14:18:07 2024 +0000

    Playouts all auto callouts in one
    
    When in StreetPreview mode, the location only updates at intersections and not in
    between. That meant that only intersections were being called out and not POIs as
    the autocallout would only do the first one it found. This change means that it
    build all of the callouts that it can on the first change in location.

[33mcommit 905db79e2f983e360b1fe2216a49d55e7e8da0f7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 16 09:33:22 2024 +0000

    Only add lines to roads and paths FeatureCollections
    
    Polygon's are valid in OSM e.g. the precinct in Milngavie, but we can't
    navigate along them so filter them out.

[33mcommit b028211251fc036aa2550382dcdaa32b9547ca50[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Dec 13 14:33:53 2024 +0000

    StreetPreview improvements for handling non-intersections
    
    Several changes in functionality here:
    
    1. The initial StreetPreview jump is now to the nearest node on the
    nearest road instead of the nearest intersection.
    2. As a result the StreetPreview code has been updated to be able to
    split a road at its current location so that it can move either way
    along a road.
    3. When the StreetPreview hits the edge of the grid, it has to stop
    and then allow time to load in the new grid. From that point, it has
    to move along the short new joining road which is slightly tricky
    because it's pointing in the wrong direction - it will always be
    along the grid edge. This commit deals with that by removing the
    joining line segment from choice and replacing it with a segment
    starting at the current location.
    
    The result is that StreetPreview now seems fairly robust - though if
    you jump to a tile grid edge there will be a delay until the next
    jump can be made.

[33mcommit d82e2a83a4232efc8b3bf0c55e72128784a4edf1[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Dec 15 12:38:19 2024 +0000

    Change approach to thread safety of FeatureTrees
    
    The FeatureTrees are updated when we the tile grid is updated. Because
    the FeatureTrees are also being used in all of the callout and street
    preview calculations we need to take care when we access them. This
    change puts all access in a single threaded context - treeContext -
    which means that the assigning of the trees to new data, and all of
    the callout and street preview calculations should be safely using
    the FeatureTrees.

[33mcommit e41e57339f4cead480e920fb9171a954df1d2854[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Dec 13 12:03:38 2024 +0000

    Add paths to StreetPreview traversal
    
    Include paths in our StreetPreview traversal. Need to check whether iOS
    makes this optional or not.

[33mcommit 95972c6ed23a3f7f8438594c51d1d28b09931ed8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Dec 13 12:01:41 2024 +0000

    Improve interpolated joiner algorithm
    
    Rather than pick the first points which are close, pick the pair that
    are nearest together (within 1.0m). Also skip precisely coincident
    points as they don't need a line to connect them. Add some tracing to
    aid checking unmatched pairs and to see how far apart points can be.

[33mcommit 7e661937a11413934f0defd8d3763fbd8f28be2b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 12 17:05:48 2024 +0000

    Initial StreetPreview work that support user interaction
    
    This change add a tiny GO button next to the StreetPreview icon which
    can be used to advance the StreetPreview in the direction that the phone
    is pointing. The current behaviour is that it jumps to the next intersection.
    
    It's definitely a work in progress.

[33mcommit aad52d692be0bcf39ff1d9634eddd497cb7e3598[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 12 17:01:58 2024 +0000

    Switch long click on map to bring up LocationDetailsScreen
    
    A long press on the GUI map was creating an audio beacon, but it's more
    useful to bring up the LocationDetailsScreen where the user can create
    a beacon or they can enter StreetPreview mode. This makes it easier to
    select somewhere to enter StreetPreview mode.

[33mcommit bbb768274d0829f084331b215a545d6541298ef8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 12 16:58:03 2024 +0000

    Creating a FeatureTreeCollection for Roads resulted in many duplicates
    
    The way that we add lines to the RTree is that each line segment is added
    pointing to the same feature. However, this means that a search will return
    all the RTree entries which are within the search criteria which means the
    same feature will be duplicated depending on how many of its segments fall
    within the search area. This change de-duplicates the feature addition so
    that each feature is only added a single time.

[33mcommit b3a8be15cf6bae9e324603a69ad07ff3e58b07dc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 12 16:56:23 2024 +0000

    InterpolatedPointsJoiner was adding two lines between each pair
    
    This change notes which points have already been joined up so as to
    avoid joining them A->B and B->A. The result is just a single line
    between each pair of adjacent interpolated points.

[33mcommit a34b0752bb3e5e8e770adaaf3e6e2d712664205b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 12 16:46:43 2024 +0000

    Split Location and Direction Providers into their own files
    
    All of the providers were hidden inside LocationProvider, this change
    splits them out. It also adds GpxDrivenProvider which is the start of
    a combined provider that is driven by GPX files. And finally it adds
    updating of the current location to StaticLocationProvider. This allows
    the app to move the location for StreetPreview purposes.

[33mcommit 111baf77dea20cd85e2988fd0595d27b7b52ad73[m
Author: AmiraFAHEM <amirafahem1920@gmail.com>
Date:   Tue Dec 31 13:58:54 2024 +0100

    Search feature (#293)
    
    * search feature
    
    * search feature - LocationDescription refactor
    
    ---------
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>
    Co-authored-by: Amira <amira@hobbys-mbp.lan>
    Co-authored-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit 2ca4d96f0cd2d1a843f3413112dc3b23a0c7d69f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Dec 31 10:44:48 2024 +0000

    Keep service alive when app is closed #305
    
    This trivial change stops the service from shutting down when the app is
    closed. That behaviour came in when we added MediaSessionService and by
    ignoring the onTaskRemoved call we can revert to the previous behaviour.

[33mcommit 1d75b9c115a3313d68e8c57ab0d1eaf396d4e615[m[33m ([m[1;31morigin/remove-in-app-language[m[33m)[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Dec 15 11:16:32 2024 +0000

    Remove trimming of resources to shortened list of languages
    
    Resources that aren't used get trimmed during the build process, but this
    setting removes all resources that aren't in the list. This includes those
    in AppCompat and Google Play Services libraries which the app uses. Given
    that those strings might be used, I think it's reasonable to keep them in
    and it takes away another place where we have to list all of the languages
    that we support and the risk of it getting out of sync.

[33mcommit c6e2e0cbc5faa30de115573c7113f25b36b926cc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Dec 10 13:12:22 2024 +0000

    Add script to convert from iOS string resources to Android and convert all
    
    The instructions in the script are all based on my folder config, but with
    a little tweaking will work for anyone. However, we should only need to do
    this one time as the iOS strings aren't changing. All new strings have been
    manually added at the end of the XML files after the iOS ones.
    
    The string resources within the XML are now surrounded by double quotes.
    This removes the need to escape the apostrophes which appear in many strings.
    
    I've also added in the extra regions supported by iOS - two regions of
    Portugese, two of English and two of French. However, I haven't managed to
    make the app use the regional resources.

[33mcommit 185b0004822c4307224b038d43fe92fa81c386bc[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Dec 6 12:58:15 2024 +0000

    Update release notes

[33mcommit e4f1ec603fb466a1c3821132df223becc9b3c07f[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.44[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Dec 5 15:51:00 2024 +0000

    Bump version to 0.0.44, version code 45

[33mcommit 78b1cf1920b9aa63aab09b464adc2a7206f9a6ee[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 15:34:50 2024 +0000

    Ensure that we use the intersection history to limit callouts
    
    Intersections shouldn't be called out more than every 60 seconds.

[33mcommit 7df38cdd03913f44521a6540a19f5148662a9a4f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 15:18:21 2024 +0000

    Ensure subclass gets passed into feature_value for POI
    
    We weren't matching on supermarkets because their feature_type was grocery,
    and their feature_value wasn't being set. This change ensures that the
    subclass (supermarket) is used and that results in us matching correctly.

[33mcommit 05d457bf5d679878c6875abad63c71e729d331e6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 14:32:51 2024 +0000

    Fix up intersection code to deal with more local roads selection
    
    getGridFeatureCollection now returns a small local subset of roads near
    to the current location. The result is that it's possible for
    roadsGridFeatureCollection to be empty. In that case getNearestRoad was
    returning an empty Feature which added to the confusion. These changes
    check for an empty collection, and also getNearestRoad returns an
    empty collection if that's what's passed in to it.

[33mcommit a05186696f330c5935f5629f1c2308aeb28e9682[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 11:40:19 2024 +0000

    Fixup MVT unit test for FeatureTree

[33mcommit 2a67ec8556762ce3ae9088d68f6fce68df1ef075[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 11:25:10 2024 +0000

    Rename Rtree.kt to FeatureTree.kt and describe it in the comments.

[33mcommit 1ba98e7b7edc1ea46d78d4725c9d5a03c76b3ae6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 11:16:38 2024 +0000

    Add mutex around rtree usage
    
    The GeoTrees are created when updating the tile grid, but there wasn't
    any synchronizatin to protect their use whilst this is happening. Add
    a Mutex to protect them and ensure that calling functions are all
    suspend capable.

[33mcommit 0ad15e192c23546ed8e8c1a6509298c4e8ce5933[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 11:16:02 2024 +0000

    Fix failing unit test CalloutHistoryTest

[33mcommit 5a554d6f966c8f10243e500dabd9a15e8d4a358f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Dec 5 11:15:06 2024 +0000

    Move filters directory inside geoengine where it belongs

[33mcommit 0c604d31dd698e414e167ebefdfa34d5b196b13b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Dec 4 17:06:22 2024 +0000

    Add intersection auto callouts
    
    iOS has an event bus which prioritizes different types of callout. It's not really
    clear why they don't just do them all at once. This is my first attempt at calling
    out intersections. It uses code from the AheadOfMe. It's currently hard to test
    because the simulator is always pointing north and so doesn't always point towards
    the junction that is being approached.

[33mcommit 8d0dccdfab74ff6518ba2a7b6dbf47ba36f7b10a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Dec 3 11:26:08 2024 +0000

    Use rtree to speed up geo searches
    
    The rtree library provides a fast spatial search implementation. There's
    a good introduction here:
    
    https://blog.mapbox.com/a-dive-into-spatial-search-algorithms-ebd0c5e39d2a
    
    On my phone the MvtPerformanceTest reduces a search for POI within a certain
    distance from ~85ms to ~2ms. There's an initial cost of creating the Rtree
    of ~500ms at the point at which the MVT is loaded. Auto callouts are
    constantly searching for nearby POI and so this should be an overall gain.
    
    Lines and rectangles can also be included in the rtree, though not arbitrary
    polygons. Because we're using longitude and latitude we have to provide our
    own set of functions for determining the distance between a point and another
    point, a point and a line, and a point and a rectangle. When creating the
    rtree I'm entering a bounding box rectangle in place of any polygons. Possibly
    entering a polygon as a number or lines might be better? It wouldn't tell us
    that we were inside the polygon, but it would perhaps give us a hint?
    
    The current integration of the rtrees is that they are now used in place of
    the FeatureCollections inside getGridFeatureCollection. The code now creates
    dynamically selected FeatureCollections based on the location and distance
    parameters passed in. This allows callers to reduce the number of Features
    that they have to process from 'all those in the grid' to 'those within 100'.

[33mcommit 83a7862e9a0247f7ab0ae3990f465ddad1049411[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 2 16:36:34 2024 +0000

    Fix earcon playout issues
    
    There were earcons missing on polygon POIs, and some erroneous errors from
    the playback. This change fixes those.

[33mcommit e796dbd631f03c0f3342e0da905e1f9910932575[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 14 16:54:46 2024 +0000

    Remove database for tiles
    
    Instead of using the database as a tile cache or decoded GeoJSON, simply do
    all of the conversion work whenever the tile grid changes and keep the result
    in memory. getGridFeatureCollections returns the result instantly from memory
    and so callouts have less delay.
    The implementation doesn't try to be smart so it throws away the old grid and
    creates the FeatureCollections from scratch each time. It doesn't reuse tile
    data from the tiles that might overlap with the previous grid.
    The change works for both soundscape-backend and the protomaps backend. The
    time taken to create the grid is considerably longer for soundscape-backend
    which obviously might explain why it was originally required (~400ms vs.
    ~3 seconds for the whole grid).

[33mcommit 803c7cd8b1ba468f6b15b99d9ffe697f5592d01b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Dec 1 14:11:36 2024 +0000

    Initial work on autocallback
    
    WIP Ongoing

[33mcommit f59962e17bcbf9373ea0fbc578dea91d924b6990[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Dec 2 16:35:16 2024 +0000

    Fix home screen map to have north at top
    
    Rotating the whole map was proving hard work for lower power phones,
    so switch to our mode where the icon showing the user location rotates
    instead.

[33mcommit 80c1ec51876f1740113c12f657d9094a5e4c791f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Dec 1 12:48:47 2024 +0000

    Alter the pixel ratio used when rendering the map
    
    On my phone at least, the text on maps is too small and the roads too
    narrow to easily read. Zooming in doesn't help with this as they are
    still small, just spread further apart. Changing the pixelRatio on
    the map to be 4.0 results in larger text, icons and roads which is
    better size wise. The rendering is a bit 'soft', presumably because
    it's being rendered at 1/16th size and then scaled up, but it's still
    an overall win.

[33mcommit a767e1611b42e3dcb6b38a4420a74e567c1f5d20[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Sun Nov 10 20:06:54 2024 +0000

    fix: combine TextField with label for better usability with talkback simply by using the label parameter of material TextField component

[33mcommit 958f41a28cee0b9691a4be2baaa407c23cfc6a8a[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Sun Nov 10 17:05:13 2024 +0000

    fix: improve contrast of markers and routes menu items, and make selection visibility based on shape in addition to color for colorblind person

[33mcommit 8e4e801d067f49de93e3b92001876eb0471f321c[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Nov 1 17:03:32 2024 +0000

    fix: Improve discoverability with talkback of the add button in route screen

[33mcommit 87f5084ff141bcca2ab71e91e49bd1abc6d6416c[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Nov 1 16:39:33 2024 +0000

    fix: Button at bottom announce “ My “ + pause + “Location”.

[33mcommit 6dc3436c78d49d3ee3ba535a2a6b0fe8d1e070a8[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Nov 1 16:26:13 2024 +0000

    remove hello dave

[33mcommit 815ff05c5dc2afdd9b53fa4ffaaefcc0b333f76d[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Nov 1 16:25:37 2024 +0000

    fix street preview mode icon which didn't had disable state + i18n

[33mcommit 4347244e87344a6445f4443d6e4c0660fdd89aa0[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Nov 1 15:23:29 2024 +0000

    fix issues related to headings and contentDescription

[33mcommit dc13f25be2de5e816d1bda2f71317e2f8b4041c0[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Nov 1 11:21:47 2024 +0000

    fix: Remove focus on “Accept Term of use” alone, as focus goes to checkbox and text before

[33mcommit 4beede6579b484ba7796f9d416b39d7f44514189[m[33m ([m[1;31mandy/main[m[33m)[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 22 15:45:45 2024 +0000

    Fix issue 267 so we detect nearest named paths as well as roads (#292)

[33mcommit fc21800cae6c8249ee29e65f7346802cf6b5f6ea[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 22 14:27:41 2024 +0000

    Fix issue with duplicate/quadruplicate POI in MVT tile grid (#291)

[33mcommit 4d598070af1a93abde2297075afa35ed08b0dd07[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 22 10:11:23 2024 +0000

    Translation strings for "Travelling north along $road" and "Heading north along $road" (#290)

[33mcommit 0d22af3d2dbb4bcc664d3e38e81d065bfa777d6e[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 22 08:03:32 2024 +0000

    Translation strings for heading and travelling in a direction. (#289)

[33mcommit fe3a1ebd1cdbffe874089e753edbeacc0025c59f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Nov 20 10:56:39 2024 +0000

    Split MVT to GeoJSON translation into separate files and add GeoEngine Configuration
    
    The GeoEngine configuration contains the grid size and zoom levels along with the
    servers. This used to be hidden away in TileGridUtils.kt which wasn't the best
    place for it.

[33mcommit 0f54eaa50320cbb589ad1efc68dc20727d7f474e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Nov 20 10:29:18 2024 +0000

    Move geo utils files into a geoengine utiles sub-directory
    
    This is a minor move really, but the aim is to have all geo functionality
    within the geoengine directory.

[33mcommit 0fa73f3c065ea75482b29c1fe6f6fb3c1147ec65[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Nov 20 10:26:36 2024 +0000

    Add whatsAroundMe to Play/Pause media button
    
    Again this isn't what iOS does, but it's useful for initial testing.

[33mcommit 984fd28a50fc111e27d61f13849c35f86d512bf7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 19 11:55:35 2024 +0000

    Add support in MVT -> GeoJSON translation for named buildings
    
    This change means that the buildings layer is now parsed and any which
    are named are added as Features. This will include warehouses (which was
    the aim), but there'll be other additions too. The buildings are
    de-duplicated against POI.

[33mcommit dce220693badf0c6f87d4679175c1356169c5f25[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 18 17:27:12 2024 +0000

    Add entrance features including the name of the POI
    
    The code here is similar to the road intersection detection code in that it's
    matching up coincedent point in a tile. In this case finding the POI polygon
    for an entrance. At present it still uses the OSM id for the entrance rather
    than the POI, but it gives us the possiblity of extending the behaviour in
    future.

[33mcommit 2d26a1e2107ceff32e66ed86d127aa86fd2ff743[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Nov 17 10:11:01 2024 +0000

    Add configuration of protomaps server to ease local testing
    
    This change consolidates the location of the protomaps server configuration
    allowing it to be easily switched between cloud and local for developer
    testing. It also lays the groundwork for having the server URI be a secret
    as we now parse the `style.json` used by maplibre and dynamically insert the
    URL.

[33mcommit 7604e9a1178a49397729fc60c811cbcc94515328[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Nov 15 12:08:02 2024 +0000

    Fully populate possible highway types from openmaptiles
    
    The previous list was only partial. This change adds all the types listed
    in the openmaptiles mapping.yaml for the transportation layer. The protomaps
    layer is condensing some road types (`residential`, `living_street`, `road`)
    into a single type of `minor`. It also does this for paths, though the subclass
    is used to store the data that was condensed e.g. `pedestrian`, `footway` etc.

[33mcommit f0790410c68c41544e2e64a0dd865aad4e1590c9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Nov 15 12:56:45 2024 +0000

    Always write out GeoJSON from unittests
    
    The GeoJSON will end up in the app/ directory with names like:
    
      16141-16142x10906-10907.geojson
    
    which describes the tile range of geojson. .geojson files are already
    in the .gitignore.

[33mcommit cf99247f1b0cb0c5acf1ecfc4c871e79ef3c9034[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Nov 15 11:39:36 2024 +0000

    Fix intersectionsCross3Test test for MVT tiles
    
    soundscape-backend was providing slightly different GeoJSON than MVT for
    paths marked as pedestrian. This change fixes that so that instead of
    being `highway=path` they are tagged `highway=pedestrian`. There may be
    other tweaks needed here too, more testing required.

[33mcommit fa3d590dd98cea10b02f2851aa9dded3382956f1[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Nov 15 11:35:06 2024 +0000

    Add function to get 2x2 grid from MVT files for unit tests
    
    In order to test the MVT -> GeoJSON -> intersection code in total we want
    to process the MVT files within the IntersectionsTestMvt tests. This change
    adds a helper function getGeoJsonForLocation which can be used by those tests
    to create a FeatureCollection from the MVT resources files. update.sh has
    been updated to fetch the tiles required for those tests.

[33mcommit 91030f50debe61fe9e849ad64d0a58a9c24ebbf7[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Nov 14 20:05:14 2024 +0000

    Converting unit tests to use new GeoJson from Mvt tiles (#281)
    
    * Converting unit tests to use new GeoJson from Mvt tiles
    
    * Converting unit tests to use new GeoJson from Mvt tiles

[33mcommit 88aec1393a7e33a2a7c32e71cc713348fdeb7060[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 14 14:14:53 2024 +0000

    Don't deduplicate in getGridFeatureCollections with protomaps backend
    
    All the deduplication has already been taken care of during the MVT to
    GeoJSON translation and no further deduplication should be done. Because
    we can have multiple segments of the same OSM id (e.g. separate ones
    where a road crosses a tile) we actually can't de-duplicate.

[33mcommit 7d4019e12053b2924e7b8c0bb180826ad636b429[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 14 11:47:54 2024 +0000

    Add road/path joining at tile boundaries to getGridFeatureCollections()
    
    getGridFeatureCollections is called to get the current tile grid whenever
    a callout is about to be calculated. This change adds tiny roads to join
    any roads/paths which cross the tile boundaries.
    
    As part of this, the tile database schema changes. I've added code so that
    if there's an exception opening the database is simply deletes it and tries
    again with a fresh database. That's okay for now, though may need more
    thinking about in future.

[33mcommit 77636738e848622bba49db14f1c206fbf858cd4e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 14 11:38:08 2024 +0000

    Use Double instead of Long for OSM id in MVT to GeoJSON conversion
    
    Long isn't a valid type for JSON and OSM id can in theory be up to 64 bit. Double gives us 53 significant bits which is 10000 times what we currently need, so use that instead.

[33mcommit 6e7bb56b89bfe8e7516f1aeee060281472017df8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 14 11:21:43 2024 +0000

    Remove Long support from GeoJsonObjectMoshiAdapter output
    
    When roundtripping GeoJSON this was causing an issue because although
    it could write out a Long it didn't know how to read it back in. JSON
    only understands Int and Double so remove the Long support.

[33mcommit 54a42257f8a77986fcf5d2f803165744bda1c0fb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 14 11:14:40 2024 +0000

    Add test to check JSON roundtripping of OSM ids
    
    OSM ids are 64 bit values. However, JSON only supports Int and Double.
    The test added here is to prove that using Doubles will be okay for the
    a very long time indeed.

[33mcommit 1cc916ced077d41414491122b23bbdcfce1a96fc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Nov 13 10:52:00 2024 +0000

    Save interpolated edgePoints in tile GeoJSON
    
    All the interpolated points that are created where roads/paths end at
    the tile edge are now added to the GeoJSON as edgePoints. This is a
    MultiPoint of all of the interpolated points for single OSM id. This
    can then be used when we stitch together adjacent tiles.

[33mcommit e8396eba1628ee42fc793428e861389f35dde4ac[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Nov 13 14:57:41 2024 +0000

    Function to get the shortest route between start and end locations and a roads FeatureCollection. (#277)

[33mcommit 619ac20d03d49c4f800564b5969f1e8400fee6e8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 12 16:13:47 2024 +0000

    Remove tile provider steps from GitHub actions and build file
    
    We don't currently need a tile provider API key so remove it from
    GitHub actions and the build.gradle.kts.

[33mcommit 4e706b3c77d56e167cb2d778caaed1b6553ff032[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 12 15:56:30 2024 +0000

    Add cropping of roads and paths to within the tile boundaries
    
    Interpolated points are added to lines which cross the tile boundary
    so that they can be accurately cropped. If a line comes back on to
    the tile, then a new segment is created again interpolating to the
    point at which it crosses back into the tile.
    The accuracy is such that if I view the GeoJSON using JOSM the
    interpolated points from two adjacent tiles appear to be only a few
    centimetres apart. This is better than the large overlaps which we
    previously had and should make it easier to join up between tiles.

[33mcommit a1232b06b374517901082acf1a700e0c176ebc30[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 12 14:13:03 2024 +0000

    Don't add intersection at points where identical line segments meet
    
    If a path has been drawn in two parts, the point where one ends and
    the other begins is not an intersection. This commit checks for that
    situation. Without it we can end up with intersection in the middle of
    a path/road etc.

[33mcommit f1c03ff3c8da498d107104ed3354c19698641077[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 12 12:59:47 2024 +0000

    Update tests for new protomaps release
    
    The map now contains crossings and polygons for POIs.

[33mcommit 463cea562f70c83aa1ee11421c0b4e38095624be[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 11 16:42:16 2024 +0000

    Handle POI duplication of polygon/point
    
    The latest protomaps maps can have the same POI stored as both a point
    (used by maplibre) and a polygon (used by our GeoEngine). This change
    handles the deduplication required in our MVT to GeoJSON translation
    code. It simply stores POI features in maps, one for polygons and one
    for points. Once they've all been processed, the polygons are added and
    any non-duplicate points POIs are then added too.

[33mcommit 8047482d853c01ccfc1a8c25075d9ca0eeb8994d[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 11 16:40:40 2024 +0000

    Update docs to improve coverage of our latest mapping data generation

[33mcommit e36d614f13a54f8e2b190750f5368f7e9b0fed53[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 11 15:02:33 2024 +0000

    Add support for crossings to MVT -> GeoJSON translation
    
    Initial addition for crossing support for protomaps backend.

[33mcommit 247e415ebabf9767676cfbf118387b8ddc84a1b1[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 8 18:06:10 2024 +0000

    Fix check intersection (#273)
    
    * Fix checkIntersection
    
    * GeoJSON test data from Mvt tiles in 2x2 grid

[33mcommit e349d773954a9ac89711af7f13a97cced0587d4a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Nov 8 16:48:24 2024 +0000

    Update docs for not needing tile API key
    
    We no longer need an API key for the tile provider.

[33mcommit 23b93a2f33d2ad70e2a246b06eaeadac5f45e0ef[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Nov 8 16:41:53 2024 +0000

    Add README.md in root directory that points to our pages website.

[33mcommit d74ea0fa77dca8a057f80db8052386eeb3648ec0[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 8 15:33:50 2024 +0000

    Revert "Mvt tile testing (#270)" (#271)
    
    This reverts commit d54fcdf078c7d6686c0f20b4159c98f863188a49.

[33mcommit d54fcdf078c7d6686c0f20b4159c98f863188a49[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 8 15:14:49 2024 +0000

    Mvt tile testing (#270)

[33mcommit 9bb3492b12ebbdc267a606e2ed2078e6ed1a4e16[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 7 14:52:47 2024 +0000

    Allow passing in of zoom level to vectorTileToGeoJson
    
    This is so that tests can pass in the zoom level without relying on it
    being set correctly in ZOOM_LEVEL. This allows soundscape-backend and
    protomaps backend both to be tested by unit tests.
    
    Add a check to vectorTileToGeoJsonFromFile to ensure that all of the
    points within the featureCollection are within the extents of the
    buffered tile.

[33mcommit 6cac17bb5b8059f256f19dbc5affe476034cd1ac[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Nov 7 09:50:08 2024 +0000

    MVT testing changes (#268)
    
    * Increase the size of the test grid in MvtTileTest
    
    Just to increase the coverage of various feature types that I'm interested
    in testing.
    
    * Add tricky location for pr#267

[33mcommit 922eaaafb1d1c64b4f42ce3b720a7b37bbba9e0a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Nov 6 14:36:03 2024 +0000

    Fix crash where there is LineString -> intersection -> LineString and the LineStrings have the same name. (#266)

[33mcommit 47211be9b43702edb335e5c9f1bc8bf7f8197b3e[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Nov 6 13:54:19 2024 +0000

    StreetPreviewTest2 with callouts (#265)

[33mcommit a46358510d202958da0bee3d00a8e83d90d6efc9[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Nov 6 12:41:46 2024 +0000

    Add code to detect LineString segments intersecting with Field Of View. (#264)

[33mcommit 0e2119966be9d0e9dc7c6c4d10edd7acdd618429[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Nov 6 11:50:11 2024 +0000

    Explode a triangle(Polygon) into linestrings/segments. (#263)

[33mcommit 050fcb205d08f932d327f92a618a8c7603ceb9b9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 5 14:12:10 2024 +0000

    Add aheadOfMe callbacks on media key for testing
    
    Whilst wandering the streets testing it's useful to be able to trigger
    aheadOfMe callbacks from my bluetooth remote control instead of having
    to get phone out.

[33mcommit d8a2552d70b57d1f2b95a95b38b7a524fe5c5d8e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 5 14:10:39 2024 +0000

    Sprinkle array size checks through GeoEngine code
    
    Quite a lot of instances where arrays are assumed not to be empty that
    could cause crashes. Add extra protection to those and change size checks
    to use isEmpty() and isNotEmpty() which are faster and clearer.

[33mcommit 770b2f04482ac285857ca92aee3dd5e9296926c5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Nov 5 13:23:07 2024 +0000

    Adjust vectorTileToGeoJson to not use transportation_name layer
    
    We've added names to all lines that have a neam in the transportation
    layer and so we no longer need to create a dictionary from the
    transportation_name layer. We do this because that layer had merged lines
    which lost OSM ids and made de-duplication impossible. The names are only
    stored once in the MVT so it's a fairly efficient addition.
    
    I've also added some more test tiles to increase testing coverage.

[33mcommit 214214d49fd6904d6df959dae2251bfed57c2f23[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 4 10:39:22 2024 +0000

    Fix errors in previous commit
    
    Code wasn't actually setting the name having figured out what it was.

[33mcommit a6035ec6917d0141bf4e02ce60e152ffb6d24fc3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 4 10:02:49 2024 +0000

    Refaction intersection detection in MVT to GeoJSON translation
    
    The tiles from our protomaps server have been updated so that every feature
    in the transportation layer have an OSM id. This prevents un-named features
    from being merged into multilines which made it impossible to de-duplicate
    reliably. The changes here make use of this so that we now generate much
    better GeoJSON without duplicate roads (e.g. prior to this change Roselea
    Drive had 4 lines, now it has 1 with an overlap at the tile boundary).
    The explanatory comments in the code have also been extended and updated.

[33mcommit 71ddbf3491b3d468776c09ee537b79646a76d945[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Nov 4 09:45:26 2024 +0000

    Updat MVT test files from latest protomaps server update
    
    OSM ids have been added to all features in the transportation layer, so update
    the test vectors to be the latest that we're serving up.

[33mcommit c90c81cd7c37d455209edcf2b9a7dcd1f30864b9[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 1 18:23:52 2024 +0000

    Proof of concept for tracking along a road using Field of View triangles (#258)

[33mcommit 367cb3d867de866a9c33680e0da80a4ca47879de[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 1 15:53:43 2024 +0000

    Fix traceLineString bugs (#255)

[33mcommit 6acd35a2d4c7ee73d8a8f8998a2679d46abeeee1[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 1 11:05:06 2024 +0000

    Added explodeLineString and traceLineString functions (#254)

[33mcommit 4e624df2d4fd0231e379e6ee07f5cfe9374ea85b[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 1 06:52:23 2024 +0000

    Sort whatsAroundMe callouts by distance (#253)

[33mcommit 60e0efff1a17b8e652371b7b6318dd6392cbd1ec[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Nov 1 06:13:56 2024 +0000

    Increase cache size from 5 to 100 (#252)

[33mcommit eac4a53024faa27a7dc83d938bf7194dc1039240[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 31 12:52:53 2024 +0000

    Move OSM id deduplication code to TileUtils and use it for removeDuplicateOsmIds
    
    I mistakenly removed OSM id deduplication from removeDuplicateOsmIds, this
    reinstates it reusing some shared code.

[33mcommit e9e9b68cf3e5b5dab55990308e4aa4e9a1ce069d[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 31 12:02:56 2024 +0000

    Add caching to protomaps tile client (#250)
    
    This change moves the soundscape-backend specific parts of TileClient out
    into a sub-class so that a protomaps TileClient can share the caching code.
    
    It also removes the unused http caching from MainActivity - we no longer
    use the type of HTTP access that would use this cache.

[33mcommit 8ee2169ec603ce44b1b73c88890087598bf47ef0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 31 10:11:22 2024 +0000

    Improve error handling when updating tile grid (#249)
    
    Handle exceptions due to lack of network, and ensure that we retry if any
    tiles fail to be updated.

[33mcommit 67e9674acc792f7ad7bfe90fa7be9e19eb5468de[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 30 18:50:45 2024 +0000

    Add ability to switch to using protomaps MVT as source of GeoJSON (#248)
    
    Changing SOUNDSCAPE_TILE_BACKEND to be false will switch the app to use
    the protomaps tiles instead of those from soundscape-backend.
    
    The MVT to GeoJSON test code has been moved out of the unit test and
    into it's own file. It's been adjusted to give a more accurate mapping
    to what soundscape-backend providers including adding osm_ids based on
    the protomap id values. With these in place the various call outs seem
    to work, though there's obviously much more to do.
    
    During the process I refactored getGridFeatureCollections to be a much
    more concise function and added some enums to share with the calling
    functions.
    
    Our VectorTile class has been renamed Tile to match TileGrid because
    there's another VectorTile class which is being automatically generated
    from the MVT protobuf file. It's confusing to have two classes with the
    same name, and it's not possible to easily change the protobuf file, so
    I renamed our one.
    
    For now, because the MVT to GeoJSON is a work in progress, for now the
    tile database is always discarded when the app is started. That's because
    it contains GeoJSON generated by the conversion code and we want it to
    update when the code changes. We'll have to think of a longer term
    solution to this (versioning or the protobufs go in the tile database and
    they are always reparsed? Or we just rely on the HTTP cache?).
    
    Co-authored-by: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>

[33mcommit 51bdc6ba802bb97612b76383eb3a7ebb8d695d6d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Oct 30 18:30:10 2024 +0000

    Proof of concept for local routing based on GeoJSON FeatureCollection containing roads as LineStrings (#247)

[33mcommit 3482245805375fa371548ac67812684be490fa63[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Oct 28 12:57:15 2024 +0000

    GeoEngine tile update cleanup
    
    This commit consists of multiple overlapping changes all with the same
    aim which is to add an extra layer of abstraction to the tile grid so
    that we can cope with different sized tiles for our audio UI.
    
    Features:
     * The upgrading of the tile grid is no longer done on a timer, but
      when the location moves out of the central grid region. This makes
      it much more up to date.
     * Display of tile grid outline and 'central area' region on map UI.
      This can be enabled from the settings and is to allow easier debug.
      The feature is only visible in debug builds. The grid shown is the
      one currently in use by the GeoEngine. Note that when changing the
      setting the service needs a manual restart (sleep/unsleep).
      The new GeoJSON layer in our maplibre UI shows that there's
      potential for other debug uses.
     * Support 2x2 grid in addition to 3x3 grid. The reason for this is
      that the protomaps vector tiles are at zoom level 15 and not zoom
      level 16. A 3x3 grid at zoom level 15 is more data than we need,
      so we can now switch to using a 2x2 grid. The central area of the
      grid is where the current location can be without resulting in a
      new grid being generated. Once the user strays outside it, then a
      new grid is required. Although this was only required for 2x2, the
      same logic is used for 3x3 grids too. The result is that there's
      now some hysteresis so that if a user is walking down a tile
      boundary the grid won't continually be updated as they stray back
      and forth from one side of the tile boundary to the other.
    
    Minor tidying:
     * All zoom levels are now Int rather than Double.
     * The grid size and zoom levels for the TileGrid are now declared
      inside the TileGrid class. They could be private and const, except
      that the unit test uses them.
     * Moved getTileGrid functions into their own file and it now returns
      a TileGrid object which contains the central region as well as the
      list of tiles.

[33mcommit eddf0bd1dfcf25d9e3aa8a718d02e6c1653fbdb2[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Oct 26 13:18:36 2024 +0100

    Add Bus stop detection to aheadOfMe (#245)

[33mcommit 36ff7fe1112403564b58ccbbf2a8274b9763e4bc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Oct 22 09:52:58 2024 +0100

    Move FMOD libraries to jniLibs directory and remove unused versions
    
    There was a problem building on one particular Mac when the FMOD library
    wasn't being installed in the APK. This change moves the libraries to
    the jniLibs directory which forces them to be installed. We weren't
    using the logging version of the FMOD library, so those versions have
    been deleted. The CMakeLists.txt file has been updated to handle the
    new location.

[33mcommit 52cc1d4b13bc679d1f9f5e347624a257e4bb2df5[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Oct 25 16:21:51 2024 +0100

    Update aheadOfMe to detect crossings and some complex intersections (#243)
    
    * Update aheadOfMe to detect crossings and some complex intersections
    
    * Update aheadOfMe to detect crossings and some complex intersections
    
    ---------
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>
    Co-authored-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit 6f9d3f02c445ec4a6e9a25b1e52c3d31acb945fe[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 25 12:12:43 2024 +0100

    Add space after full stop to add speech engine
    
    See issue #201 for discussion on this. Some speech engines read out
    full stops as 'dot' if there's not a space following them. This
    avoids it in this one situation.

[33mcommit 9cad9f2fe4484655d80bf95ec9b14571df63a42a[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Fri Oct 25 10:35:25 2024 +0100

    Fde/a11y improvement language selection (#224)
    
    * use dropdown instead of list
    
    * improve test for language screen
    
    * add translation and fix checkbox color in TermsScreen

[33mcommit a4bbe802171d458ca6f8c1b3a813e3e5d1fc2331[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 25 11:45:04 2024 +0100

    Use system Locale when an application Locale isn't set
    
    There were several places where it was assumed that there was a valid
    application Locale. This commit replaces those with a call to a utility
    function which returns the application Locale if it's valid or the default
    system one if it's not.

[33mcommit b95eab1d33f1b85294ad3b28ad48833c15af91f7[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Oct 25 11:36:58 2024 +0100

    Add Crossing detection to Intersection detection (#240)

[33mcommit 1b1a59eeb86b002013043b76775cab6bd3d9273a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 25 10:37:58 2024 +0100

    Add option of scrolling map in MapContainerLibre
    
    The map on the main Home screen should always be centered on the phone
    location, but for other maps it's useful to be able to scroll away from
    the current location. This adds that control and enables it within the
    LocationDetailsScreen.

[33mcommit b52c396aa40aea6aad014513086f18211be0bde2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Oct 23 13:53:31 2024 +0100

    Prototype function to calculate the nearest point on a segment of a polygon and return the coordinates of the point

[33mcommit a62a515c38baa995af480458da8c97c0c1720a8e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Oct 22 12:33:48 2024 +0100

    Switch to using our own protomaps server and built in maplibre style
    
    The main change here is to store as assets the files required to implement
    the osm-bright-gl-style in the maplibre UI. These consist of fonts, sprites
    and the main style description. The tile server URL used provides the TileJSON
    description of the server which includes attribution for the data sources used
    in the maps themselves.

[33mcommit 0cb24212d8a1972908147fdaf13966dfd8476216[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Oct 23 10:38:51 2024 +0100

    Updating the straightLinesIntersect() function to catch some cases (#234)

[33mcommit 202e5c34e6aeaf4a05d35f1b7296f1fe8b0077d0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Oct 21 14:08:23 2024 +0100

    Move geo functions out of SoundscapeService and into their own class (#231)
    
    There's nothing clever going on here, mostly just moving functions around.
    The only other changes are:
    
     1. The locale configuration is created and held in the GeoEngine to save
      re-initializing it each time it's required,
     2. All functions that require the application context are made within
      the start() function so that it's only required in that one place.
     3. The myLocation, whatsAroundMe and aheadOfMe functions now all return
      lists of Strings (or PositionedStrings) so that the text can be used
      in the UI and not just the speech.

[33mcommit 4a6b1edd4c680b22781c43f5194758544fd410d9[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Oct 19 21:01:33 2024 +0100

    Determine if linestrings intersect (#230)

[33mcommit 6810a17f9cb4a67e9f37d49ad91607cff72810e4[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 18 15:31:34 2024 +0100

    Add initial Earcon support to audio engine
    
    The iOS Soundscape has lots of audio 'earcons' which are used to indicate
    state changes happening in the app e.g. going online/offline from network,
    or arriving at a beacon. This initial support allows for positioned (and
    non-positioned) audio file playback which is queued along with the text to
    speech.
    
    See textAndEarcon for an example of how to insert an earcon.

[33mcommit 7ada3836ef5e46872e734746f1b88cb2af52105f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 18 12:19:49 2024 +0100

    If no TTS voices are found allow mis-matched locales
    
    I installed the Vocalizer TTS app which gives me access to a much wider
    range of voices. They sound better than Google, are well named, and seem
    to do a better job with Street vs. Saint abbreviations. However, the
    locale for the voice I chose was "eng" rather than than the "en" which
    was in our locale and so the voice was ignored. If no voices are found
    when checking the list from the TTS engine this change means that it's
    re-checked this time without matching on the locale language.

[33mcommit 0024fa72fdb0396a2c91a85b185fe9abe5b5cd91[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 18 11:23:09 2024 +0100

    Update audio engine immediately if any settings change
    
    The main aim of this change is to update the audio engine whenever any
    of the audio settings are changed. This is done by listening to changes
    on the SharedPreferences from within the NativeAudioEngine. As well as
    updating the audio engine, changes to the speech settings also trigger
    some speech so that it's easy to fiddle with settings and hear their
    results. This behaviour is disabled for AudioEngine created by Hilt
    injection as those are running during onboarding when not all of the
    preferences have been configured.
    
    Another complication is that prior to this change the SettingsViewModel
    was creating its own audio engine just to get the possible
    configuration options. This has been changed so that now it connects
    to the Soundscape service and uses its audio engine.

[33mcommit 85a766f35404dbc392b34b8e307cfab613cba2e8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 18 09:27:09 2024 +0100

    Update application locale when language is selected
    
    Without this change the UI stays in English during onboarding.

[33mcommit eae3ad3319845bf694a6b956639b157e0ca6c65e[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Oct 16 13:24:14 2024 +0100

    fix PreviewTest

[33mcommit 87840a5d3c0f989fd4d33f86eed6c1becf144f1b[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Oct 15 20:29:02 2024 +0100

    refactor finish

[33mcommit 387c359a7bca1217c2c6cb987c2c0dc44f6d7aeb[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Oct 15 20:19:40 2024 +0100

    refactore Terms

[33mcommit 8b31832b220e8051ccac022fcb07ec4ed43d11f5[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Oct 15 20:08:06 2024 +0100

    refactor audio beacons

[33mcommit aa5f900096bfbc656ff69d328daf609f1da73256[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Oct 15 11:18:47 2024 +0100

    refactor hearing + remove center text in navigating as it breaks the alignement when text need 2 lines

[33mcommit a274a9303a4ba959611ea714604b867c8cb7b8e7[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Oct 15 11:07:07 2024 +0100

    refactor listening screen + language (manage enabled status of continue button) + navigating screen (merge elements)

[33mcommit 675d9e14fb2847128c176b5b856bea695b8b0af5[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Oct 15 10:31:19 2024 +0100

    refactor Navigating scren

[33mcommit 540e8085833f946df44c0bb35b2eb40c73e38821[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Mon Oct 14 14:38:25 2024 +0100

    refactor language screen + welcome screen

[33mcommit ca10226a0b13562bdb672f0018af50b80046a088[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Sun Oct 13 17:56:53 2024 +0100

    use webP for sounscape_alpha_1024 image instead of png (93% lighter)

[33mcommit 47fb1a3936e5baab7742cdbbc8f01e6d22289a5d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Oct 17 17:42:24 2024 +0100

    Compound intersections and roundabouts. (#225)

[33mcommit 63b6e13749467bc80019f5e26b2cc78361a8eefb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 11 15:17:02 2024 +0100

    Update settings with some more audio control and refactor
    
    The main aim of this change was to add a list of supported TextToSpeech voices
    and control of the rate of the speech. To do this, the Settings screen needed
    some work:
    
     1. It didn't scroll when there were too many settings to fit on screen
     2. It didn't deal with the delay in the TextToSpeech engine initialization
    
    The change fixes both of these. The second is done with a flow from the audio
    engine to indicate when the TextToSpeech engine has been configured. When that's
    received the Settings screen will recompose with the updated values.
    
    The main issue with the voice names is that their poor! There doesn't seem to
    be any way to get a description of the Voice from the API, so we're left with
    their names which are ugly to read. There are various online threads which
    suggest just mapping these to made up names - or Voice 1/2 etc. but that's not
    been done yet.
    
    The second issue is that as with all of the other settings, the service has to
    be restarted to use any changes in value.

[33mcommit 303da3281070d921141041b31eadbe48b4591401[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Oct 11 12:58:31 2024 +0100

    Simple Crossing detection (#221)
    
    * Bus stop test which highlights how inaccurate the underlying data for bus stops can be.
    
    * Simple crossing detection which in this case are just Points.

[33mcommit 81dd43ac35fbb01bbaf10404df63f377c51689d0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 11 11:28:39 2024 +0100

    Revert "Make Play and Play/Pause media controls synonymous"
    
    This reverts commit 7f7d19e3b842f96af0036ddeba01d952572dbc4c.

[33mcommit 80c5dd323e70feff50e926f9421ec4562269c176[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 11 11:28:39 2024 +0100

    Revert "Add draft upload to PlayStore to relase build action - DEBUGGING ONLY"
    
    This reverts commit 5e3205f7787d5b9a77fb79778808e21f17602d43.

[33mcommit 5e3205f7787d5b9a77fb79778808e21f17602d43[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 11 09:47:09 2024 +0100

    Add draft upload to PlayStore to relase build action - DEBUGGING ONLY
    
    Also rename the file for the action to something more accurate.

[33mcommit 7f7d19e3b842f96af0036ddeba01d952572dbc4c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 11 09:31:06 2024 +0100

    Make Play and Play/Pause media controls synonymous
    
    Headphones seem to send one or the other of these button events.

[33mcommit 48344580ecdad40222449788b93d484ed49afd62[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Oct 11 09:49:19 2024 +0100

    Bus stop test which highlights how inaccurate the underlying data for bus stops can be. (#218)

[33mcommit 7e5ce5fb0a1da921fca24503afb95cc49e3e5f02[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 10 16:40:24 2024 +0100

    Add support for Media Transport remote controls - Issue #141
    
    In order to receive Media Transport button events in the service we need
    to have the service derive from MediaSessionService. We pass into that
    a dummy media player based on SimpleBasePlayer and then install a callback
    to get the Media button events.
    I can see all the events that I've sent from my headphones and remote
    control being logged. I've only hooked up skip-next as it's the one that
    we already have code for `myLocation`. The others will be simple to hook
    up once we've done more of the callout code.

[33mcommit 6dce0ee455fe539b4d2fe9c4a95d6309f212ec7c[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Oct 10 14:23:44 2024 +0100

    Simple ha ha compound roundabout (#216)
    
    * change toString longitude latitude ordering to match GeoJSON
    
    * fix bug in calculateCenter function and add pointOnRightSide function
    
    * add minimum size segment (3 coordinates) that test will work for.
    
    * Added a Circle data class to return center coordinates and radius and updated unit tests and GeoUtils to work with Circle
    
    * Add doc string

[33mcommit 55db82244c83cf6850d06e389cfb0cc266dda07f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 10 09:18:56 2024 +0100

    Nightly build Robo test was limited to 5 minutes, bump it up
    
    Now that the Robo test is running for the full length of the timeout
    bump the duration up to 30 seconds.

[33mcommit 2fe2208f8287d340370a3ccf04a89b5b16a6f685[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.43[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Wed Oct 9 16:20:09 2024 +0000

    Bump version to 0.0.43, version code 44

[33mcommit 2db1ee3557973a6658059d78d65c0530f590659b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 9 16:53:41 2024 +0100

    Fix memory leak when stopping Soundscape service
    
    Leak Canary found a memory leak in the LocalBinder class within the
    Soundscape service. The implementation was straight from the Android
    docs, but I guess it's not so common to disconnect from a service
    when the app isn't shutting down too?
    The main fix is to move the LocalBinder class out and add to it a
    reset() function which removes the reference to the SoundscapeService.
    When the service is destroyed it calls reset() on the binder and then
    the garbage collection can continue unhindered.

[33mcommit b8e820a250c03d2451b5f9037073af191038824b[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Oct 9 16:37:21 2024 +0100

    Segments circles and geometry (#212)
    
    * fix "Ahead of Me" button onClick
    
    * Unit test for GeoUtils.kt function and doc strings

[33mcommit 8758a3fee478d93880e2d19a43feadd85f312039[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 9 11:38:11 2024 +0100

    Fix memory leaks in MapContainerLibre
    
    The listeners and symbol management that happens in the LaunchedEffect
    code were not being removed/deleted when the composable was being disposed
    of. I tried changing the LaunchedEffect code to be DisposableEffect, but
    the MapView had already been destroyed by the time the onDispose code was
    called. Instead the clean up code is passed in to the MapViewHelper so that
    it is called just before onDestroy is called on the MapView. This resolves
    the memory leaks that Leak Canary was spotting when locking/unlocking the
    phone or swiping away the app and reopening it with the service still
    running.
    To ease this behaviour, the call to rememberMapViewWithLifecycle was moved
    inside MapContainerLibre and I've confirmed that rendering is still correct.
    I've also changed the function signature on onMapLongClick so that it
    returns a Boolean. This means that it can just be passed in to the mapLibre
    calls without using the arcane syntax that it had previously.

[33mcommit 19358c52af205bb5bd9539caa747f434a91718c2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 9 09:45:06 2024 +0100

    Fix SoundscapeServiceConnection related memory leaks
    
    There were various issues here that resulted in memory leaks spotted
    by Leak Canary.
    
     1. SoundscapeServiceConnection was a singleton and bound to the Soundscape
      service. If the app is swiped closed but with the service still running,
      then the SoundscapeServiceConnection would still be present along with
      a reference to the MainActivity that it used to bind to the service. That
      reference was keeping the MainActivity from being garbage collected.
    
     2. The HomeViewModel wasn't clearing up its flow collecting coroutines.
      They were all scoped with viewModelScope, but when StreetPreviewMode was
      added those were tide to a specific job and that seems to have changed
      the behaviour when the ViewModel is cleared. Adding an override to
      onCleared which stops the jobs fixes this.
    
     3. Remove the context that was stored in SoundscapeServiceConnection. This
      was only needed by tryToBindToServiceIfRunning and it can be passed in
      directly to that function instead.
    
     4. Removed the duplicated SoundscapeServiceConnection reference in
      HomeViewModel.
    
    With those added Leak Canary seems to run without error.

[33mcommit 09e0e70164fa8035873274011d7a34d65cebbaf5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 9 09:42:17 2024 +0100

    Add Leak Canary to debug builds
    
    Leak Canary helps spot memory leaks and so I've added it to our debug
    builds. It immediately spotted some issues I'd created due to lifetime
    mis-handling and helped me fix them.
    It's straightorward to turn off (comment out the implementation line in
    app/build.gradle.kts) but I suspect that it will be useful to catch more
    of my newbie mistakes.

[33mcommit cab85f9094119c24bcb89568e56137a818701acd[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Oct 8 11:29:34 2024 +0100

    Intersection finessing for VectorTileTest
    
    I think this is as far as I'm going to go with detecting intersections from vector tile
    data. The changes appear to successfully de-duplicate the data from transportation and
    transportation_name and the output has points at every intersection of interest that I
    looked at including entering tunnels and where steps are. However, I'm missing some
    data to allow roads that make up the intersection to be queried by Adam's code. As a
    proof of concept it's okay, though it's hard to be 100% confident in the results and
    the intersection data is in a wholly different format from what Adam's current code
    would expect.

[33mcommit 97654697ebd31312e3f02b9d27f38ff220cf5010[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Oct 9 13:50:35 2024 +0100

    Some functions to calculate the approximate (+/- 10cm) coordinates for the center of a circle based on a given segment of coordinates/linestring of a circle. (#211)
    
    * Local search using the 3x3 grid and the "name" property
    
    * Some functions to calculate the approximate (+/- 10cm) coordinates for the center of a circle based on a given segment of coordinates/linestring of a circle.

[33mcommit 5aaf29ed0218d9f77457fbc0f3758ab5904e6ab7[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Sun Oct 6 21:12:50 2024 +0100

    fix HomeBottomAppBar not displaying all button item depending on available space. improving accessibility at the same time

[33mcommit 2a22296fa4f36884970b5d003621fb9e18efc594[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Mon Oct 7 21:03:09 2024 +0100

    fix AddRouteScreen missing from HomeScreen

[33mcommit 8ab73f902f690bbaa6e6ad0611ed0fefcd7a8c35[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Mon Sep 30 22:17:55 2024 +0100

    add viewmodel for addroute and refactor validation logic

[33mcommit 413f9c13b817c2599d26d167d28713e404f39640[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Sun Sep 29 21:31:49 2024 +0100

    use better font size as accessibility settings of the device will auto scale the fonts if the user need larger font.
    global composable ui review

[33mcommit 1b230738b83af4c1d9f5e45f336a933ca80408ed[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Oct 7 16:44:00 2024 +0100

    Switch to planetiler vector tiles for test and start intersection detection
    
    The vector tiles generated by planetiler use different layer names (and include
    slightly different data) from those served up by the protomaps demo map. This
    change set switches all of the test content for the VectorTilesTest code to be
    our own generated set of tiles. The only non-default options used were:
    
      --maxzoom=15 --render_maxzoom=15 --simplify-tolerance-at-max-zoom=0
    
    We're skipping a whole lot of the layers when turning into GeoJSON and only
    handling the place, poi and transportation_name layers. What this means is
    that the intersection code is actually spotting all of the intersections between
    named roads and generating a point in the GeoJSON for them. However, there are
    still other issues:
    
     1. The transportation_name layer is only for named highways/footpaths etc. All
      of the unnamed ones are in the transportation layer (I don't yet understand
      why this distinction is made!).
     2. If a line changes from a path to a road on the map then it will be described
      as an intersection. A good example of this is Hillhead Street in Milngavie at
      -4.3158,55.9421 which turns pedestrianised.
     3. I've currently just made the intersection name a concatenation of the street
      names involved. Unique ids are not making it into the GeoJSON
    
    Otherwise I'm optimistic about this working. To address item 1 either we change
    the map generated by planetiler, or we merge the transportation and
    transportation_name layers. Named roads currently appear in BOTH layers which I
    think is 'daft' so perhaps makinf the changes within planetiler would make most
    sense as it would shrink the size of the output. We may then be able to add in
    intersections there (which would increase the size of the output) possibly
    without then having to rely on the simplify-tolerance-at-max-zoom flag.

[33mcommit 1b6c33734d79e72c90d29473bbb8b39e1ea8f497[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Oct 5 17:37:32 2024 +0100

    Test de-duplication of POI when merging GeoJSON from vector tiles
    
    testVectorToGeoJson2x2 merges the GeoJSON from 4 separate vector tiles.
    Polygons and Lines simpluy overlap, but Points are cropped to within
    each tile so that on merging there are no duplicates.

[33mcommit 3506c476312ac8aab585b7a5a3502a5ad134a9c9[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Oct 4 13:56:24 2024 +0100

    Local search using the 3x3 grid and the "name" property

[33mcommit 503365d93e8a62443d2abcf3c75aabf8374dc798[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Oct 5 16:13:48 2024 +0100

    Improve GeoJSON testing workflow and document it
    
    The changes in VectorTileTest are just so that the GeoJSON output is
    written to a file. The document describes how to display that GeoJSON to
    aid debugging.

[33mcommit 2b09a9fa9e41caeeb48f81adbf23d0af5a9b655e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 4 13:23:10 2024 +0100

    Move VectorTileTest to be Unit test and debug
    
    The vector tiles from protomap for these tests are stored as resources and
    used directly in the code. This allowed easier fixing of Polygon closure and
    multi-segment roads both of which were broken.

[33mcommit aed4d0ae0c18411e2da8bf6ff5bee095d00dba24[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Oct 4 11:10:22 2024 +0100

    Initial proof of concept for using Photon search API
    
    This commit adds a PhotonSearchProvider which can be used to retrieve
    search results from a komoot photon server. The test shows a simple
    use case for it. The retrofit API uses Moshi to parse the JSON inline
    via the MoshiConverterFactory.

[33mcommit 2934447d8b0957508f0c00375198e6a7eb21bbeb[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 3 13:56:48 2024 +0100

    Release notes markdown had minor formatting error

[33mcommit 681127230385f7a9151e8fef557bee9f89de43d7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Oct 3 13:43:11 2024 +0100

    Update release notes and start testing doc
    
    I don't know how best to write the testing doc. The release notes cover
    lots of the features already. I think a QA/test person might prefer a
    database of features and how to test them - I require some advice!

[33mcommit 2ecd692893c4f670def58fff78d68e799596be11[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.42[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Oct 3 11:18:07 2024 +0000

    Bump version to 0.0.42, version code 43

[33mcommit 72ea9aecb848b942266d4c5971a2afea471a717f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 2 15:12:29 2024 +0100

    Ensure location providers are cleaned up when Street Preview mode changes
    
    Without this change, the previous orientation provider remains running which
    results in a double callback causing audio distortion.

[33mcommit afc04b49e321cbbfc0d4aebdf8d252f0ae72690c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 2 15:11:26 2024 +0100

    Measure acutal time delta in UpdateGeometry call
    
    The time between UpdateGeometry calls varies, so use the wall time
    rather than a hard coded value.

[33mcommit 5a7b4cf879f287e83fa5e5b0f0a2525bfc2e3a61[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 2 12:55:25 2024 +0100

    Fixup POC text to speech crashes
    
    The code here is only temporary, but here are some fixes to stop it crashing.

[33mcommit b4872432c07eaf02322aadc84fcbf25a860ac454[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 2 12:03:20 2024 +0100

    Change createTextToSpeech so that it defaults to non-positioned audio
    
    If createTextToSpeech is called with just a string, the text to speech
    is played equally to both ears with no positioning in 3D space. This
    is done by making the default latitude and longitude both NaN and then
    the C++ code skips positioning the audio if that's detected.

[33mcommit 1fa010f7a8c82edc964a0708864c9aefbea272c8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Oct 2 11:27:10 2024 +0100

    Add tricky-locations.md for storing links to problem locations
    
    This is really a testing resource containing a list of locations which
    prove problematic for various reasons.

[33mcommit 7762947ec6fb0fd3eea920f00024bec84787df04[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Oct 1 11:43:37 2024 +0100

    Remove Ramani maps from app and reuse MapContainerLibre
    
    MapContainerLibre is making more sense now, so re-using it for the
    LocationDetails screen is a better idea than using Ramani maps. This
    change makes MapContainerLibre slightly more configurable so that it
    can be centered on any point. This allows it to be easily reused.
    
    The map center location and rotation are now independent from the user
    location and rotation so that on the Home screen the map can rotate
    with the user and on the LocationDetails screen the user icon rotates
    with the user and the map stays with the top at North.
    
    There's likely more to do here, but I think this is a useful first
    step.

[33mcommit b912773d8d7a0ae0f6734c5597c635a8ed330d0a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Oct 1 09:27:57 2024 +0100

    Throttle direction heading rate from AndroidDirectionProvider
    
    Although the code requests a refresh rate of 50Hz, on my phone at least
    updates are received at up to 200Hz. This change keeps the update rate
    to the audio engine where the performance is crucial, but limits updates
    to flow listeners to 50Hz and only sends the heading when it changes by
    more than 1 degree. This greatly reduces the number of redraws that the
    map UI does.

[33mcommit 81461b8da0c54a9d37b6f5192bc0b481b49ecd73[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 30 16:12:36 2024 +0100

    Disable Home screen scrolling of map from UI
    
    The map is always centered on the location provider location, so
    disallow scroll gestures from the UI.

[33mcommit bb3f6aefa05d7591207cef39808f35ef9610d496[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 30 16:09:07 2024 +0100

    Upgrade libraries - mainly for maplibre
    
    maplibre has had some bug fixes (including crash fixes) since 11.0.0,
    so update the libraries.

[33mcommit 3b9d3fdcb11f035c0063588d0c5a76c59fc7315b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 30 12:20:34 2024 +0100

    Don't return to LocationDetails screen on rotate
    
    When an intent is used to launch the app that puts it in the
    LocationDetails screen it should only navigate there when the app
    first starts. Prior to this change it was also opening that screen
    whenever the screen is rotated.
    There are two changes:
    
    1. Only parse the Intent the first time the service connection is
    bound, and not subsequent times.
    2. Reset the `Navigator` destination to "" once the LocationDetail
    screen has been displayed and then check for that after a rotate.

[33mcommit 8f7491915757abdc688667af8469f3f734d646f3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 30 10:15:38 2024 +0100

    Add icon to turn off Street Preview mode
    
    The full blown street preview mode of the iOS Soundscape allows navigating
    the remote location. Our current location provider is static and entered
    via the LocationDetails screen. This change adds an icon on the Home screen
    to show that the Street Preview mode is active and clicking on it will exit
    it.
    When the location provider changes the HomeViewModel collecting of the
    location and orientation has to be restarted with the new providers.

[33mcommit 58602c0aa91a1dbe83aec513bfdcb1cb31332ce7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Sep 29 17:23:18 2024 +0100

    Initial StreetPreview implementation
    
    StreetPreview mode is when the Android GPS location provider is replaced
    with a static latitude/longitude. With this change the mode is enabled
    when:
    
    1. A `soundscape:lat,lng` URI is used to open the app
    2. The "Enter Street Preview" button is clicked in the Location Details
     screen.

[33mcommit 6008fcd02346a44324040ae0dd6f8056d36ee8f2[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Sep 29 20:23:15 2024 +0100

    Soundscape seems to turn mini roundabouts into intersections so using the existing intersections code we should be able to pickup them up (#191)

[33mcommit fa632a35d5987d89bb84640d0dabba25ae83f778[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Sep 29 15:03:22 2024 +0100

    Fix moving navigation symbol to follow center of map
    
    The main fix here is to update the symbol showing the phone location so
    that it stays at the center of the map. This uses addOnCameraMoveListener
    to track the camera position. The rotate gesture has been disabled as the
    rotation is controlled by the compass heading.
    
    The drawable used to indicate the user location has also been changed so
    that it's an SVG image, replacing the poor resolution PNG.

[33mcommit 4dfe5b58bdb3347228e560f80b7ac1a136f6a2c3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Sep 29 14:57:52 2024 +0100

    Remove unused test code for highlighting points of interest
    
    This code was to test a feature of the vector map rendering, but it's
    not required for any current app features. Removing it to make the
    API easier to understand.

[33mcommit 4d8b1cdfce056abbaf3900f6b99af43b93b5d271[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sun Sep 29 13:33:51 2024 +0100

    Build docs on all pushes to main to capture Dokka changes

[33mcommit e948e539472fc538f2859fcf3172c9ec27e6dac7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Sep 28 13:46:38 2024 +0100

    Initial Route import and playback
    
    This is for some very initial testing and development work. If the user
    selects "Open with Soundscape" on a GPX file from the Android File app
    then the GPX will be parsed and an inMemory (temporary) database will be
    created. There's then a new RoutePlayer class which is created to playback
    the first route that can be found in that database. There's some speech
    describing when the marker changes and the map reflects the location of
    the current beacon (marker in the Route).
    
    If the app is launched in any other way, it will work as before with no
    route available or played back.

[33mcommit 9cddf70006e39deff2455180b687ccdd34cd3661[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 27 14:50:23 2024 +0100

    Proof of concept for markers (#187)

[33mcommit af924f5576de64517b57e5f1347ab6384a1f3c7d[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 27 09:35:24 2024 +0100

    Add dokka support for auto-generation of code documentation
    
    This enables dokka markdown generation and includes the output in our
    GitHub pages site. I've included the mermaid plugin which means that
    mermaid can be included in comments in the code. Dokka is generated
    from code containing Kdoc syntax comments, see
    
      https://kotlinlang.org/docs/kotlin-doc.html#kdoc-syntax

[33mcommit 956bf6d286505e318fa965bd317a2b85f062317a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 26 21:52:16 2024 +0100

    Proof of concept for roundabout detection (#185)

[33mcommit f6523c167bd1ad56d9b27ded77a4b129282c165a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 26 14:40:26 2024 +0100

    Add clearing of speech queue to stop text when leaving Hearing screen
    
    Now that we can clear the speech queue, call it in the Hearing screen
    before it exits and before it plays audio. This means that the Listen
    button can now be hit multiple times to immediately hear the speech.

[33mcommit e7d1348dfdd89b73556d3247e388424ce74d2de9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 26 13:02:01 2024 +0100

    Update actions instructions to remind about removing linebreaks
    
    Because of the way they're handled internally GitHub secrets need to all
    be on a single line - that's for both JSON and base64. Update the
    instructions on how to do this.

[33mcommit 58a8104187f41fc5057013178d34a6022062187e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 26 11:17:15 2024 +0100

    Add clearing of the speech queue to the audio engine
    
    The default behaviour of createTextToSpeech is to queue the speech and
    play it out once any currently playing speech has finished. However,
    some of the UI interactions need to be able to cancel the current
    speech and start afresh. This change adds clearTextToSpeechQueue which
    does just this.
    
    In the C++ code there's some more care taken so that only currently
    playing audio ends up in m_Beacons and all text to speech queued
    audio is in m_QueuedBeacons. This means that ClearQueue on the C++
    side just has to delete all of the PositionedAudio objects in
    m_QueuedBeacons to clear the queue. However, the TextToSpeech engine
    in Kotlin needs to take action to close the sockets for the queued
    speech or it will just block waiting for the data to be read. We
    already had code to do this on destroying the AudioEngine, so this
    is now down within the new clearTextToSpeechQueue function too.
    Forcing the ending of synthesis required some extra care with the
    UtteranceProgressListener.

[33mcommit 05510b46f6a8909fcc5c9a0a50f05770c3090870[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 26 11:10:10 2024 +0100

    Use receive socket as main debug identifier
    
    Tracking the speech data is made simpler if we have a common identifier
    on the kotlin and C++ side. This change makes that the socket passed
    over into createNativeTextToSpeech.

[33mcommit b4c1f6f9543498f05fe4486fc58cdcf281a943f6[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 26 10:39:17 2024 +0100

    Translation strings for LocationDetailsScreen.kt (#181)

[33mcommit b6400cb0aae149cb715485528c7c235b29a2ea83[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Thu Sep 26 08:34:39 2024 +0100

    create fully custom app bar to scale properly with font, and use material theme typography for app bar (#180)

[33mcommit ca8e5420e0eb96bcce55c69e6ff96b47f774428d[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Sep 25 16:47:47 2024 +0100

    auto review (renaming, cleaning stuff)

[33mcommit 6fae7f2c9e3a5aa91c1c482ee9df6ec1e4e590c6[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Sep 25 16:04:29 2024 +0100

    use navController at HomeScreen and avoid sharing it at lower level

[33mcommit a85021ee40c9fb62d76abbbf69c91f6788467b09[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Sep 25 15:31:24 2024 +0100

    handle point of interest (to be tested) + remove map from homeviewmodel

[33mcommit a5afa5ccad77ba62937bc5a9696816d7b80587ee[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Sep 25 14:54:44 2024 +0100

    handle beacon creation

[33mcommit 360b78e9083c0f35640994398db5dca42748935c[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Sep 25 13:49:55 2024 +0100

    handle marker click and long click + fix preview issue due to map libre not enable in preview (we must set null for lat and long in preview)

[33mcommit 130551a401b93457b8c9a696b4d3b1a9531066a0[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Wed Sep 25 13:20:24 2024 +0100

    wip refacto viewmodel to get preview back in home

[33mcommit dde14ca33baa9f83453d2fcc5e9e329d58f43ec7[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Sep 24 21:21:04 2024 +0100

    fix map container by properly embed it in an androidview.

[33mcommit e2691bae7877bf33be241d4b16cb0d394de84016[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Sep 24 17:45:40 2024 +0100

    refactor to have viewModel only accessed by parent

[33mcommit 34c48f5d8f6035ae4aaec1239fd19be754aa815f[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 25 20:16:02 2024 +0100

    Initial setup for detecting roundabouts. (#179)

[33mcommit f9e5d03669a13ee170599f9639376677662987d1[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 25 14:51:46 2024 +0100

    Change to make distance_to work the same as original Soundscape. Previously was using a bounding box and the middle of a feature to calculate distance_to which works well with Points but not so much with large, irregularly shaped Polygons. Now calculating the nearest edge of a feature rather than its center. (#178)

[33mcommit 3ed8870f3e0abb2d10077f133335bd1bd57c742f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 24 18:04:12 2024 +0100

    Fix mermaid parsing error
    
    Mermaid parsing appears to have changed a bit too much between the
    Visual Code version and that used by jekyll in our GitHub action.
    Removing the stray <p> and replacing the bold text ** markers with
    <b> seems to do the trick. The layout isn't nearly as good though as
    the text isn't wrapped sensibly. This is at least compiling though.

[33mcommit 8a6896413aa716bacbcb72e8a00d2483a8c4a9b8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 24 17:50:19 2024 +0100

    Enable mermaid on architecture.md

[33mcommit d1e088ff026bbcc2b0b4c9d48bb7f846dd7b5435[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 24 17:46:37 2024 +0100

    Fix mermaid diagram in architecture.md for GitHub formatting

[33mcommit 6dd3caef9195b89161966ee04285eaeb1e47abd7[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 24 17:40:10 2024 +0100

    Add start of an architecture doc

[33mcommit cc720c3ad05a4190ed90ccf008f2fe5f62024607[m
Author: DEMEY Fanny <fanny.pluvinage@gmail.com>
Date:   Tue Sep 24 13:38:34 2024 +0100

    update readme with infos about tileProviderApiKey

[33mcommit b38b2261a22b4273e4b0a9fae1d4fd41fbb528b3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 23 14:26:17 2024 +0100

    Remove unused docs from README

[33mcommit 31f74a94c674efa21ad81f03438d775beef8b780[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Sep 21 20:54:42 2024 +0100

    Fix issue with Features that have names ending in numbers getting merged with distance (#135)

[33mcommit a608ba1632d3c9e5b8ae3a8cb5e9093273c5e289[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 20 19:12:39 2024 +0100

    Around Me button PoC (#134)

[33mcommit c5ac2d1fc573b77b900b11a675011c62a673fe8b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 19 10:38:32 2024 +0100

    Settings translation strings

[33mcommit c8df228b3628de87f71e9d9890912ca429cdf3ca[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 19 09:10:27 2024 +0100

    "Nothing to callout.." strings (#133)

[33mcommit eb90db5ee9e78e8773c9d48aa0ef8e37576c1150[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 19 07:51:38 2024 +0100

    Format code and remove/update old strings from PoC (#132)

[33mcommit eacda9924714880662d48b899c14f1e48a5a1b8d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 19 06:42:50 2024 +0100

    Translation strings for "Intersection with $roadname $relativedirection" (#131)

[33mcommit 011cd94b8098b1ede739fc4f3e9c25c7ee4c6b45[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 18 16:30:23 2024 +0100

    Ahead strings (#130)

[33mcommit 8f70ca39c4cbb079acc0e0647181662423391f1b[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 18 13:16:18 2024 +0100

    Relative direction labels "Ahead", "Ahead Right", etc. and their translation strings. (#129)

[33mcommit 4ef26f5981462af1d44ec63aae2c2693a455878a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Sep 17 21:27:02 2024 +0100

    Translation strings (#128)

[33mcommit f4b189c839ea92393284aa87a86fd9e948b7243c[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.41[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Tue Sep 17 17:29:06 2024 +0000

    Bump version to 0.0.41, version code 42

[33mcommit 41d048239df208d2926cf24fc2f5caaf5ce90846[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 17 14:32:15 2024 +0100

    Add button to create beacon on LocationDetails page
    
    Creates an audio beacon at the location described.

[33mcommit 95ca891108e4b5d32c5716ef63eb64df5f86815f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 17 13:33:32 2024 +0100

    Improve RamaniMaps marker
    
    One of the current downsides with RamaniMaps is that it doesn't use
    vector drawables. This change imports as a red PNG the marker image
    that we want.

[33mcommit ac3bfd00a5bca1dda36d5075eb79050d0071bec3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 13 09:54:26 2024 +0100

    Improve handling of intents
    
    This change centralises the handling of intents. geo, soundscape
    and 'shared google URL' intents all now open the LocationDetails
    screen. The handling of the intent is asynchronous and in some
    cases relies on network being available. More finessing is likely
    required to make the behaviour as robust as possible.
    
    To support the sharing of locations from Google Maps the Soundscape
    app now accepts plain/text data from other apps. Clicking "share"
    on a location in Google Maps will pass a https://maps.app.goo.gl/...
    URL in plain text into Soundscape.
    The code here uses URL to get the real (unshortened) URL
    https://www.google.com/maps/place/... It passes that into the
    Android Geocoder which returns a fixed up address including
    longitude and latitude.

[33mcommit 83640eafcad7bb011ca0443dc822f8654a146caa[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 16 13:05:56 2024 +0100

    Start implementing LocationDetails screen
    
    This screen is widely used in the iOS app. It can be used to show the
    details of a searched location, the current location or a saved
    marker. It has a map and a text description which varies depending
    on what type of location it is. The map opens at a fixed zoom around
    a marker for the location. It also should show the crrent location if
    that is within the bounds of the map.
    
    This is a very rough screen which uses Ramani Maps which is a composable
    wrapper for MapLibre. The string used to navigate to the LocationDetails
    screen is appended with JSON containing the data required by the screen.
    That JSON is parsed when the LocationDetails navigation takes place.

[33mcommit 96e2b0899ca98f0430c59a51539ae298b7cb7591[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 16 13:04:22 2024 +0100

    Refactor MarkersAndRoutesAppBar for reuse
    
    This is taking wholesale the contents of MarkersAndRoutesAppBar and putting
    it in CustomAppBar for reuse by other screens.

[33mcommit ceccc482e7e33b884dc3415482576c755ccee4e4[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Sep 16 20:37:05 2024 +0100

    Translation strings (#126)

[33mcommit 06e26548206e3bc90f041772aa404746777bb124[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Sep 15 20:04:15 2024 +0100

    Translation strings (#124)

[33mcommit 6e199476a7f3a23ec9df2973cea9e0df54e8a86c[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Sep 14 21:00:08 2024 +0100

    Translation strings (#123)

[33mcommit cd8771d7514f2f85b9292981843ee70f1762bfbc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Sep 14 14:56:40 2024 +0100

    Add Markers and Routes screens to ScreenShot test

[33mcommit 1025cd75f1f1249e425fc34235e03580f0886b58[m
Author: thow76 <thow1976@hotmail.co.uk>
Date:   Sat Sep 14 09:33:31 2024 +0100

    ui_and_navigation_for_markers_and_routes_screen_with_tabs_and_add_route_screen_implemented

[33mcommit 81830a12bad0462cd2cd80de28e54f5a10e8874d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 13 17:13:25 2024 +0100

    Translation strings for Settings (#120)

[33mcommit 7125c283b2564b05a4f2ae694977b6e651bab5d3[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 13 12:03:56 2024 +0100

    Added 3x3 grid rather than using a single tile (#119)

[33mcommit 6548698ffc5a4323fb5a1cc80697bf2eb6f12a39[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 13 09:56:25 2024 +0100

    Relative directions for the roads that make up the intersection (#117)

[33mcommit 49263f6b4b8c7647ba6e716a788c4b54469fd797[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 13 08:02:04 2024 +0100

    Detect the road names that make up the intersection in the field of view (#116)

[33mcommit 3c564a7e5b2d8a70649fcdce7d40f896c2c563ba[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 13 07:15:38 2024 +0100

    Added intersection detection to the Ahead Of Me button. The device must be within 30 meters of the intersection and the intersection within the Field Of View. It will tell you how far away the intersection is. Still to do names of roads that make up intersection and relative direction of those roads (#115)

[33mcommit 730a03d87fcdcd6b4c85ba7a9575fe5a546c1cd1[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 16:08:44 2024 +0100

    Add beacon type selection to Settings screen
    
    Just a drop down with the beacons, no audio (yet). When the soundscape
    service is initialized it uses the value from the settings. Also save
    the beacon type to the SharedPreferences when set from the onboarding
    screen.

[33mcommit c258af9daa0e5d80917859be0f910e6a7c5d77a6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 15:09:55 2024 +0100

    Add Settings screen to ScreenshotTest
    
    Add Preview for Settings screen and add it to the ScreenshotTest.

[33mcommit 6fd2b6868ea44ed4dc180e8b9b34890bbe0fc8bb[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 12 18:38:52 2024 +0100

    Fix bug for "My Location" when in Cameroon (#113)

[33mcommit a96a2ca4c68b7127ab45708e5902ee4840dcd0bd[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 12 15:38:08 2024 +0100

    Added get3x3TileGrid function and unit test for different latitudes (#112)

[33mcommit 190f2268e91a4e1d133976dddd6fb40e5b062524[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 14:51:55 2024 +0100

    Add setting for ignoring unnamed roads in callouts
    
    This setting exists on iOS where it claims to be only for Street Preview,
    but it actually applies everywhere.

[33mcommit 500f8ab641d6b2a0dd686d344cc8db6d3cacf3a0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 14:35:24 2024 +0100

    Move FirstLaunch flag to be a SharedPreference
    
    The flag to indicate the first launch of the app was being stored with
    DataStoreManager, whereas all of the other settings were using
    SharedPreferences. This change moves the FirstLaunchFlag into
    SharedPreferences just to simplify matters.

[33mcommit 8855b289fcaa01b19c4301ff17bbbf71360d1e7e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 14:20:40 2024 +0100

    Add constants for preference keys and default values
    
    We also dump the values that are in SharedPreferences from the start
    of MainActivity. The identical code also work from within the
    Soundscape service, so the preferences should be universally available.

[33mcommit fb0839de031664860dc406e53f96cffd6184416f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 14:03:11 2024 +0100

    Move to using androidx.preference.PreferenceManager
    
    A client can now use code like this to get values set by the settings:
    
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
      val allowCallouts = sharedPreferences.getBoolean("allow_callouts", true)
    
    Until the settings page has a value changed, the getBoolean call will
    return the default value (true) in this case. The default value has to
    be the same as in the Settings Composable code in order not to cause
    confusion.

[33mcommit 3e9c0ef9649ff236db9680d511dd9e812020992e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 12 11:46:16 2024 +0100

    Add settings screen and associated navigation for home screens
    
    Many of the changes here are renaming Screens code to be OnboardingScreens.
    There's then the addition of HomeScreens as we now have Home as well as
    Settings. A navigation controller was then added so that settings can be
    navigated to, and some initial settings values added. These values end
    up in SharedPreferences where they can be accessed by other code.

[33mcommit abf4831722c369e81f1c57c0a12e699672b2926d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 11 18:31:16 2024 +0100

    Proof of concept for "Ahead Of Me" button. Only detects roads with a name. (#109)

[33mcommit fc316fb70769ced4fa9eca7c76c3172d5cead0bf[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 11 16:07:10 2024 +0100

    Skeleton code for "Ahead Of Me" button (#108)

[33mcommit 39bfee6dbd0aee2e7717ad0de51db317e5f41d3c[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 11 14:30:09 2024 +0100

    Remove duplicate Features using "osm_ids" from 3x3 grid and individual tile data for unit tests. (#107)

[33mcommit c949b3f901324fb1bdf7ce4871bfdabcc5f27709[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Sep 8 20:42:16 2024 +0100

    Added a 3x3 grid rather than a single tile. Not finished yet as need to check for duplicate features. (#106)

[33mcommit 6b43fad880dc1a399908f00e04d0af68e0cf52e8[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sun Sep 8 11:59:33 2024 +0100

    Remove double service start
    
    A stray checkAndRequestNotificationPermissions was left in that resulted
    in the Soundscape service being started twice. That didn't have too much
    impact except when running teleport where it resulted in the orientation
    listener being registered twice. Removing the call resolves the issue.

[33mcommit b29014320381ad1c2e7bf660ee4a094040f2e27c[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.40[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Sat Sep 7 18:13:54 2024 +0000

    Bump version to 0.0.40, version code 41

[33mcommit da0cc89409bc7c72524ea99cc408c3c8929d33e6[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Sep 7 18:07:59 2024 +0100

    Rate app second attempt with correct activity passed in
    
    This looks like it might work, still needs tested via Play Store though.

[33mcommit 8835331ac751900c940c9582e363f13944ff5a20[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Sep 7 18:17:37 2024 +0100

    POC for "Around Me" button. Lots more to do (#103)

[33mcommit 4c8bbb58c50a88338e7509be3e056447c886741e[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 6 19:17:22 2024 +0100

    Update release notes for 0.0.39

[33mcommit 2fba03f19e53180008bcd655d93adb6c7889a076[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.39[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Fri Sep 6 17:10:56 2024 +0000

    Bump version to 0.0.39, version code 40

[33mcommit 5f76d2bd009b17f344df68ed41c9fb19c3488c55[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 6 17:29:33 2024 +0100

    First attempt at in app review
    
    This seems fairly standard stuff, but it's not possible to easily
    test without building a release and putting it on the internal
    test track.

[33mcommit 645b6505595f935f5702039e3ab5d8f81dbe781a[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 6 16:17:56 2024 +0100

    Replace button that stops service and exits with sleep toggle
    
    The iOS app had a 'sleep' button in the top right and this change
    moves towards that model. Clicking on the button toggles the running
    of the foreground service. When the service isn't running, there's no
    location/heading update and no text to speech. The icon showing the
    current location on the map is removed, and then when the service is
    restarted it reappears and the map is recentered.
    
    Not sure what iOS does in this case, need to compare.

[33mcommit 73352b8b51da4e7ac8078fe4818142fa2e2ebf94[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 6 14:17:28 2024 +0100

    Fix initial audio beacon buffer direction
    
    The first buffer played for an audio beacon was always the 'straight
    ahead' one. This change ensures that on creation the beacon uses the
    current listener location to select the correct initial audio
    direction. This is easy to demonstrate in testing - simply long click
    on the map in different directions from the listener and immediately
    hear the possible sounds that the current beacon type can make.

[33mcommit c5e3471fa98095a0758dde6e95337b3029a0bbee[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 6 09:30:17 2024 +0100

    Upgrade libraries including maplibre
    
    Upgrading MapLibre removed all of the outdated MapBox naming so now
    it's all MapLibre which makes more sense.
    Almost all of the warnings in libs.versions.toml were resolved which
    included:
    
     * Upgrading libraries
     * Removing duplicate declarations
     * Removing declarations of two different versions of a library (okHttp)
    
    The only remaining out of date library is Realm. If we bump that to 2.x
    then it doesn't compile (known issues it seems). Otherwise, everything
    seems to be working the same as before.

[33mcommit ed24e55efa15f5130d8258298037335ed912dafe[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Sep 6 05:51:14 2024 +0100

    Forgot to add translation string for missing tile scenario. (#97)

[33mcommit 3d753e09e34313135ce555d37303661c04bfa7b5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Sep 6 05:08:32 2024 +0100

    Fix Text to Speech at API 30 (#96)
    
    This simply required an addition to the manifest of the
    INTENT_ACTION_TTS_SERVICE.

[33mcommit 8d2ff077a4c58945e26279aba7cd6ec215cc2e07[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 5 20:34:14 2024 +0100

    Fixes for monkey driven crashes
    
    Reset any beacon handles to zero after destroying them.
    Add permissions for BROADCAST_CLOSE_SYSTEM_DIALOGS. This allows the
    app to restart with an intent even if the notification dialog is open.
    This is important for when running monkey on API 30.

[33mcommit 797a475f2a7ce44ad0df17ffef9b9059cb9537bd[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 5 18:39:26 2024 +0100

    Add Kalman filter to smooth location (#94)
    
    This is loosely based on the Soundscape iOS version. The test isn't very
    scientific and could probably use tightening up.

[33mcommit 052bb73b61d7f3d5209ec1a606cb8addf4edd226[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 5 18:23:50 2024 +0100

    Fix translation strings for null location (#93)

[33mcommit 644acb302a21a649d126e09943eed23350e6c5b8[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 5 15:07:25 2024 +0100

    Call updateGeometry on AudioEngine even when we've no location yet
    
    The aim is to call updateGeometry on every orientation callback.This
    change means that this will always happen, even if location setting
    has been disable.

[33mcommit e30525e428bf0eaec32bbfc6474d9f8ebcc4c8c9[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Thu Sep 5 14:51:23 2024 +0100

    Initialize Text to Speech engine with the correct language from the start
    
    The code was always setting English and then allowing it to be changed.
    Now it gets the application locale and uses that to initialize the TTS.
    That should always be correct.

[33mcommit ed1a3c2817b47a0d1eb63978d0b261ed4811ea15[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 5 12:56:47 2024 +0100

    Initial setup for whatsAroundMe() (#91)

[33mcommit c2803551d6bc962c78a33fd45ba805a212cd472a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 5 11:20:07 2024 +0100

    Google translate for missing "Notifications" strings (#90)

[33mcommit 0b3d042b7877b772cc8fa1954fa86ba2c628df55[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 5 10:30:56 2024 +0100

    Show/Hide Notifications row for version >= 33 (#89)

[33mcommit a01bb7a4b7ade112228cc47827aa0ff35c113093[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Sep 5 09:18:23 2024 +0100

    Tidy myLocation() and translation strings (#88)
    
    * Add multiple strings to TTS example
    
    * Comment out multiple Log statements in myLocation()
    
    * Remove old code for translation strings
    
    * Comment out Log.d

[33mcommit 4fd6d0030b4c6e7e1816f95fd2fc907b473b77f1[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 4 21:46:03 2024 +0100

    Fix myLocation() to use translation strings with TTS (#87)

[33mcommit f4af6caa5ff6930ef2c2e1deab144038a2628b0c[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Sep 4 17:59:34 2024 +0100

    First pass at catching some failure scenarios for myLocation() (#86)

[33mcommit 2deda2b43275cc0a3ea47e43a29f9aba596f04ad[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Sep 3 12:59:58 2024 +0100

    Mapping documentation
    
    This is mostly to document what I've been learned about vector tiles
    and how to create them.

[33mcommit b6d9adea5bec9f3feda9f9ab6d98717aa9ce7cce[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 2 15:38:19 2024 +0100

    Initial attempt at generating parsed GeoJSON from VectorTile protobuf

[33mcommit 0abf273e717d3afb4bdccd2fb66a93759cdfacc0[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Sep 2 10:50:02 2024 +0100

    Initial vector_tile protobuf compilation

[33mcommit 66ae61b86cbbf8d1e330787b70a8179631befeed[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 31 16:19:08 2024 +0100

    Make geo: intent create an audio beacon, and soundscape: a mock location
    
    Also, when the Share button in the drawer is pressed (for now), share
    the current location as a soundscape URI. This means that we can easily
    share locations and open soundscape at that mock location.

[33mcommit 9be532a67442d5ebdad53041a7589d42f608957a[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 31 14:47:50 2024 +0100

    Fixup My Location to be robust to null returns
    
    Having !! asserts if the value is null, so it should really only be
    used if we think that it could never actually happen.

[33mcommit 2b5a15990cb74b86016c1991aa1d7434e6c7eaff[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Aug 30 20:07:27 2024 +0100

    Translation strings for "My Location" (#84)

[33mcommit b4a5f44a3094d9dcc49ceecaa4a2d6a57fcc7d79[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Aug 30 16:41:09 2024 +0100

    bug fix for myLocation() (#83)

[33mcommit 56dc9b62dcbecf16635ff2e5cd1ad9d8276ffcbd[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Aug 30 15:55:47 2024 +0100

    Fix bug for "My Location" (#82)

[33mcommit 4a735a4d7c3b73074f5b79b4d9ac3e514bff1471[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 30 13:53:20 2024 +0100

    Revert "Upgrade libraries including maplibre"
    
    This reverts commit a6a2aa5c8f47f9a23849e959d914d0cc4b66623e. It
    was causing crashes in API 30 and API 31 during startup and speech.

[33mcommit 786a4e24e4da66d1755fcc89cb658ce5b51713d3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 30 13:10:26 2024 +0100

    Add geo intent handling which teleports user to location
    
    The MainActivity now accepts geo: URIs and the location is passed into
    the SoundscapeService. When the service gets passed a location it
    replaces the regular location provider with a StaticLocationProvider
    at the location requested. This will update the provider even if the
    service is currently running.

[33mcommit 99c7d057f101214297aa6021fa15a8959c66b7c2[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 30 11:12:23 2024 +0100

    Move location and orientation out into separate class
    
    This is the start of adding alternative location and orientation providers.
    The location and orientation providers have been moved into their own class
    and the option of creating a static location provider has been added. That
    teleports the user to a fixed location but allows the orientation still to
    be controlled.

[33mcommit 79b5d359596c1b87bfb5d3b1d81b0750d6d17742[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Aug 29 20:11:53 2024 +0100

    My Location button with TTS giving direction and nearest road (#78)

[33mcommit 0cfaac215babcb257ce9e4f74336bf8ee7f68488[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Aug 29 15:32:27 2024 +0100

    CompassDirectionLabels and translation strings for "My Location" button (#77)

[33mcommit b834abfbab8e1c84b456aaf06ae751f6cde782df[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Aug 29 13:51:46 2024 +0100

    Setup "My Location" button (#75)
    
    * Setup the "What's Around Me" button to use TTS and to discuss how to setup up the ViewModel(s)
    
    * Setup the "My Location" button to use TTS via the SoundscapeService

[33mcommit 31dcb208b79779d7fe8a4f024d883e0f19d4a245[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 29 10:55:57 2024 +0100

    Restore water on map UI to blue
    
    I've left the code in that turned the water layer purple for future
    reference, but commented it out.

[33mcommit a6a2aa5c8f47f9a23849e959d914d0cc4b66623e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 29 11:19:27 2024 +0100

    Upgrade libraries including maplibre
    
    Upgrading MapLibre removed all of the outdated MapBox naming so now
    it's all MapLibre which makes more sense.
    All of the warnings in libs.versions.toml were resolved which included:
    
     * Upgrading libraries
     * Removing duplicate declarations
     * Removing declarations of two different versions of a library (okHttp)
    
    No issues seen during testing.

[33mcommit 0a165419850b9519d361af47fe0a1c08d4c7ea5f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 29 10:49:17 2024 +0100

    Add SDK API version matrix to nightly build

[33mcommit d78bd701a9bc66688bdfba7cf4760319dd3f6e1b[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.38[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Aug 29 08:25:30 2024 +0000

    Bump version to 0.0.38, version code 39

[33mcommit 66df27935da83e6c7b5bae2c9ad19bff3394e0c5[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 29 09:06:36 2024 +0100

    Fix splash screen for API 30
    
    Having dropped the SDK level down the splash screen resulted in a
    crash on API 30 because it didn't inherit from Theme.AppCompat.

[33mcommit 7e9e0c7f5fa77daabdbdc76e3aa607ffca55b2cc[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Aug 28 21:11:47 2024 +0100

    Update to getTileGrid() so it runs every thirty seconds (#71)

[33mcommit 4b3322e76025fa6f9c39ce52c6f554ee71e881eb[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Aug 28 18:34:48 2024 +0100

    Notify the user with a Toast every hour that the SoundscapeService.kt is running in the case where they dismissed the app but left the service running - battery life and all that. (#70)

[33mcommit 1b6a120bdd4ceed72f6222fc6572714202d9d6f6[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Aug 28 11:58:17 2024 +0100

    Add release notes for Play Store release

[33mcommit 1e6228c867adc2ba147bdf94dd7f1d2bc226e675[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.37[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Tue Aug 27 13:09:05 2024 +0000

    Bump version to 0.0.37, version code 38

[33mcommit 5e5715cb6594713e0b07c1009d64c4dec8ffc798[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 27 13:47:28 2024 +0100

    Fix AudioEngine in SoundscapeService
    
    Although the code worked whenever it was run from Android Studio, the
    AudioEngine Singleton didn't get initialized when ran by clicking on the
    icon to run. This change stops it being a singleton and simply initializes
    it from with the service instead.

[33mcommit 1076f559b6cf585c973eeb09c4d4d0112fee2989[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Aug 26 10:26:16 2024 +0100

    Initial maplibre integration for vector maps
    
    There are two bug advantages to using vector maps:
    
      1. Server costs. By switching to using protomaps the costs are a
         fraction of those serving up pre-rendered PNG tiles. See
         https://docs.protomaps.com/deploy/cost for analysis.
    
      2. Dynamic maps. The maps can be 'styled' on the fly meaning that
         we could highlight points-of-interest or hide unimportant data
         depending on the context that the user is operating in.
    
    maplibre is a branch from mapbox at the point where they changed their
    licensing. It's probably the most complete and widely used open source
    map rendering solution.
    The CPU usage appears to be similar to using pre-rendered maps, though
    we should keep an eye on this.

[33mcommit fc05b227d1cd44ec6bfcd2c560c9bb5338ae1414[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Sun Aug 25 18:22:57 2024 +0000

    Bump version to 0.0.36, version code 37

[33mcommit c7e05117cc1362348774df5549fbae9d61fdaf9e[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 20 15:47:11 2024 +0100

    Initial integration of MapCompose map
    
    This code is the initial integration of the MapCompose open street map
    UI. The soundscape service connection has been moved out into a separate
    class and then a MapCompose based UI has been added to the Home screen.
    The service now uses lastLocation to give a much quicker 'lock' on the
    current position.
    
    The local map is displayed with a marker displaying the current location.
    The marker is moved whenever the Soundscape service updates the device
    location. The map can be zoomed in/out and repositioned independently of
    the marker location.
    The map centers on the current location when the location is first updated.
    The heading is constantly updated so that the location icon rotates to point
    in the direcion of the phone.
    A long press on the map will create an audio beacon at that location. A long
    press on the marker that appears will destroy that beacon, or a long press
    elsewhere will move the beacon to the new location.
    There's obviously a lot more UI that can be done, this is just the very start.
    
    An HTTP cache has been initiated - this likely affects all HTTP and not just
    the Tiles for open street map. As a result it may be required to mark Tile
    HTTP requests with a cache-control header if we don't want them to go through
    the cache?
    
    The API key for thunderforest is stored in local.properties as
    tileProviderApiKey so must be configured for each developer i.e.
    
    tileProviderApiKey=xxXXxxXXxxXXxxXXxxXXxxXXxxXXxxXX
    
    with an appropriate key. For builds, there's a new GitHub secret
    TILE_PROVIDER_API_KEY that needs setup.

[33mcommit 17e5829f75dcaac1f5e77244aeb662513499eb62[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Aug 24 17:11:19 2024 +0100

    translation strings for Home.kt (#67)

[33mcommit 8bd6e1b1d1d339fd53e1ef55899438ee6bcf8751[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Aug 24 13:09:54 2024 +0100

    translations for Home.kt (#65)

[33mcommit 4de5f9d4ff17384427fd485b11accf3a259069bc[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 24 10:04:49 2024 +0100

    Move onboarding screen ViewModels into their own files (#64)
    
    Android Studio does this with a few clicks via Refactor (F6). Moved
    them next to the PermissionsViewModel.

[33mcommit f3e5acd73dc93c2369d29065274e1bcc2cb5a378[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Aug 24 08:41:28 2024 +0100

    Change onboarding sequence to request permissions earlier. (#63)

[33mcommit bab30901e5ab70564856e1937a8c2384ad340e43[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Aug 23 21:10:41 2024 +0100

    Moved permissions requests to Navigate.kt (#62)

[33mcommit f5c4e50859aa994cbbc67abd858ff79056e20fb5[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri Aug 23 18:26:42 2024 +0100

    Add Activity Recognition permission request and tidy (#61)

[33mcommit 4d079de6f2a4aa9ddc82fa8d6e83eac46e926945[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu Aug 22 20:49:35 2024 +0100

    Translation strings for Terms.kt (#60)

[33mcommit a1fae9e2dde3584bab3569096103277de8ffa7da[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Aug 21 20:29:01 2024 +0100

    Some of the translation strings for Terms.kt (#59)

[33mcommit 84b2ad2e7615a5d4b693e10ba2384fc703bf4dfe[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 20 20:36:27 2024 +0100

    Created a rubbish app icon. (#58)

[33mcommit d96b39f48aba1af2330eead19cd302df287afe56[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 20 19:57:00 2024 +0100

    Fix beacconList instrumentation test failure

[33mcommit 7c02465580ad33985ba12741acf36f1b4eb7f221[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.35[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Tue Aug 20 18:36:29 2024 +0000

    Bump version to 0.0.35, version code 36

[33mcommit 2d7482c4d8d7ea3eee45f36d0caf8715ec5a0b02[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 20 19:26:45 2024 +0100

    Update beacon names from Classic/New to Original/Current
    
    This is to match the translations strings. There are two places, in
    the AudioEngine C++ code and in the mock data for the Preview.

[33mcommit 4186bf31c1b86d8749a4a19e93ecd8f7b75e918b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 20 19:01:42 2024 +0100

    Reduce minSdk version and increase targetSdk version
    
    minSdk can drop to 29 because it turns out that the splash screen
    is supported on that release too, just not as well. The splash.xml
    has to be versioned due to the less good support.
    AndroidStudio has been complaining about the targetSdk not being the
    latest, so bump that up at the same time. In doing so move the
    dependencies to using libs.versions.toml which is the current way to
    handle package versions.

[33mcommit d66899e69d91a3652ce488298eb2d46e7c89abcb[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 20 19:12:32 2024 +0100

    Menu items translation strings for Home.kt (#54)

[33mcommit c8fcbf3a50b5e2e65cf2b07b5f29fc54c8ce8d31[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 20 17:08:56 2024 +0100

    Make the UI work a bit better on smaller screens and landscape
    
    Two main changes here:
    
    1. Any screen with a large image at the top in portrait now has two
       columns when in landscape. The image appears in the left hand column.
    
    2. Vertical scrolling enabled on parent Column/Row so that any off the
       screen UI can be accessed.
    
    Various padding changes - but I'm less confident in those.

[33mcommit 744ea73acfddbc479aa1871ed0d894e4f953cc5c[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 20 16:59:45 2024 +0100

    Finish.kt translation strings (#52)

[33mcommit 2fc687a30dd43f683711614a6f90a734a2d88842[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 20 10:31:43 2024 +0100

    Permissions logic jiggle (#51)

[33mcommit 6ee869a073c46f9c090ae950eec5b5cdf3016352[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Aug 19 20:50:13 2024 +0100

    Translation strings for AudioBeacons.kt (#50)

[33mcommit 27c27183f235615e63ea76625b285acec1787d97[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Aug 19 12:40:44 2024 +0100

    remove HTTPLoggingInterceptor (#49)

[33mcommit 206bccae40c152ab4d161553e0b77c97e6aa01c9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Aug 19 11:55:57 2024 +0100

    Use injected AudioEngine in Soundscape service (#48)
    
    We now have a Singleton AudioEngine that is managed by the dependency
    injection code. It is created if onboarding screens are run, and this
    change ensures that we use the same AudioEngine when the Soundscape
    service is run.

[33mcommit c0563ef6ddf2a6a9505301b1ad19b08f9e30262c[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.34[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Mon Aug 19 09:47:21 2024 +0000

    Bump version to 0.0.34, version code 35

[33mcommit becf557841903b7d2474de20973947249307dc55[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Aug 19 10:16:57 2024 +0100

    Remove screenshots from docs but build them everywhere else
    
    Putting the screenshots in the docs increased the overhead of docs
    compilation by a factor of 10 and the result wasn't that readable.
    This change removes that and instead we build screenshots during
    all of the other actions. Release and nightly builds both upload
    them as artifacts which should make them easily accessible for
    developeres.

[33mcommit df96608fcb9385ff591aeca839e0017eafd83560[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Mon Aug 19 09:24:50 2024 +0100

    Deliver onboarding screens' ViewModel data as flows
    
    Firebase was reporting a test issue:
    
    Your app uses 1 non-SDK interfaces, which are incompatible with Android P+.
       Ljava/lang/invoke/MethodHandles$Lookup;-><init>(Ljava/lang/Class;I)V
    
    It turns out that initializing ViewModel data inside an init function is a
    bad idea - see
      https://www.droidcon.com/2024/03/22/mastering-android-viewmodels-essential-dos-and-donts-part-1-%f0%9f%9b%a0%ef%b8%8f/
    
    This change removes the init from AudioBeaconsViewModel and instead uses a
    flow to initialize the list of beacon types. That initialization will now
    happen when the UI code calls into the ViewModel with
    collectAsStateWithLifecycle instead of when theAudioBeaconsViewModel was
    created.
    
    The LanguageViewModel has been changed in the same way. The list of supported
    languages is now in a flow and is only initialized once.

[33mcommit 4c04a8694d5ad9a6061fbe4c9b39905270625d1a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 18 18:50:39 2024 +0100

    Clear up null pointer exception from Soundscape service and put default values in getTilesForRegion() (#45)

[33mcommit 6e452917c12dd6ce6b4ed438b2e66508812b0b18[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 17 16:45:43 2024 +0100

    Try including screenshots in documentation compilation
    
    We could include the report from the preview screenshot validation test,
    but that requires validating against reference images which we don't have
    yet. Instead, we build the reference images and use some very basic HTML
    to display all of the screenshots for each Test. The full resolution test
    can be saved from the link and so this should give us automatic screenshots
    of each screen in all locale and a couple of screen sizes.

[33mcommit d29ab0f912a48281a6aee3b42b2b539768d3f979[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 17 10:19:17 2024 +0100

    Update to screenshot test approach
    
    This change adds a number of different configurations for which the
    Preview is run:
    
     * Locales - all of the languages that we support
     * Devices - tablet, small phone and medium phone
     * Fonts - smaller and larger
    
    Currently this results in 119 files. The names change each time the
    update builds is run. It doesn't make sense to pollute our main
    git branch with these, so I've added them to a .gitignore.
    For github actions, the approach I'll likely take is:
    
     * First new action which runs the update screenshot build and then stores
       the resulting screenshots in a reference artifact for the repo
     * Second action runs the screenshot validation against the retrieved
       reference artifact. Ideall it would then publish the resuling
       org.scottishtecharmy.soundscape.PreviewTest.html to the GitHub pages
       site.
     * Third action runs the update screenshot build but discards the results
    
    The last of these is the most useful in that it would hopfully be useful
    for checking whenever Previews get broken which otherwise go unnoticed in
    the build. The first action could be run manually to get a quick overview of
    the current state of the UI. The second might be more useful once we've got
    a more stable application.
    
    The main problem with not storing the reference images in the repo is that
    rewinding to an older commit won't rewind the reference and so building on a
    branch would get confusing. Needs some more thinking about.

[33mcommit 3d947d65da85c5bf65a029c1546f18371f0cb61f[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 16 17:14:59 2024 +0100

    Add screenshot test support to our project
    
    I just followed the instructions here:
    https://developer.android.com/studio/preview/compose-screenshot-testing
    
    Once I've added this, I'll add a screenshot valid test to some of the
    github actions. Once that's done, anything that changes any of the UI
    will have to update the reference image.

[33mcommit 0b1a32896734c56ee12cc46bc364611262698598[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 16 09:41:52 2024 +0100

    Initial audio support for onboarding screens
    
    This commit adds ViewModels for the Hearing, Language and AudioBeacons Screens.
    It adds them within the same files as the UIs because they are small and it's
    easier to see what's going on. The NativeAudioEngine is now a singleton and
    is automatically created on first use.
    We want @Preview to carry on working on all of the streams, so for those the
    ViewModels are left uninitiated and mock data is supplied by the @Preview
    functions. This seems a reasonable way of fixing it up, though I'm less clear
    if it's the most correct way.

[33mcommit 68c2257de14a803e7bbed23efe36f55295703eb9[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 15 16:25:47 2024 +0100

    Creation of OnboardingActivity
    
    There are a number of changes in this commit, and I'm really just
    submitting it for discussion. I'm not wholly clear that these are
    the right approaches!
    
    I've moved the onboarding screens into their own OnboardingActivity.
    The initial reason for this was because I didn't like the behaviour
    of the back action in the app. During onboarding, it moved back
    through the screensm but once at the Home screen it would still move
    back to the Finish screen. By moving the onboarding screens into their
    own Activity I found that it was easier to discard the action history
    that got the user to that point. Now the behaviour is that going back
    from the home page exits the app - which is the same whether the
    onboarding was run on that session or not. Activities seem to be "old
    fashioned" and so perhaps this isn't the right approach, but it does
    keep all of that code in it's own little world which seems neater.
    
    The other change I made within the Screens was to pass in an onNavigate
    callback rather than a NavHostController. That seems slighly neater and
    was recommended in the Android docs as it keeps the navigation stuff
    out of the UI. The next changes will involve ViewModel injection, and I
    was just trying to match the code to the docs as much as possible.
    
    This commit also includes the permissions messing around. However,
    because the permissions are currently in MainActivity, none of those are
    currently requested in the onboarding screens. Once we have some
    ViewModels in place, then the permissions can be moved to one of those
    and the requests should happen at the right point in time.

[33mcommit a2283d5e250a79835e52a6fc9241c6916d191775[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 15 10:26:16 2024 +0100

    No need to checkout source for firebase testing

[33mcommit 51e836de8743f7a13dc3a3046df802fa2ac8f39c[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 15 09:41:50 2024 +0100

    Add nightly build action
    
    The nightly build only runs if there's been a new commit within the
    last 24 hours. It builds a debug build and runs various tests on it
    including Firebase.

[33mcommit 608eab874dde39476d7651e3491e4de7b15ac0f4[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Aug 14 20:32:53 2024 +0100

    Fixed button so it behaves like other screens (#40)

[33mcommit b99f874d61ff016ae5adadc2e9c91d4ccbbd74e1[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Aug 14 16:06:30 2024 +0100

    Various AudioEngine tests and improvements
    
    AudioEngine.setBeaconType now takes a String which is the name of the
    Beacon type to use. The test loops to test every Beacon type that
    AudioEngine reports, rotating the listener so that the sound for each
    of the orientation angles is played. This is done fairly quickly so
    as not to make the test over long.
    
    Mentions of com_scottishtecharmy_soundscape were replaced with the correct
    org_scottishtecharmy_soundscape.
    
    Added awaitTextToSpeechInitialization which blocks all AudioEngine speech
    related APIs until the TextToSpeech engine has fully initialized. Without
    this there's a high risk of speech being dropped.
    
    Added APIs to get the available voices and languages.

[33mcommit cf5a63a5d359b8a8cf40ecbed5174d55228b8f1d[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.33[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Tue Aug 13 15:24:21 2024 +0000

    Bump version to 0.0.33, version code 34

[33mcommit 0be88cf9860a8de7c84a07a6cb1e78bff2bc6889[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 13 15:53:21 2024 +0100

    Add getListOfBeaconTypes to AudioEngine and instrumentation test for it
    
    getListOfBeaconTypes returns a list of the names of the available beacons within the
    AudioEngine. This is to allow the UI to interrogate the AudioEngine rather than having
    two separate lists of beacon types. In addition, I didn't understand enough about
    Instrumentation tests when I wrote the AudioEngine code. Now that I do, I've added a
    belated set of initial tests.

[33mcommit 9f864970f86280b6c4ca839f2d9a239af0d7bfd4[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 13 13:12:02 2024 +0100

    Fix some accessibility issues flagged by Firebase (#37)
    
    * This item's height is 41dp. Consider making the height of this touch target 48dp or larger
    
    * Consider increasing this item's text foreground to background contrast ratio.

[33mcommit fe613ab96e17d1605823dd1639951ca73f5aeb5f[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.32[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Tue Aug 13 09:43:23 2024 +0000

    Bump version to 0.0.32, version code 33

[33mcommit ad4fc02a8d624b441c19aeeeda852a3ce16c6c63[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 13 10:08:14 2024 +0100

    Fix GpxTest tests to all run standalone without dependency on each other
    
    The GpxTest tests were reliant on the order in which they were run. The
    parsing tests were populating the inMemory database and then the database
    tests were testing that. However, when Firebase runs the test the database
    from previous tests doesn't exist and so the tests were failing. This
    change makes each of the tests standalone without dependency on other tests.

[33mcommit 49e9e458c211701a3454c31e6e1c4b82dc0afaed[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.31[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Tue Aug 13 08:47:52 2024 +0000

    Bump version to 0.0.31, version code 32

[33mcommit 0863549175999ab200df88fdf56a1fbf6113980d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 13 09:26:34 2024 +0100

    Fix "This is not implemented yet." Toasts (#35)

[33mcommit fa1f85ad52311f1ca7df226b4c7b94653fd145b3[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Aug 13 08:42:30 2024 +0100

    Fix CheckBox behaviour (#34)

[33mcommit ddc497b74f3ad5c3a91dec4739b9924c8f068241[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Mon Aug 12 11:59:22 2024 +0100

    Remove RouteData from RealConfiguration
    
    Adding RouteData was causing the database opening to fail - possibly
    because there was already a database file and the schema differed. Once
    we start using RouteData come up with a migration plan or use a separate
    database.

[33mcommit 4914388819557058896b3b4d72b080ee6770a4c8[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Aug 12 14:10:12 2024 +0100

    Hook AudioBeacons.kt into onboarding sequence (#32)

[33mcommit e9c137c3cda62623fb219548aa160fb039699d70[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.30[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Mon Aug 12 09:29:37 2024 +0000

    Bump version to 0.0.30, version code 31

[33mcommit 6d036b89e979e96624d9f379ad7fa2879b022f84[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Aug 12 10:18:50 2024 +0100

    Initial Home.kt and all the other bits it needs...lots more to do for this (#31)

[33mcommit 9d01083d6269a67f4613df7db18d36f15bc45c0a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 11 16:19:42 2024 +0100

    Finish onboarding screen (#30)

[33mcommit 70c9c79d5bab2e8586ae31c5dae7cb3383280451[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 11 14:38:34 2024 +0100

    Terms of use (#29)

[33mcommit f964ac6b12a0752573629c20854b117538f89acc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 11 12:15:16 2024 +0100

    Add NavGraph to get rid of build error

[33mcommit ef024dd01581badbc30ece5b7224f4c316409326[m
Merge: 5e2c965f a93a33c9
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 11 11:43:46 2024 +0100

    Merge remote-tracking branch 'origin/main'

[33mcommit 5e2c965fff25e3374c644f8dc80d550d21a6a0c0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 11 11:43:18 2024 +0100

    AudioBeacons.kt and Navigating.kt

[33mcommit a93a33c903340701ba42bccbbcb864dca6210ffa[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 10 12:36:11 2024 +0100

    Extend GpxTest to have multiple routes in database
    
    Rather than test with one route at a time, we now import all of the GPX
    into the database and then test each one separately.

[33mcommit 3b0814583f31f2c007f00a5db1f3c3398f440ff7[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Sat Aug 10 11:35:13 2024 +0100

    Extend Route database for geospatial support
    
    This commit alters the RoutePoint class so that it can be used with the
    Realm geospatial functions. Whilst doing this it was noticed that the
    lifetime of waypoints wasn't being correctly handled. Deleting a
    RouteData in realm won't delete the child RoutePoints and so we do that
    by hand. After deleting a RouteData any RoutePoint with empty backlinks
    to routes are also deleted.

[33mcommit 1a19f2f0966acfe5436d1d7d4ff24c2b0116d6c5[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.29[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Aug 8 15:25:50 2024 +0000

    Bump version to 0.0.29, version code 30

[33mcommit 1d178dec1ea74f6b7f7eb06292c835754df1ffbd[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 8 16:02:38 2024 +0100

    Initial GPX route code including database
    
    This is the initial code to allow the importing of GPX files and storing
    them in a Realm database. The Instrumentation test in GpxTest.kt tests
    the API on GPX files from 3 different sources.

[33mcommit 4eeffac65cd297fdbc6cf15251ce5a7b48330f42[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Tue Aug 6 12:15:42 2024 +0100

    Upload logcat on test failure (#26)
    
    Failing tests had little info to help debug the error. This change means
    that the logcat for failed tests is uploaded as an artifact.

[33mcommit 5a4eac718dda73800cd758116d8d33b0b9cc5018[m[33m ([m[1;31morigin/crashylitics[m[33m)[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 4 10:38:59 2024 +0100

    Changed row height to follow Firebase accessibility recommendation "This item's height is 41dp. Consider making the height of this touch target 48dp or larger" (#25)

[33mcommit 3338df953d031841f31c268e5eea2a16c3514628[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Aug 4 08:19:41 2024 +0100

    Fix lint warnings in TileUtils unit tests (#24)

[33mcommit 390a17e1d7914bd2685274fab91afe62956132eb[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Aug 3 22:01:36 2024 +0100

    Language screen test (#23)
    
    * LanguageScreen.kt and LanguageSelectionBox.kt tests
    
    * LanguageScreen.kt and LanguageSelectionBox.kt tests
    
    * LanguageScreen.kt test
    
    * LanguageScreen.kt test with sleep added
    
    * LanguageScreen.kt test with sleep added and only one assertIsDisplayed()
    
    * LanguageScreen.kt test with only one assertIsDisplayed()
    
    * LanguageScreen.kt test with two assertIsDisplayed()
    
    * LanguageScreen.kt test with three assertIsDisplayed()
    
    * LanguageScreen.kt test with three assertIsDisplayed() and Thread.sleep(5000)
    
    * LanguageScreen.kt test with two assertIsDisplayed() and ui_continue assertIsDisplayed commented out

[33mcommit 85e10fdae331affac384ed19ae79376447e8f113[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 2 17:51:44 2024 +0100

    Enable Firebase crashylitics (#21)
    
    This requires a valid google-services.json. This is provided as a GitHub
    secret for release builds, but is a mock one for all other builds.

[33mcommit cb52e24bbd4b516c7bf0029f71edf47f307137d8[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Aug 2 17:50:52 2024 +0100

    Tidy up lint warnings and land some old work (#20)
    
    * Ensure FMOD_RESULT is defined to aid code inspection
    
    fmod_codec.h was using FMOD_RESULT without a definition and that was
    leading to various lint warnings. Adding the include of fmod_common.h
    resolves this.
    
    * Clear up TextToSpeech progress listener onError warning
    
    UtteranceProgressListener::OnError is deprecated, but we still need
    to define it for now. Adding the @Deprecated message stops this from
    being a lint warning.
    
    * Load FMOD at earliest point
    
    We want the FMOD library to be loaded before the app starts calling
    initialization functions on it. Loading it with the application should
    do this.
    
    * Tidy up application behaviour
    
    Two changes here:
    
    1. Clicking to stop the foreground service now exits the application.
    2. The MainActivity launchMode is now singleTask.
    
    Combined, these two gives a slightly more obvious behaviour when
    starting the application and running it via intents.
    
    * Extend notification to open app
    
    Clicking on the notification now opens the app making it easier to stop
    the audio.

[33mcommit 0fd57edd35eb26fd0f425bfbd4d8f5a6651bd7b6[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.28[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Aug 1 15:16:10 2024 +0000

    Bump version to 0.0.28, version code 29

[33mcommit 8880f8ca13a1ea82310605ed68d8135bb0cff315[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 1 16:15:40 2024 +0100

    Fix firebase robo API version
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit a8a4d07b6852606253852e52d5681a199b98ec6c[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.27[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Aug 1 14:38:27 2024 +0000

    Bump version to 0.0.27, version code 28

[33mcommit 7159d937a6ed1e99dbc3fa08b7296aca088229da[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Aug 1 15:37:55 2024 +0100

    Add Firebase robo test to release build action
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit 9752837f53652fab1efcff92bf05dcc92a940221[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Wed Jul 31 11:18:14 2024 +0100

    Add docs on GitHub actions
    
    Some initial documentation describing the GitHub actions and the various
    secrets that they use.

[33mcommit 71ea2e9aec27f93c32726c18e306da6ad804a2f0[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Wed Jul 31 09:28:30 2024 +0000

    Bump version to 0.0.26, version code 27

[33mcommit 4cb34a2b09fab106320889f140f0c47d7c80734b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jul 31 10:27:18 2024 +0100

    Add admin PAT to checkout
    
    The repository doesn't allow regular users to commit code without a pull request. This change adds a token which allows the pull request to be bypassed. See https://github.com/stefanzweifel/git-auto-commit-action?tab=readme-ov-file#push-to-protected-branches for details.
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit 16c4a7d10232823712458ed599e31be3bc9437c8[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Wed Jul 31 09:48:35 2024 +0100

    Add mock google-services.json for use in actions
    
    When running the run-test action to test a pull request, the user
    may not have repo write access. This means that they won't have
    access to the GitHub secrets. This mock google-services.json ensures
    that the build will still work. It's over-written by the 'secret'
    file when building a release build.

[33mcommit 1513d48b2ce780edcbe0836c094a4f764ae71f67[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Tue Jul 30 14:16:32 2024 +0100

    Add Google Firebase support to release action

[33mcommit e7ad19a1943d232630dae8e8a46248f80795a40d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 30 15:27:44 2024 +0100

    Simple onboarding screen tests and fix mistake in Welcome.kt (#15)

[33mcommit 12d6ac4c4ff43872222d7732f86a32c15bfb8b1a[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Tue Jul 30 12:16:29 2024 +0100

    Improve github actions
    
    The gradlew file is now stored in git as executable so that no
    permission messing is required.
    The run-tests action is extended to run the instrumentation tests
    in an emulator.
    setup-gradle is now used to improve gradle caching.

[33mcommit 0b6a279f9631c9dd4d469e266cb75ef2c035f360[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 30 11:41:56 2024 +0100

    Simple first UI test (#13)

[33mcommit b033b1f41d3cc2230f2b93d59c46a9d9aebc6b02[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 29 08:50:47 2024 +0100

    Fix lint warnings as far as possible (#12)

[33mcommit cf19dc30da9b1e19bf06a7cf7bd3d967d078176d[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 28 17:34:51 2024 +0100

    Fix lint warnings in SoundscapeService.kt (#11)

[33mcommit 1f1eca6dac637d47daa8b0ff988e48b34a17c9c6[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 28 15:50:31 2024 +0100

    Fix lint warnings for TileUtils.kt (#10)

[33mcommit 6a0d2b8df9d5f37830a5c5610efcded3d28426a4[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 28 14:21:52 2024 +0100

    Fix moshi adapter lint warnings for LineString and LngLatAlt (#9)

[33mcommit 305abafaa78f376fb4d997e3151e226dd1a516d2[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 28 12:21:11 2024 +0100

    "Fix" unchecked cast warnings. (#8)

[33mcommit 4355940b96201c6d9546e9deb73c7858e4c0a845[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 28 09:42:18 2024 +0100

    Tidying up some of the lint warnings (#7)

[33mcommit 633b40075b38a7b98c9a42bee00cff0c25434ada[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 28 08:15:59 2024 +0100

    Update "app_name" to Soundscape from SoundscapeAlpha (#6)

[33mcommit c5057665d366344f8da7a199a0a976964a2ba0dc[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Jul 26 11:31:33 2024 +0100

    Add project status to README

[33mcommit 32f134e1a079e4820ea010d3ff019bcb39de45a9[m
Author: davecraig <davecraig@unbalancedaudio.com>
Date:   Fri Jul 26 10:56:52 2024 +0100

    Testing branch on main repo

[33mcommit 7e3880c0a800f04b81aa35d2705f44804758172b[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jul 26 09:45:38 2024 +0100

    Update README.md
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit da0f58e6f7f0330b2ffe983f1e6b74f0bde424b3[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Fri Jul 26 09:42:46 2024 +0100

    Create jekyll-gh-pages.yml
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit 8b7d64367afc5e2a99c766107e80a55ed7523d42[m
Author: apps <apps@unbalancedaudio.com>
Date:   Fri Jul 26 09:39:32 2024 +0100

    Workflow finessing

[33mcommit 060473a7bfe9e3ad84fd3a9b9ebf8331dea1b6b6[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Thu Jul 25 13:58:22 2024 +0100

    Add lint checks to build/test workflow

[33mcommit f1ff821723baca2ec036dfdc89061156370f7e68[m[33m ([m[1;33mtag: [m[1;33msoundscape-0.0.25[m[33m)[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Jul 25 12:50:52 2024 +0000

    Bump version to 0.0.25, version code 26

[33mcommit 3ae0bbc908ef037a311bd5446b6a13f37caf2415[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Thu Jul 25 13:48:49 2024 +0100

    Fix lint errors in strings
    
    Some new strings do not have translations. I think it's likely that some
    of these will not be used long term, so mark them as not translatable
    and with a TODO. This avoids the lint errors.

[33mcommit cf1d2a61f71e74279830a9c91461e1762b6a7511[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Jul 25 12:37:08 2024 +0000

    Bump version to 0.0.24, version code 25

[33mcommit 15b275a8d4563f0622f44cae58ee5524a0f040b8[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Thu Jul 25 13:36:15 2024 +0100

    Bump minSdk up to 31 so as to include splash screen

[33mcommit a05c1533dcc7daf0d4993d24308e634b772c974b[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Jul 25 12:24:07 2024 +0000

    Bump version to 0.0.23, version code 24

[33mcommit 7ddbd13edd40482b3b3d2ef7de588d249b543ef6[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Jul 25 11:58:28 2024 +0000

    Bump version to 0.0.22, version code 23

[33mcommit f970337dfba3ac85f4c9a32329a28b5af863c84f[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Jul 25 11:43:44 2024 +0000

    Bump version to 0.0.21, version code 22

[33mcommit 1072c9226ed4c01261d385824cfec4960c0620c3[m
Author: davecraig <davecraig@users.noreply.github.com>
Date:   Thu Jul 25 11:28:23 2024 +0000

    Bump version to 0.0.20, version code 21

[33mcommit 8b80b677cdf473705857a355fe02cf48238e3cc6[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Thu Jul 25 12:19:15 2024 +0100

    Add github action to build software
    
    The signing of release bundles requires various github secrets.

[33mcommit 6b93fb4522ce56f54ddcb83724d9527b25f01768[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Thu Jul 25 09:52:38 2024 +0100

    Add license file
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit d1515cdfb6ceb89d055d43d415aab6a4f927834c[m
Merge: ea83cebb 87c0d7ac
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jul 24 20:21:20 2024 +0100

    Merge pull request #1 from Scottish-Tech-Army/davecraig-patch-1
    
    Update README.md

[33mcommit 87c0d7ac4555f94813c5068c14c30377d33513db[m
Author: Dave Craig <davecraig@unbalancedaudio.com>
Date:   Wed Jul 24 20:20:21 2024 +0100

    Update README.md
    
    Signed-off-by: Dave Craig <davecraig@unbalancedaudio.com>

[33mcommit ea83cebbf5b47773e19b96bc3dc4e7b978a1a227[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Wed Jul 24 17:06:43 2024 +0100

    Squashed audio engine changes
    
    Audio engine updates are driven from Kotlin by the orientation callback.
    That's the main event that affcects the 3D audio and so it seems like a
    sensible point in time to update the audio positioning.
    
    Calling createBeacon in Kotlin creates an audio beacon of the type set
    by setBeaconType at the location provided.
    createTextToSpeech outputs the speech provided from the text string at
    the location provided.

[33mcommit 18a23b231b4c8d3e80f4df0c2b2db66600cff91a[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Tue May 28 10:44:05 2024 +0100

    Initial fmod engine commit v2.02
    
    This commit contains the fmod engine headers and libraries from
    https://www.fmod.com/download#fmodengine for v2.02 of FMOD. It also has
    the LICENSE file and a version number text file.
    
    These are only the files that we need - headers, .so and .jar files.
    Examples, authoring tools and libraries which we don't use have
    been excluded.

[33mcommit 4e3a188944a223dd3c3a5660e192367415be8f3b[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Wed Jul 24 16:04:26 2024 +0100

    Rename com.kersnazzle.soundscapealpha as org.scottishtecharmy.soundscape

[33mcommit 6c4a5b1d7c1afacca8aeca5fab075080e21fd148[m
Author: davec <davecraig@unbalancedaudio.com>
Date:   Wed Jul 24 15:35:11 2024 +0100

    Move documentation to docs/ directory
    
    This change is to support github pages including mermaid.

[33mcommit 92d1fb8d2ef6a6d604587ba434539d2885c4ec3b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 21 20:13:25 2024 +0100

    Required permissions list

[33mcommit 2f4e532a2639acd6ef21cb40ed74b518abc0f96e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 21 19:16:53 2024 +0100

    Splashscreen

[33mcommit 53d4f20d52e127102ac43ab11844fb4825a17e8e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 21 12:23:17 2024 +0100

    Onboarding screens, navigation, translations, icons, DI, blah!

[33mcommit a80ee5704e90e2d9a12fc40755a2846fc4841b8e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 9 18:39:26 2024 +0100

    Start and Stop functions for ActivityTransition tracking

[33mcommit 335eb2306d9aed2c5b4b6b07a49dca868f16a7f9[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 9 18:08:09 2024 +0100

    Rename LocationService to SoundscapeService and all the other gubbins

[33mcommit 6353735a0a0c1cad430e6470bce94be2c1897c1f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 9 11:38:57 2024 +0100

    ActivityTransitionReceiver class to listen for the activity transition broadcast from the device

[33mcommit d62b94da03ab21d4c9139ae47659f714965d6b51[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 9 11:11:16 2024 +0100

    ActivityTransition class to detect if the device is in a vehicle or not

[33mcommit 88f6149e8d69ead6b5c25700b61032f67921e236[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 8 19:43:15 2024 +0100

    Activity Recognition API permissions

[33mcommit c8b210169e66938c4c03533a820778f0030791a5[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 8 18:41:35 2024 +0100

    Activity Recognition API permissions

[33mcommit 21c2d2dbb6bc2f91a5fdc90511468c741bb7b0a3[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 8 18:18:05 2024 +0100

    Activity Recognition API permission

[33mcommit 5a884ffca3738f1fe727eb752cfc9a493bd91663[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 7 21:31:34 2024 +0100

    delete old prototype function

[33mcommit 7b71afc4d72473da2bac38f0b994cebec5384ac0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 7 20:40:57 2024 +0100

    getTileGrid function to get a 3 x 3 grid of tiles and insert/update into db

[33mcommit 1dee1b6a58f7e42c3c7beb33f6a19a9475dc688a[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jul 7 08:48:34 2024 +0100

    added TTL_REFRESH_SECONDS const for tile check

[33mcommit abb2a01e142953ec83ab6eca0f953b0b94659a79[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Jul 6 21:25:09 2024 +0100

    checking lastUpdated timestamp in RealmDB

[33mcommit 46163bc3667a5c8bbede6b24df68bcad37a9f42c[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat Jul 6 20:50:13 2024 +0100

    checking lastUpdated timestamp in RealmDB

[33mcommit a44ac4839c8f11dda8520efd2ca616bd284e1d32[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 19:33:53 2024 +0100

    clean up imports

[33mcommit 0b15ced171a80206d373913fe5966c2849c4a35e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 19:30:06 2024 +0100

    add doc string to processTileString

[33mcommit 00c4e5116bb9c2d192915f8bf3702a6e93c02adc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 19:24:47 2024 +0100

    move processTileString into TileUtils.kt

[33mcommit 9d967d8d47e53556266671d5da5861160d05f209[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 19:02:43 2024 +0100

    update processTileString

[33mcommit a20eb51cfbc2b3a6b4448e6d7f4e6a6223e64bef[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 18:56:27 2024 +0100

    update RealmDB schema and TilesDao to contain bus_stops and crossings

[33mcommit 0d50817944cdc5cd85adc3e58264df6efca7744c[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 18:51:30 2024 +0100

    added getCrossingsFeatureCollectionFromTileFeatureCollectionTest

[33mcommit eeab4a3cb301d0d1bc019d2bd19ba35522120b0a[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 18:46:25 2024 +0100

    added getBusStopsFeatureCollectionFromTileFeatureCollectionTest

[33mcommit a24e66ab0cc1916d5d021c8b59d932f219343a28[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 18:31:43 2024 +0100

    added getBusStopsFeatureCollectionFromTileFeatureCollection and getCrossingsFromTileFeatureCollection

[33mcommit e3e946084a394e60b584172fb4a87632f9ef7edf[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 17:28:15 2024 +0100

    processTileString function

[33mcommit de8a0ef3f51ecc36d83c4b2e8a96669b856fc298[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 15:55:29 2024 +0100

    change service icon

[33mcommit 9341bc3f814b8243d92448c060c92110ea4f316b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 15:03:38 2024 +0100

    process tile string and insert into RealmDB

[33mcommit a103edb1d5de0020ad40fbde0f54fabc6ef3ea7a[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 14:59:50 2024 +0100

    process tile string and insert into RealmDB

[33mcommit a19a83e7fe57bf4045a384313ada45dbd4044b81[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed Jul 3 10:41:34 2024 +0100

    RealmDB get and insert

[33mcommit 6a390bb36a07580f168e4bf5828e9ccc883a3d9e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jul 2 12:16:09 2024 +0100

    get rid of waffle

[33mcommit fca0cceedcf1e3a99b6815186e68956765c053bc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 21:55:17 2024 +0100

    add getTile function to Dao and Repository

[33mcommit 8c7868d9c19d287cfa59dabf54fa3cdc038de91b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 21:50:16 2024 +0100

    add getTile function to Dao and Repository

[33mcommit a4c770d1e38783d7def181a053d4ede48a7edffb[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 21:20:53 2024 +0100

    change created property to lastUpdated

[33mcommit 6f327122c757ba18d3e1f6f7aa896253c51c764f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 21:07:15 2024 +0100

    deleteRealm function to clean up mess while testing

[33mcommit cc7757ee5c0ce380eb538cf2eadbb746927fc090[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 20:08:48 2024 +0100

    RealmDB query for existence of current tile in db

[33mcommit f8fddb27613306da8c46f39fc6d99ac08579f455[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 20:04:10 2024 +0100

    RealmDB query for existence of current tile in db

[33mcommit 7d30779c037f5f7e1659519c36be6da6d681247d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 19:17:23 2024 +0100

    RealmDB object created onStart

[33mcommit ad4681225095fa40a63d7da50a16163579338eaf[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 16:11:18 2024 +0100

    RealmDB configuration

[33mcommit 9930a94244ca040298c7f327abc1522d0db0f8a4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 08:23:04 2024 +0100

    update insertTile

[33mcommit de9ee1cc39103f684a8f665f9bc7bcc51fe4afe7[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:56:17 2024 +0100

    update coroutines to 1.8.0

[33mcommit 1b4f3aa98131e39fd3b054a3cc0989323daca006[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:54:08 2024 +0100

    prototype Realm DB - needs a lot more fiddling

[33mcommit 13921a88590e2d402452a02ffdcd12ec7eb866da[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:52:30 2024 +0100

    prototype Realm DB - needs a lot more fiddling

[33mcommit 3daf4516f483a3a252107b8a1d175fc6b43dae5b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:48:14 2024 +0100

    prototype Realm DB - needs a lot more fiddling

[33mcommit 0ebfc59c943bebdc4097ab762b98a3a4b6deddfd[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:44:37 2024 +0100

    prototype Realm DB - needs a lot more fiddling

[33mcommit fb680c1a4583b561d7cee9947d782dfabf792e30[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:41:20 2024 +0100

    prototype Realm DB - needs a lot more fiddling

[33mcommit 9875ed92a993be315ad53a966fda7b1cfcd7c3cc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jul 1 07:08:45 2024 +0100

    Remove old Retrofit test functions and use okhttpClient and caching

[33mcommit 44304bd9a516980bc89c2dc269677240af55654d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 30 20:12:54 2024 +0100

    Network and Okhttp

[33mcommit 67788c7fc5f4b07ec46a9cdbb1be31310da7c0e8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 25 11:03:09 2024 +0100

    Network and Okhttp

[33mcommit d611e8c788cae521c695f5d2460402616e667c2d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jun 17 05:00:17 2024 +0100

    Network and Okhttp

[33mcommit 68a409b7d1d97ed0d3686ecd4c5f96f691e99172[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jun 17 03:55:11 2024 +0100

    Network and Retrofit

[33mcommit f03c313493e499c0138042327196d6f5f35f51f1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 16 12:21:09 2024 +0100

    Network and Retrofit

[33mcommit ee262007d150fa85da9c319ec08f54e5fdb4bc07[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jun 10 14:23:56 2024 +0100

    Foreground service - Fused Orientation Provider test output orientation to screen

[33mcommit b5cec7217ebfdd0619b3073ebd85253ed7ac7e52[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon Jun 10 08:53:18 2024 +0100

    Foreground service - Fused Orientation Provider test

[33mcommit de5b76bf96c0df79d07a8c4802e9fc85eb72cb5e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 9 10:43:43 2024 +0100

    Foreground service

[33mcommit e0df02e23ae493028d4356e967fa385eff25b207[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 9 10:42:43 2024 +0100

    Foreground service

[33mcommit d361eed17f5aa7c08c295c5d880072d57f1fd25a[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 9 10:27:40 2024 +0100

    Foreground service

[33mcommit ddc74eed084b553fa12917fd45b9ffce3122f414[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 9 08:45:10 2024 +0100

    Foreground service

[33mcommit e6f7f2cdd5d5373bb41d5d8cb2cb723872b173a2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun Jun 9 08:29:06 2024 +0100

    Foreground service

[33mcommit 7949b797d7af19f7d1d5959e8598330c20c39e03[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 4 21:55:33 2024 +0100

    Foreground service launches and notifies user but not much else

[33mcommit ff83193a6a1f5bb783cd986fed1aec75c3686374[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 4 19:28:09 2024 +0100

    letting the user know we are running a foreground service

[33mcommit db6ea03c5121c2a3f9601d550dbcf76f98b91bd8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 4 18:58:06 2024 +0100

    override onStartCommand to check this is creating the worlds most useless service

[33mcommit 6bdfe3a59ac0d1e84f4184710ff73bb7e149417f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 4 18:27:06 2024 +0100

    Setup Binder so clients can access the service

[33mcommit 32df05a60c085ffc1686ff772a1e54f144694aa3[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 4 18:20:49 2024 +0100

    Setup Binder so clients can access the service

[33mcommit a7e3996638b479cff67735a59b48d2245403ae20[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue Jun 4 18:13:44 2024 +0100

    Setup LocationService as a Foreground service

[33mcommit 7f0658c106cd1d2f51c88ac22c8e872fb34e822b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 30 21:13:38 2024 +0100

    lineStringIsCircular function

[33mcommit e4005c69f572a72996170ffc366d021c7d3ccaac[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 29 20:55:00 2024 +0100

    intersection type LoopBack and duplicate "osm_ids" in the intersection data

[33mcommit a116437855c343003df4dc7b71d0369d2f286b71[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 29 20:18:06 2024 +0100

    setup for intersection type LoopBack

[33mcommit ba82cf07f848f34bb78bc1a51d28748ee7d05705[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 28 18:01:45 2024 +0100

    setup for intersection type LoopBack

[33mcommit f0e6e0b7a1751c843df51e2a9ff0e940af655668[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 28 09:43:48 2024 +0100

    setup for intersection type LoopBack

[33mcommit 1ff02710e50888c7285bb381c2c3d84e971ae6bb[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 28 08:53:36 2024 +0100

    delete unused imports

[33mcommit 17e50ce5fe205ac59928dc2bd22e94acad45cc85[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 28 08:18:10 2024 +0100

    sort intersection road names by direction getIntersectionRoadNamesRelativeDirections

[33mcommit 7ff718fb8e98be261e88dc7075d108c6c386e60d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 28 05:32:38 2024 +0100

    bugfix splitRoadByIntersection

[33mcommit 79ad180831e26fa01dfb9455509a06d549939ca8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 19:28:43 2024 +0100

    bugfix IntersectionsTest

[33mcommit 1e41d0a10227fe1b978485945d05ae1197561426[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 15:23:25 2024 +0100

    intersectionsSideRoadLeft test

[33mcommit 76f22c98ce390678c304b54c82c5367496e6b97d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 14:14:33 2024 +0100

    intersectionsSideRoadRight test

[33mcommit 322ac744b07eecbf04270452c12d10e12e5442b1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 13:06:59 2024 +0100

    intersectionsLeftTurn test

[33mcommit c3a12e4e8ce7582aa23dbf8bf49faf950eab95e4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 12:43:20 2024 +0100

    intersectionsRightTurn test

[33mcommit 9d66e1fae35f04ec8fb973bb452f8062a69e8084[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 12:35:04 2024 +0100

    intersectionsStraightAheadType test

[33mcommit e05dc8278dad71eeb7e51135741cc7a1aa577703[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 27 12:14:43 2024 +0100

    tidy up IntersectionsTest and move functions to TileUtils.kt

[33mcommit 4e0847202c3b02d9615d9f33238098b52e5d4172[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 17:25:36 2024 +0100

    tidy up

[33mcommit 4f6f9c9c8694d8fa8b0cc0ae5167cf7a8ce44ee8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 17:23:34 2024 +0100

    tidy up

[33mcommit f99cf9fecde379546c9b14f1d92f8a20034dfb39[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 16:33:40 2024 +0100

    update all the tests

[33mcommit 25714c1f2404a106f1b827c8ce651f0ee749d380[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 14:03:29 2024 +0100

    update intersectionsSideRoadLeft

[33mcommit 345bca084a7158bdf472c10003aae3e6e1aa47dc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 13:56:22 2024 +0100

    update intersectionsT1Test

[33mcommit 8180e683de8b0084dfe84a1f8083bcc57b0a1838[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 13:52:19 2024 +0100

    update intersectionsT2Test

[33mcommit 5224d308d30ce6184c4da88fee388588e1588062[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 13:48:27 2024 +0100

    update intersectionsCross1Test

[33mcommit 07cf1313c279f524fef0a0ff613fe98a0947b291[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 13:44:42 2024 +0100

    update intersectionCross2Test

[33mcommit 987ce277bec171960bc58296e89f82263261722d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 13:39:45 2024 +0100

    move getRoadBearingToIntersection function into TileUtils

[33mcommit 9d99bf54b8327c13c645143f9e571eb3483696f2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 13:34:10 2024 +0100

    simple intersections Crossroads type 3

[33mcommit 8e8ec7f62e7564787f217bc1c2fae959a5959146[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 11:57:29 2024 +0100

    simple intersections Crossroads type 3

[33mcommit d799bdbfa97bcb95c2deb50a60e50b247d1d0b20[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 26 08:16:45 2024 +0100

    Setup simple intersections Crossroads type 3

[33mcommit 8351efc92692e0486ce35e1c035dda8ac19ff19d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 12:37:32 2024 +0100

    TODO

[33mcommit aeb821e79b4cb867ed582a6b87379941786d1206[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 12:09:59 2024 +0100

    images

[33mcommit 61ae2df16956ae6c8cc23d2b3e91e774f0930f35[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 12:08:43 2024 +0100

    Update README.md

[33mcommit 8b2a322882b95f4abb25ffa8aa80198f5a3dcfc5[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 12:03:01 2024 +0100

    Update README.md

[33mcommit ef044ecafb27cee240c36e33cd41fe925a0bdb8e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 11:59:37 2024 +0100

    update intersection urls

[33mcommit afeb0b1294824d7dd4770f821e3e7995b5d64f62[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 10:32:09 2024 +0100

    Crossroads type 2 test

[33mcommit c58adc50b3ba0c3ffb653ca8ba43c4bbf83a1449[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 10:31:20 2024 +0100

    Crossroads type 2 test

[33mcommit e38dacbbe0701c6f83c7561ffdcfa1b34f2975b4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 10:23:46 2024 +0100

    Crossroads type 2 test

[33mcommit cb879e8c1269114c21270edf0d788421ea95f740[m
Merge: d4eb1ff6 1e9a91a3
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 09:58:29 2024 +0100

    Merge branch 'main' of https://github.com/AsquaredWsquared/SoundscapeAlpha

[33mcommit d4eb1ff6bf9a1889aa5e35596e89924bf58736d2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 09:58:10 2024 +0100

    Crossroads type 2 setup

[33mcommit 1e9a91a39f8a143c621af417aef0a312c7bf0f74[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 09:21:26 2024 +0100

    Update README.md

[33mcommit 1387949b6ab5e2265b37c38eea5f1f8094477051[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 09:16:37 2024 +0100

    Update README.md

[33mcommit 41c7295f8997419ee14b7079233df138524d0ed1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 09:03:55 2024 +0100

    Crossroads type 1

[33mcommit b955f3a2966798c37c0b1b5b12b2380617971173[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 07:45:03 2024 +0100

    Crossroads type 1

[33mcommit 27a13242565520d5c1d4e914b496de8cd1adaba8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 07:26:32 2024 +0100

    Crossroads type 1 data setup

[33mcommit 9fca44c4c0125aaa265f892ea7839757f78d6b73[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 25 07:02:41 2024 +0100

    T2 intersection update

[33mcommit 17889ed1ca3b5534f496f8b646ebb0c9b278f1c5[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 24 15:27:44 2024 +0100

    T2 intersection update

[33mcommit 66b7ff8844465c65e509a880832a507113340ae8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 24 15:22:41 2024 +0100

    T2 intersection test

[33mcommit d67907ea63813c60320872f0a12a49b2b0b0b653[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 24 14:51:37 2024 +0100

    Setup for T2 intersection test

[33mcommit 18eecb0ece67bff1d756fef474b321f0bc2d930c[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 24 13:07:03 2024 +0100

    sad face

[33mcommit 3f6f99295182406457ec28b972cae0fb2e805ef4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 24 12:59:37 2024 +0100

    splitting LineString by intersection coordinate

[33mcommit 2c900f1cf25e4ea54e66f5563a4507bf00b7d25e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 24 09:57:57 2024 +0100

    splitting LineString by intersection coordinate

[33mcommit 1627752cd501ae547a2b21d80fbee5064605977f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 20:55:40 2024 +0100

    bus_stops and crossings appearing as roads in FOV. Temporarily exclude from getRoadsFeatureCollectionFromTileFeatureCollection function

[33mcommit 835178e811d933535f817e2041c8c301ddd4e311[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 20:32:13 2024 +0100

    bus_stops and crossings appearing as roads in FOV. Temporarily exclude from getRoadsFeatureCollectionFromTileFeatureCollection function

[33mcommit 0b0e4e62026564f058aae3acde6db609ffa8c4d0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 19:40:10 2024 +0100

    update doc string

[33mcommit 18eb6ce1b17d6282064f74848e397dc5ffb7d0c2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 19:25:59 2024 +0100

    cursed IntersectionsT1 test!

[33mcommit e2b02b8b7cfe02e9ee33e65177f5163fc6c7b3c7[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 15:02:28 2024 +0100

    directionAtIntersection function

[33mcommit 4f8b2db53c2a191b35010b05995a7370ea481c02[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 14:59:07 2024 +0100

    directionAtIntersection function

[33mcommit 41d08de9764d68a47c956d940777595782a9fc01[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 13:03:35 2024 +0100

    IntersectionsTest Side Road Left

[33mcommit 6abe66e93573b43c6864edb5c1d74860d98cc199[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 12:54:01 2024 +0100

    IntersectionsTest Side Road Right

[33mcommit 7748da76b722725e6994740ea88a722d3ca4bdc5[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 12:50:49 2024 +0100

    IntersectionsTest Side Road Right

[33mcommit 234469a03dfcc9fda43a63c689aeae03c714a6cc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 11:09:42 2024 +0100

    IntersectionsTest types - simple

[33mcommit ec644f9f9a5d0cb9db936e379c130989c84a4da0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 11:08:16 2024 +0100

    IntersectionsTest types - simple

[33mcommit 402306c31085c5d91e97ab08807e645acebe7cd4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 10:49:25 2024 +0100

    IntersectionsTest left turn

[33mcommit 843f23c5dc37707c2bb29dff8269ac7176179edb[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 10:38:18 2024 +0100

    IntersectionsTest right turn and test data

[33mcommit 349953c8cf017d7433c8f2a913e2e7bba7c8cf64[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 10:30:22 2024 +0100

    IntersectionsTest right turn and test data

[33mcommit acd69f5a3c400c3a8b270eff002d9557dd6c3c2e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:47:18 2024 +0100

    IntersectionsTest file

[33mcommit d89ef9ba2e8b461bece2eae3ff6170dd2b15d4af[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:46:02 2024 +0100

    Update README.md

[33mcommit be530b0a9c0eb6e82921fdee8a1898db725b8938[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:33:08 2024 +0100

    Update README.md

[33mcommit f2318885e5856e1016ad9ed6810004f797ed9ef0[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:30:48 2024 +0100

    Update README.md

[33mcommit 766bd7e4e5531186e4697e66cfda2e88d3af407c[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:10:43 2024 +0100

    Update README.md

[33mcommit 9c94939c48ad7c22facd592531d01eb320f1ed1d[m
Merge: 7fcce892 184fe7b8
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:07:30 2024 +0100

    Merge branch 'main' of https://github.com/AsquaredWsquared/SoundscapeAlpha

[33mcommit 184fe7b864be4170a1ae14d384906183b01c0f6a[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:03:58 2024 +0100

    Update README.md

[33mcommit 7fcce892c552c58bd6e22af741c4c585d4f7d191[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 09:01:00 2024 +0100

    update readme

[33mcommit bf9de17f88734d4859c641730c464389b2b538ff[m
Author: Adam Ward <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 08:59:42 2024 +0100

    Update README.md

[33mcommit 9bfc2ae3ddd2e04bf187c446df1db5659dae2a26[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 23 06:57:18 2024 +0100

    update doc string

[33mcommit b51de99acf762d0822d5a0c621479930c6af1fcf[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 20:21:41 2024 +0100

    add doc string

[33mcommit 8fa2869775ee97a55dbd060e49e6171b1830fc49[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 17:50:48 2024 +0100

    add doc string

[33mcommit 3d7f72c18112bab8ee6952a5ce7f0b4edbd1351b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 17:19:19 2024 +0100

    Looking at original Soundscape it tracks around clockwise from 6. Need to change relative direction functions to match

[33mcommit 2c73161e453ae910f1589ddaa6561aacc9a41710[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 15:38:56 2024 +0100

    Looking at original Soundscape it tracks around clockwise from 6. Need to change relative direction functions to match

[33mcommit db90a0914b3345dd21aa8e0c7b6b2c30ea82dbc0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 15:05:18 2024 +0100

    tidy up

[33mcommit c0aa888d1b9f851bd400c0dba36ac394acb6484d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 14:57:06 2024 +0100

    refactor getIndividualDirectionPolygons

[33mcommit 45e8cf3f3ab8cf06b81433f4d336ceeb815f7ec5[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 14:55:03 2024 +0100

    refactor getAheadBehindDirectionPolygons

[33mcommit c10187d5103e75728188ad89be0b79d7ad26ec14[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 14:34:20 2024 +0100

    tgetRelativeDirectionsPolygons test

[33mcommit e0eab38f6750620ecd993af47d28131484a2792d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 14:25:51 2024 +0100

    relativeDirectionsLeftRight refactor

[33mcommit 67888b854b71bde72416891d69a93ee6a8cbebd1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 14:18:26 2024 +0100

    getAheadBehindDirectionPolygons refactor

[33mcommit 8335ff1e0b4edfd9679e655273cf1f8a1ab74ff8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 14:05:53 2024 +0100

    getIndividualDirectionPolygons refactor

[33mcommit 58a9bc28375a2d0181c28236f479b059732cea7f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 13:56:01 2024 +0100

    getCombinedDirectionPolygons refactor

[33mcommit f9ada5f06cc3d3941bf53eb4e79b57eba5ae4ea4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 13:30:03 2024 +0100

    getCombinedDirectionPolygons refactor

[33mcommit 4c8c55a9ddb34f4a8333ede86e09bb5bf1e46ab5[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 12:05:53 2024 +0100

    getRelativeDirectionsPolygons function and enum for relative directions functions.

[33mcommit c19d7863d15f989d223f759649fcccc9ca68d82f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 11:14:28 2024 +0100

     getLeftRightDirectionPolygons bias to left and right of 120 degree triangles

[33mcommit 3552440d7481e1ad78b1cbd6f5771e2514d74334[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 09:40:48 2024 +0100

    getAheadBehindDirectionPolygons bias to ahead and behind of 150 degree triangles

[33mcommit 28a108f0ab6c7bff8d63853289578344f272127a[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 08:29:21 2024 +0100

    getIndividualDirectionPolygons 90 degreee triangles for ahead, right, behind, left directions

[33mcommit 0342adf44d3070c695d2c40f673fe7da29352a71[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 06:48:15 2024 +0100

    update TODO

[33mcommit 61e961acd21358d8fb868f13c1e89b1a9ded7e9b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Wed May 22 06:44:14 2024 +0100

    distanceToIntersection function

[33mcommit c0a889c560ae6c038c66d8b56fe50d0ab282f9f2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 22:02:40 2024 +0100

    getIntersectionRoadNames function to get the road names that make up intersection

[33mcommit 2074391049682bdb59b4d05f4958ca05be311d43[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 20:30:46 2024 +0100

    update doc string

[33mcommit ed176ffbf8835f0752c7cf44ef3ac4f50fa88a48[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 20:12:13 2024 +0100

    getRelativeDirectionsTest

[33mcommit 1c91bba1eb32263f423ac6f11ddcc9961c87d529[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 18:52:49 2024 +0100

    relativeDirectionsPolygonsTest to check that can report a point in a relative direction

[33mcommit 6cbc55126f8f9a52b936f862ca38ff6da795b151[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 18:24:21 2024 +0100

    getCombinedDirectionPolygons Relative directions using device heading, location and distance

[33mcommit edbc44e7d547e239b7fd28ce04117195711abd99[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 16:21:09 2024 +0100

    Relative directions using device heading

[33mcommit 91856d731296eea755cd8ea45b0e519394b538a2[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 08:10:25 2024 +0100

    tidy up

[33mcommit 2bab37475b48d8223f5dd889fb20bda36b394846[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 07:18:07 2024 +0100

    tidy up doc strings

[33mcommit 9d6275fedd991ab0f57e435cd679d80760ac3da9[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Tue May 21 06:49:41 2024 +0100

    tidy up doc strings

[33mcommit f91949152dd23fdae8fbad3049953edb0f33e9ba[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 20:17:06 2024 +0100

    remove unused imports

[33mcommit facde984d6b1be70ca300913ec5137602a17b917[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 20:14:55 2024 +0100

    Put some functions in the wrong file

[33mcommit 4d9c4495653cb6b5d766a466df2a940b9e513e3f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 20:04:24 2024 +0100

    tidy up

[33mcommit 1600a1ca6e4a56935fd8238fe8908bd11740a6a7[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 19:32:27 2024 +0100

    getNearestPoi and test

[33mcommit 59641a8c96b48a887f6c5a6ee5473cdf5a0e3ee1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 16:57:29 2024 +0100

    getNearestRoad Need to have a look at some tiles with motorways/dual carriageways as only handles LineStrings and Points

[33mcommit 106242c60f1e9cb80c3055f7530f5e52b1366c63[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 16:37:29 2024 +0100

    tidy up

[33mcommit 6201386b01e612c5beebcc5edbea64b00769a916[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 16:33:11 2024 +0100

    tidy up

[33mcommit e03b89d1d6c4048cdf11510a2fec1310a17208b4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 16:16:43 2024 +0100

    tidy up

[33mcommit 2a809c969f3304b186cf517ee09318579395e5f0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 15:02:34 2024 +0100

    getNearestRoad and test

[33mcommit 8929e0205e32e56c7dd491b46945cc3c08b2d7b1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 13:14:05 2024 +0100

    getNearestIntersection and test

[33mcommit e0d201d925dee4d5df30f3e071463b75e54b6669[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 08:59:20 2024 +0100

    poiFieldOfView test

[33mcommit 07e1ec56ee74c2c8772db2fd42d577234cd69ef8[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 08:31:11 2024 +0100

    getPoiInFovTest

[33mcommit eb2df99a4af6c956e2e2cd354b067ba7812d5e4b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 08:11:02 2024 +0100

    getFovIntersectionFeatureCollection, getFovRoadsFeatureCollection and getFovPoiFeatureCollection can be rolled into one as just repeating the same thing

[33mcommit e1b59621a48a1b32ecdc65590eac4ce681d7cce9[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 08:06:04 2024 +0100

    Added getFovPoiFeatureCollection function

[33mcommit 26f9d1e1a6841e161a501662a008b4d7fa611d10[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 07:55:22 2024 +0100

    Tidy up

[33mcommit 7f9b1dfd2a985e6437fa95542f8d36338ef46ce7[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 07:34:59 2024 +0100

    Tidy up getPoiFovFeatureCollection function

[33mcommit a983816ca9a3b742af579d4845087e3a8c2abb72[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 07:32:21 2024 +0100

    Added getPoiFovFeatureCollection function

[33mcommit d6fb6acabf473bbad277d3e55b792616083596e6[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 06:51:08 2024 +0100

    tidy up

[33mcommit 279562938e1d415374413afdceb919d9934401bc[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 06:12:23 2024 +0100

    tidy up

[33mcommit 1d25c402b9cdbb9068271d46efa15edde3bef906[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 06:09:47 2024 +0100

    added getRoadsInFovTest

[33mcommit 7f628ab58f9dfb47674e8b9384fc7b3a4ef123e1[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 06:05:06 2024 +0100

    added getIntersectionInFovTest

[33mcommit 05c941b33771494e6403137250620478cef37f83[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 05:56:58 2024 +0100

    Update getPathsFeatureCollection function name to getPathsFeatureCollectionFromTileFeatureCollection for consistency

[33mcommit 7fd186e651e448ddb4f172f5c8c38a041f6c7669[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Mon May 20 05:25:43 2024 +0100

    Added the roadsFieldOfView function to output FoV triangle and roads in it.

[33mcommit acfe1c76cbea5c730ae6cf4c016e966993d41c87[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 14:37:44 2024 +0100

    Added the intersectionsFieldOfView function to output FoV triangle and intersection

[33mcommit 65e9c08da20f56b288b0d95457bc6c80946d9129[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 13:33:03 2024 +0100

    Added the getFOVRoadsFeatureCollection function

[33mcommit 5d9ff6a1205658886414d45fd3413aa50214988e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 13:00:09 2024 +0100

    Test functions for some visual checks of the GeoJSON output and added a couple of FoV functions I forgot to include

[33mcommit 8f97bd6252eebaace264e81687f7905f6eef8294[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 11:12:24 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit 47e8e0b01ac316dc987927255eea709cc0d73e61[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 11:01:03 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit e03b124322667db4b81d0f18c3bec23c24bf4f71[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 10:01:12 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit 50b859172dd7738191857abba52ecf41317275aa[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 09:46:48 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit 32a65bb8edb93cacf390c1fc44dd03755c684265[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 09:37:09 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit a68a10a2250a28ad7f40b7548152f2ca6d935d6b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 09:34:35 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit 77e36d77da43ce8bc0ddd925324b751d76c1984f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 09:25:01 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit ceed6ec30fc71bc2967ae02f732311401d36ad19[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 08:42:02 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit 3d92da55695fbbe6bb75c1cb5f3d8b62c9451d9c[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 08:25:33 2024 +0100

    Test functions for some visual checks of the GeoJSON output

[33mcommit 44f4fc9e998978541512b2286e3295e20c99a1f4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 07:33:23 2024 +0100

    Forgot to add "Test" so had duplicate file names - oops!

[33mcommit 9165e57716df8191b42574f38036f3101779bd31[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sun May 19 07:15:24 2024 +0100

    getQuadrants test

[33mcommit 29c882301ba64d36dbdca07d10124014d209fb39[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 18:20:22 2024 +0100

    getQuadrants test

[33mcommit 30e52ebbce8b42a0d53e7b0b61dbd2b153b31076[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 18:14:54 2024 +0100

    getQuadrants function and Quadrant class

[33mcommit 08b2b18b67942419557cdf493c553eb17d21d369[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 18:01:34 2024 +0100

    createTriangleFOVTest function test

[33mcommit 9f8ab1f30f46e3e46ec196587a9b4eb24dbc429f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 17:10:19 2024 +0100

    circleToPolygon function

[33mcommit 87843ac05e1377646ef51f9a4b00e0cac8201394[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 17:06:26 2024 +0100

    createTriangleFOV function

[33mcommit c66e03b047286610651079456b3360dacebd598e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 16:44:30 2024 +0100

    spelling

[33mcommit a823d8256584727b26d7c4f61a212a9d2d76520b[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 16:43:07 2024 +0100

    cleanTileGeoJSON function

[33mcommit ab2f02cf39b8deb383020eb411a49267e6729f92[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 13:48:36 2024 +0100

    getReferenceCoordinate for LineString function and test

[33mcommit 3627d8b076e6c7df855f38bc3d391c47eb45290d[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 13:38:04 2024 +0100

    getDestinationCoordinate function and test

[33mcommit dc65a512c27d361caffc91fae7908a10c75cb789[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 13:24:39 2024 +0100

    polygonContainsCoordinates function and test

[33mcommit 26fe21acbcd8e4e5c6a8e1831cf83cffb00e1a50[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 12:57:12 2024 +0100

    bearingFromTwoPoints function and test

[33mcommit c1015445d0eac4370e05b5c346b4a1d02f516557[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 12:42:11 2024 +0100

    getPolygonOfBoundingBox function to create closed Polygon around bounding box coords

[33mcommit 5f6ce792c4cffa3520a194a1ee630dca9b8c68af[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 09:13:34 2024 +0100

    get Bounding box center coordinates

[33mcommit e39d3300252fa665843c2a602710e0de03ff2eb3[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 08:58:31 2024 +0100

    Bounding box corner coordinates

[33mcommit 40eeb13dc46b5aa058030cd3929a33a47d832451[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 08:48:59 2024 +0100

    Bounding box of MultiPolygon

[33mcommit 2c359e1304ad485b17e651537237fb7d0cb3ff42[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 08:19:10 2024 +0100

    Bounding box of Polygon test

[33mcommit 661fc41e66b17627783152f2d068c12c44b8f782[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 08:12:34 2024 +0100

    Bounding box of Polygon

[33mcommit b5b8ce2ac713969759748583de2cd916c45d0197[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 08:03:08 2024 +0100

    Bounding box of MultiLineString

[33mcommit 205112aca867d3d2d8d2c905dda5191e551d936c[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 07:47:20 2024 +0100

    Bounding box of MultiPoint

[33mcommit 282fa7d697507381e1d2b99a4b91fcecd291e2fd[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 07:40:55 2024 +0100

    Bounding box of LineString

[33mcommit 9c3602d31a16fc67eb6b6d145c3d19e55535e264[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 07:32:54 2024 +0100

    Bounding box of Point

[33mcommit 8b98bd4d1767ff6edeb5f01cf54a7f1102491699[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 07:27:31 2024 +0100

    Bounding box data class

[33mcommit 51e5e6f54e1e7ec4adadb4b2f09f34f7bcda2d71[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:44:33 2024 +0100

    getTilesForRegion tests for n x n tile grid depending on radius provided

[33mcommit 78b346dc8c13acbb87775122924c8ea0ecfaa215[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:38:41 2024 +0100

    getTilesForRegion tests for n x n tile grid depending on radius provided

[33mcommit f172c4cd539f3bde91c4fb291ce09c36d20f4a78[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:34:26 2024 +0100

    getTilesForRegion for n x n tile grid depending on radius provided

[33mcommit e415296d91517f1972082857708348149dfb0aaf[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:28:54 2024 +0100

    Data class to store tile info for 3 x 3 grid

[33mcommit cb4a81a713b7c3837304c4ec78b76ad249a59d23[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:24:10 2024 +0100

    getPixelXYToLatLon function need to get 3 x 3 grid

[33mcommit 55a16fc0e9a1da5f50063f9a09f7b77dfa4cf228[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:19:25 2024 +0100

    getPixelXY function need to get 3 x 3 grid

[33mcommit 6ee93d4348a1ae52f8ab19c0a229d7fe97a5f9a4[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 06:08:01 2024 +0100

    getQuadKey function

[33mcommit bd01dd2d6084facd409ee8e841d77172ca48226e[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 05:57:50 2024 +0100

    getTileXY function needed for creating 3 x 3 tile grid

[33mcommit df5992c56b2585c47a21d9b9d11442bcea06e895[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 05:56:01 2024 +0100

    groundResolution test function needed for creating 3 x 3 tile grid

[33mcommit 06949219f1916c6ce7e6717a70f91284f9fcde28[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 05:51:57 2024 +0100

    groundResolution function needed for creating 3 x 3 tile grid

[33mcommit 82529ef939ee473de076d593d711860adb877f4c[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 05:49:22 2024 +0100

    Clip function needed for creating 3 x 3 tile grid

[33mcommit bb478ef7713fa5d15a3e66662ffeb44d5c22534f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 05:47:12 2024 +0100

    Map size function needed for creating 3 x 3 tile grid

[33mcommit 85ba2adf068f67621907f846573a25b2c66d30a3[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 04:59:21 2024 +0100

    remove println

[33mcommit e2a8843966c622560452a8071757b037d237e75f[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 04:57:26 2024 +0100

    Get Super Categories feature collection from POI feature collection

[33mcommit 067155fc6e3f9d123918f246ca928a620da91937[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 04:46:23 2024 +0100

    Get POI feature collection from Tile

[33mcommit 4aa7ba4d22193c71d805082acd44f06398380f18[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 04:40:20 2024 +0100

    Get entrances FeatureCollection from real data

[33mcommit 484c2e00e72c43e404dd5c4b574180db19826104[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Sat May 18 04:35:44 2024 +0100

    Get intersections FeatureCollection from real data

[33mcommit cb95f4a16bb93c17f13e252aee966d35dcd57f9a[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 16:31:58 2024 +0100

    Get paths FeatureCollection from real data

[33mcommit 1d985e7d1750d87732a484041fba7d41a6b35a15[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 16:13:59 2024 +0100

    Get roads FeatureCollection from real data

[33mcommit c50164fbefdf49212f46916dc3a78b624d7f7f01[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 16:05:25 2024 +0100

    Splitting utils into Geo and Tile

[33mcommit 926d99d7f3d5c0f033a58821542c8cbf9b699aac[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 15:59:23 2024 +0100

    distance function update earth radius const

[33mcommit 015fe98760e35a3112c8585e853552bc8d1f0deb[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 14:08:52 2024 +0100

    distance function

[33mcommit e28a0a8730a8eaaf4a9ebc21089f49e263785697[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 14:00:31 2024 +0100

    GeoUtils and Test function getXYTile

[33mcommit 1ddd5bdc31e34da3628417262c04fc4bfa2e81de[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 13:42:55 2024 +0100

    Correct bbox data. Should be min lon, min lat, max lon, max lat

[33mcommit 41ed1b3b7b14bb5d878e18ab5e5a72e108747bd0[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 00:43:22 2024 +0100

    GeoJson test data and test

[33mcommit 883bcdc479ff447a282cb8628a1ec13796c49057[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Fri May 17 00:42:30 2024 +0100

    Update Gradle

[33mcommit af9dc32b7cbfd80fcbb8c2ded5f15cec29db3072[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 16 23:27:03 2024 +0100

    Fake data for each GeoJSON type on Null Island

[33mcommit 311a6d62fc0d9223ac70678b4a1ad02b19b4abbf[m
Author: AsquaredWsquared <76879959+AsquaredWsquared@users.noreply.github.com>
Date:   Thu May 16 21:34:27 2024 +0100

    Initial commit
