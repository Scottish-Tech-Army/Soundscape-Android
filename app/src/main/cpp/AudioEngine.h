#pragma once

#include <set>
#include <list>
#include <thread>
#include <mutex>
#include "fmod.hpp"
#include "fmod.h"
#include "BeaconDescriptor.h"

namespace soundscape {

    /**
     * iOS Soundscape supports a number of different sound types. Although the terms used are 2D and
     * 3D audio, we're really dealing with 1D (no positioning) and 2D audio (positioned on a plane).
     *
     *  standard - 2D audio
     *  localized - 3D audio localized to a GPS coordinate e.g. a beacon or a POI
     *  relative - 3D audio relative to the user's heading e.g. to the left, or to the right
     *  compass - 3D audio localized to a compass direction e.g. to the north, or to the west
     *
     * The initial implementation of PositionedAudio only supported localized, but it's been
     * extended to support the other modes which become important when we don't have access to a
     * heading from the device.
     */
    class PositioningMode {
    public:
        enum Type {
            STANDARD,
            LOCALIZED,
            RELATIVE,
            COMPASS
        };
        Type m_Type = STANDARD;
        double m_Latitude = 0.0;
        double m_Longitude = 0.0;
        double m_Heading = 0.0;

        PositioningMode() = default;
        PositioningMode(Type audio_type, double latitude, double longitude, double heading) :
                m_Type(audio_type),
                m_Latitude(latitude),
                m_Longitude(longitude),
                m_Heading(heading) {
        }

        [[nodiscard]] FMOD_MODE Get3DFlags() const {
            switch(m_Type) {
                default:
                // No positioning
                case STANDARD: return FMOD_2D;
                // Positioning based on a LatLngAlt
                case LOCALIZED: return FMOD_3D;
                // Positioning based on a heading relative to the head
                case RELATIVE: return FMOD_3D | FMOD_3D_HEADRELATIVE;
                // Positioning based on a compass direction. For this we make up a position a long
                // way away which should remain constant enough for the life of the audio.
                case COMPASS: return FMOD_3D;
            }
        }
    };


    class PositionedAudio;
    class AudioEngine {
    public:
        AudioEngine() noexcept;
        ~AudioEngine();

        void UpdateGeometry(double listenerLatitude,
                            double listenerLongitude,
                            double listenerHeading,
                            bool focusGained, bool duckingAllowed);
        FMOD::System * GetFmodSystem() const { return m_pSystem; };
        FMOD::ChannelGroup * GetBeaconGroup() const { return m_pBeaconChannelGroup; };
        FMOD::ChannelGroup * GetSpeechGroup() const { return m_pSpeechChannelGroup; };

        void SetBeaconType(int beaconType);
        const BeaconDescriptor *GetBeaconDescriptor() const;

        void AddBeacon(PositionedAudio *beacon, bool queued = false);
        void RemoveBeacon(PositionedAudio *beacon);
        bool ToggleBeaconMute();

        void Eof(long long id);

        const static BeaconDescriptor msc_BeaconDescriptors[];

        void GetListenerPosition(double &heading, double &latitude, double &longitude) const
        {
            heading = m_LastHeading;
            latitude = m_LastLatitude;
            longitude = m_LastLongitude;
        }

        void ClearQueue();
        unsigned int GetQueueDepth();

        FMOD_VECTOR TranslateToFmodVector(double longitude, double latitude);

    private:
        FMOD::System * m_pSystem;
        FMOD::ChannelGroup *m_pBeaconChannelGroup = nullptr;
        FMOD::ChannelGroup *m_pSpeechChannelGroup = nullptr;

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

        bool m_BeaconMute = false;
    };

} // soundscape
