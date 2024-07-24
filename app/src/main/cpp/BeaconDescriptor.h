#pragma once

#include <vector>
#include <string>
#include <utility>

namespace soundscape {

    class BeaconAsset {
    public:
        BeaconAsset(const std::string &filename, double max_angle)
        : m_Filename(filename), m_MaxAngle(max_angle)
        {
        }

        double m_MaxAngle;
        std::string m_Filename;
    };

    class BeaconDescriptor {
    public:
        BeaconDescriptor(unsigned int beats_in_phrase, const std::vector<BeaconAsset> &beacons)
        : m_BeatsInPhrase(beats_in_phrase),
          m_Beacons(beacons)
        {
        }

        unsigned int m_BeatsInPhrase;
        const std::vector<BeaconAsset> m_Beacons;
    };

} // soundscape
