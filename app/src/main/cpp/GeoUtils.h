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
