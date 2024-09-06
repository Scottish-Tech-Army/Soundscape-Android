#pragma once

#include <set>
#include <list>
#include <thread>
#include <mutex>
#include "fmod.hpp"
#include "fmod.h"
#include "BeaconDescriptor.h"

namespace soundscape {

    class PositionedAudio;
    class AudioEngine {
    public:
        AudioEngine() noexcept;
        ~AudioEngine();

        void UpdateGeometry(double listenerLatitude, double listenerLongitude, double listenerHeading);
        FMOD::System * GetFmodSystem() const { return m_pSystem; };

        void SetBeaconType(int beaconType);
        const BeaconDescriptor *GetBeaconDescriptor() const;

        void AddBeacon(PositionedAudio *beacon, bool queued = false);
        void RemoveBeacon(PositionedAudio *beacon);

        const static BeaconDescriptor msc_BeaconDescriptors[];

        void GetListenerPosition(double &heading, double &latitude, double &longitude) const
        {
            heading = m_LastHeading;
            latitude = m_LastPos.z;
            longitude = m_LastPos.x;
        }

    private:
        FMOD::System * m_pSystem;
        FMOD_VECTOR m_LastPos = {0.0f, 0.0f, 0.0f};

        double m_LastHeading = 0.0;

        std::atomic<int> m_BeaconTypeIndex;

        std::recursive_mutex m_BeaconsMutex;
        std::set<PositionedAudio *> m_Beacons;
        std::list<PositionedAudio *> m_QueuedBeacons;
    };

} // soundscape
