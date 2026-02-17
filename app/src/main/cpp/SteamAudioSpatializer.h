#pragma once

#include "phonon.h"
#include <unordered_map>
#include <mutex>

namespace soundscape {

    class SteamAudioSpatializer {
    public:
        SteamAudioSpatializer(int sampleRate, int frameSize);
        ~SteamAudioSpatializer();

        bool isInitialized() const { return m_Context != nullptr; }
        int getFrameSize() const { return m_AudioSettings.frameSize; }

        // Create a per-source binaural effect, returns an ID
        int createSourceEffect();

        // Destroy a per-source effect
        void removeSourceEffect(int id);

        // Spatialize mono input to interleaved stereo output.
        // azimuth: 0 = ahead, positive = right (radians)
        // elevation: 0 = level, positive = up (radians)
        void spatialize(int effectId, const float *monoIn, float *stereoOut,
                        int frames, float azimuth, float elevation);

        // Get the IPLContext (for iplAudioBufferInterleave etc.)
        IPLContext getContext() const { return m_Context; }

    private:
        IPLContext m_Context = nullptr;
        IPLHRTF m_Hrtf = nullptr;
        IPLAudioSettings m_AudioSettings{};

        struct SourceEffect {
            IPLBinauralEffect effect = nullptr;
        };
        std::unordered_map<int, SourceEffect> m_Effects;
        int m_NextId = 0;
    };

} // soundscape
