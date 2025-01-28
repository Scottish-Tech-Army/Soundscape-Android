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
            latitude = m_LastLatitude;
            longitude = m_LastLongitude;
        }

        void ClearQueue();

        FMOD_VECTOR TranslateToFmodVector(double longitude, double latitude);
        void TranslateFmodVector(FMOD_VECTOR &location);

    private:
        FMOD::System * m_pSystem;
        double m_LastLatitude = 0.0;
        double m_LastLongitude = 0.0;

        double m_FmodOriginLatitude = 0.0;
        double m_FmodOriginLongitude = 0.0;

        double m_LastHeading = 0.0;
        std::chrono::time_point<std::chrono::system_clock> m_LastTime;

        std::atomic<int> m_BeaconTypeIndex;

        std::recursive_mutex m_BeaconsMutex;
        std::set<PositionedAudio *> m_Beacons;
        std::list<PositionedAudio *> m_QueuedBeacons;
    };

} // soundscape
