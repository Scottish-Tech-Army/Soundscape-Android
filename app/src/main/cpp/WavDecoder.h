#pragma once

#include <android/asset_manager.h>
#include <string>
#include <vector>
#include <memory>

namespace soundscape {

    class WavDecoder {
    public:
        // Load WAV from AAssetManager. If targetRate > 0, resample to that rate.
        //
        // Decoded PCM data is cached in memory, keyed by (asset path, targetRate). Earcons and
        // beacon segments are re-created from their asset on every playback, and without this
        // cache each of those calls re-opens and re-parses the WAV file while holding the
        // engine's lock - AAssetManager_open serializes on a process-wide native mutex, so
        // concurrent loads from multiple threads can stall that lock for long enough to ANR the
        // main thread.
        WavDecoder(AAssetManager *mgr, const std::string &path, int targetRate = 0);

        const float *data() const { return m_Data ? m_Data->samples.data() : nullptr; }
        int numFrames() const { return m_Data ? static_cast<int>(m_Data->samples.size()) : 0; }
        int sampleRate() const { return m_SampleRate; }

        int originalSampleRate() const { return m_OriginalSampleRate; }
        bool isValid() const { return m_Data && !m_Data->samples.empty(); }

    private:
        struct DecodedWav {
            std::vector<float> samples;  // mono float32 samples
            int sampleRate = 0;
            int originalSampleRate = 0;
        };

        static std::shared_ptr<const DecodedWav> loadCached(AAssetManager *mgr,
                                                              const std::string &path,
                                                              int targetRate);
        static std::shared_ptr<DecodedWav> decode(AAssetManager *mgr, const std::string &path,
                                                    int targetRate);

        static void parseWav(DecodedWav &out, const unsigned char *rawData, size_t rawSize);
        static void resampleTo(DecodedWav &out, int targetRate);

        // Strip "file:///android_asset/" prefix if present
        static std::string stripAssetPrefix(const std::string &path);

        std::shared_ptr<const DecodedWav> m_Data;
        int m_SampleRate = 0;
        int m_OriginalSampleRate = 0;
    };

} // soundscape
