#pragma once

#include <oboe/Oboe.h>
#include <vector>
#include <mutex>
#include <atomic>
#include <unordered_map>

#include "AudioSourceBase.h"
#include "SteamAudioSpatializer.h"

namespace soundscape {

    class AudioMixer : public oboe::AudioStreamDataCallback {
    public:
        AudioMixer();
        ~AudioMixer();

        bool start();
        void stop();
        int getSampleRate() const { return m_SampleRate; }

        // Source management (called from game thread)
        void addSource(AudioSourceBase *source);
        void removeSource(AudioSourceBase *source);

        // Volume control (called from game thread)
        void setBeaconVolume(float vol) { m_BeaconVolume.store(vol); }
        void setSpeechVolume(float vol) { m_SpeechVolume.store(vol); }

        // Spatialization mode (called from game thread)
        void setUseHrtf(bool use) { m_UseHrtf.store(use); }

        SteamAudioSpatializer *getSpatializer() { return m_Spatializer.get(); }

        // Oboe callback
        oboe::DataCallbackResult onAudioReady(
                oboe::AudioStream *stream, void *audioData, int32_t numFrames) override;

    private:
        static constexpr int FRAME_SIZE = 1024;

        int m_SampleRate = 48000;
        std::shared_ptr<oboe::AudioStream> m_Stream;

        std::unique_ptr<SteamAudioSpatializer> m_Spatializer;

        struct MixerSource {
            AudioSourceBase *source;
            int effectId = -1;  // Steam Audio binaural effect ID
        };

        std::mutex m_SourcesMutex;
        std::vector<MixerSource> m_Sources;

        std::atomic<float> m_BeaconVolume{1.0f};
        std::atomic<float> m_SpeechVolume{1.0f};
        std::atomic<bool>  m_UseHrtf{true};

        // Scratch buffers (allocated once, reused per callback)
        std::vector<float> m_MonoBuf;
        std::vector<float> m_StereoBuf;
    };

} // soundscape
