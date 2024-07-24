#pragma once

#include <cmath>

//
// C++ versions of GeoUtils functions
//
const double DEGREES_TO_RADIANS = 2.0 * M_PI / 360.0;
const double RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS;
const double EARTH_RADIUS_METERS = 6378137.0;   //  Original Soundscape uses 6378137.0 not 6371000.0

// We use this for FMOD coordinates so that we can just pass in straight GPS values as if they
// were X/Y coordinates. We can only do this because we're always close enough to our beacons to
// consider the earth as flat. FMOD_DISTANCE_FACTOR is set to the number of metres per degree of
// longitude/latitude.
const float FMOD_DISTANCE_FACTOR = static_cast<float>((2.0 * M_PI * EARTH_RADIUS_METERS) / 360.0);

inline double toRadians(double degrees)
{
    return degrees * DEGREES_TO_RADIANS;
}

inline double fromRadians(double degrees)
{
    return degrees * RADIANS_TO_DEGREES;
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

inline void getDestinationCoordinate(double lat, double lon, double bearing, double distance, double &new_lat, double &new_lon)
{
    auto lat1 = toRadians(lat);
    auto lon1 = toRadians(lon);

    auto d = distance / EARTH_RADIUS_METERS; // Distance in radians

    auto bearingRadians = toRadians(bearing);

    auto lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(bearingRadians));
    auto lon2 = lon1 + atan2(sin(bearingRadians) * sin(d) * cos(lat1),
                                    cos(d) - sin(lat1) * sin(lat2));

    new_lat = fromRadians(lat2);
    new_lon = fromRadians(lon2);
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
