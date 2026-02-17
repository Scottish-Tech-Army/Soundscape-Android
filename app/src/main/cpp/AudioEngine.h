#pragma once

#include <set>
#include <list>
#include <thread>
#include <mutex>
#include <jni.h>
#include <android/asset_manager.h>
#include "BeaconDescriptor.h"
#include "AudioMixer.h"

namespace soundscape {

    /**
     * iOS Soundscape supports a number of different sound types. Although the terms used are 2D and
     * 3D audio, we're really dealing with 1D (no positioning) and 2D audio (positioned on a plane).
     *
     *  standard - 2D audio
     *  localized - 3D audio localized to a GPS coordinate e.g. a beacon or a POI
     *  relative - 3D audio relative to the user's heading e.g. to the left, or to the right
     *  compass - 3D audio localized to a compass direction e.g. to the north, or to the west
     */
    class PositioningMode {
    public:
        enum AudioType {
            STANDARD,
            LOCALIZED,
            RELATIVE,
            COMPASS
        };
        AudioType m_AudioType = STANDARD;

        enum AudioMode {
            HEADING,
            PROXIMITY
        };
        AudioMode m_AudioMode = HEADING;

        double m_Latitude = 0.0;
        double m_Longitude = 0.0;
        double m_Heading = 0.0;

        PositioningMode() = default;
        PositioningMode(AudioType audio_type, AudioMode audio_mode, double latitude, double longitude, double heading) :
                m_AudioType(audio_type),
                m_AudioMode(audio_mode),
                m_Latitude(latitude),
                m_Longitude(longitude),
                m_Heading(heading) {
        }
    };


    class PositionedAudio;
    class AudioEngine {
    public:
        explicit AudioEngine(AAssetManager *assetManager) noexcept;
        ~AudioEngine();

        void UpdateGeometry(double listenerLatitude,
                            double listenerLongitude,
                            double listenerHeading,
                            bool focusGained, bool duckingAllowed,
                            double proximityNear);

        AudioMixer *GetMixer() { return m_pMixer.get(); }
        AAssetManager *GetAssetManager() const { return m_pAssetManager; }

        void SetBeaconType(int beaconType);
        const BeaconDescriptor *GetBeaconDescriptor() const;

        void AddBeacon(PositionedAudio *beacon, bool queued = false);
        void RemoveBeacon(PositionedAudio *beacon);
        bool ToggleBeaconMute();

        void Eof(long long id);
        void BeaconDestroyed();

        // Method to be called from Kotlin to set up the callback
        void SetBeaconEventsListener(JNIEnv *env, jobject listener_obj);
        void ClearBeaconEventsListener(JNIEnv *env); // To release the global ref

        const static BeaconDescriptor msc_BeaconDescriptors[];

        void GetListenerPosition(double &heading, double &latitude, double &longitude) const
        {
            heading = m_LastHeading;
            latitude = m_LastLatitude;
            longitude = m_LastLongitude;
        }

        void ClearQueue();
        unsigned int GetQueueDepth();

        void UpdateAudioConfig(std::string &utterance_id,
                               int sample_rate,
                               int audio_format,
                               int channel_count);

    private:
        AAssetManager *m_pAssetManager;
        std::unique_ptr<AudioMixer> m_pMixer;

        double m_LastLatitude = 0.0;
        double m_LastLongitude = 0.0;
        double m_LastHeading = 0.0;
        std::chrono::time_point<std::chrono::system_clock> m_LastTime;

        std::atomic<int> m_BeaconTypeIndex;

        std::recursive_mutex m_BeaconsMutex;
        std::set<PositionedAudio *> m_Beacons;
        std::list<PositionedAudio *> m_QueuedBeacons;
        bool m_QueuedBeaconPlaying = false;

        bool m_BeaconMute = false;

        // For JNI callbacks
        JavaVM *m_pJvm = nullptr;
        jobject m_jBeaconListener = nullptr;
        jmethodID m_jMethodId_onAllBeaconsCleared = nullptr;

        // Helper to notify Kotlin
        void NotifyAllBeaconsCleared(int line);
    };

} // soundscape
