#pragma once

#include <cmath>

//
// C++ versions of GeoUtils functions
//
const double DEGREES_TO_RADIANS = 2.0 * M_PI / 360.0;
const double RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS;
const double EARTH_RADIUS_METERS = 6378137.0;   //  Original Soundscape uses 6378137.0 not 6371000.0

inline double toRadians(double degrees)
{
    return degrees * DEGREES_TO_RADIANS;
}

inline double fromRadians(double degrees)
{
    return degrees * RADIANS_TO_DEGREES;
}

inline void getDestinationCoordinate(
        double start_lat,
        double start_lon,
        double bearing,
        double *end_lat,
        double *end_lon) {

    auto lat1 = toRadians(start_lat);
    auto lon1 = toRadians(start_lon);

    auto d = 1000.0 / EARTH_RADIUS_METERS; // Distance in radians for 1km away

    auto bearingRadians = toRadians(bearing);
    auto lat2 = asin(
        sin(lat1) * cos(d) +
        cos(lat1) * sin(d) * cos(bearingRadians));
    auto lon2 = lon1 + atan2(
        sin(bearingRadians) * sin(d) * cos(lat1),
        cos(d) - sin(lat1) * sin(lat2));

    *end_lat = fromRadians(lat2);
    *end_lon = fromRadians(lon2);
}

inline double bearingFromTwoPoints(
        double lat1, double lon1,
        double lat2, double lon2)
{
    auto latitude1 = toRadians(lat1);
    auto latitude2 = toRadians(lat2);
    auto longDiff = toRadians(lon2 - lon1);
    auto y = sin(longDiff) * cos(latitude2);
    auto x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff);

    return ((int)(fromRadians(atan2(y, x)) + 360) % 360) - 180;
}

inline double distance(double lat1, double long1, double lat2, double long2)
{
    auto deltaLat = toRadians(lat2 - lat1);
    auto deltaLon = toRadians(long2 - long1);

    auto a =
            sin(deltaLat / 2) * sin(deltaLat / 2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(
                    deltaLon / 2
            ) * sin(
                    deltaLon / 2
            );

    auto c = 2 * asin(sqrt(a));

    return (EARTH_RADIUS_METERS * c);
}

/**
 * The initial audio engine code made the assumption that the latitude and longitude could be used
 * directly as coordinates in the FMOD audio engine. These coordinates are used for positioning the
 * audio which affects where the audio sounds like it's coming from when it's played. However, FMOD
 * rightly assumes that 1 unit on the x-axis is the same distance as 1 unit on the y-axis, and this
 * isn't true for longitude and latitude and becomes less true further from the equator. This
 * function simply maps longitude and latitude so that x and y are the same.
 * Note that the change in beacon sound is done based on actual longitude and latitude and is
 * already calculated correctly. Prior to this function this meant there was a discrepancy between
 * the beacon tone changing and where it was positioned in the FMOD engine.
 *
 * @param latitude Location for the audio to sound from
 * @param longitude
 * @param fmod_x Location mapped to within the FMOD coordinate system
 * @param fmod_y
 */
inline void translateLocationForFmod(double latitude, double longitude,
                                     double origin_latitude, double origin_longitude,
                                     double &fmod_x, double &fmod_y)
{
    auto latRad = toRadians(latitude);
    auto lonRad = toRadians(longitude);
    auto originLatRad = toRadians(origin_latitude);
    auto originLonRad = toRadians(origin_longitude);

    // Calculate the difference in longitude
    double deltaLng = lonRad - originLonRad;

    // Calculate x and y coordinates using the Equirectangular projection
    fmod_x = EARTH_RADIUS_METERS * deltaLng * cos(originLatRad);
    fmod_y = EARTH_RADIUS_METERS * (latRad - originLatRad);
}
