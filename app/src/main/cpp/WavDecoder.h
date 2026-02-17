#pragma once

#include <android/asset_manager.h>
#include <string>
#include <vector>
#include <memory>

namespace soundscape {

    class WavDecoder {
    public:
        // Load WAV from AAssetManager. If targetRate > 0, resample to that rate.
        WavDecoder(AAssetManager *mgr, const std::string &path, int targetRate = 0);

        const float *data() const { return m_Data.data(); }
        int numFrames() const { return static_cast<int>(m_Data.size()); }
        int sampleRate() const { return m_SampleRate; }
        int originalSampleRate() const { return m_OriginalSampleRate; }
        bool isValid() const { return !m_Data.empty(); }

    private:
        void parseWav(const unsigned char *rawData, size_t rawSize);
        void resampleTo(int targetRate);

        // Strip "file:///android_asset/" prefix if present
        static std::string stripAssetPrefix(const std::string &path);

        std::vector<float> m_Data;  // mono float32 samples
        int m_SampleRate = 0;
        int m_OriginalSampleRate = 0;
    };

} // soundscape
