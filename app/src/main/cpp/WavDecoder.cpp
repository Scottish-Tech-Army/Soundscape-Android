#include "WavDecoder.h"
#include "Trace.h"
#include <cstring>
#include <cmath>
#include <algorithm>

namespace soundscape {

    std::string WavDecoder::stripAssetPrefix(const std::string &path) {
        const std::string prefix = "file:///android_asset/";
        if (path.compare(0, prefix.size(), prefix) == 0) {
            return path.substr(prefix.size());
        }
        return path;
    }

    WavDecoder::WavDecoder(AAssetManager *mgr, const std::string &path, int targetRate) {
        std::string assetPath = stripAssetPrefix(path);

        AAsset *asset = AAssetManager_open(mgr, assetPath.c_str(), AASSET_MODE_BUFFER);
        if (!asset) {
            TRACE("WavDecoder: failed to open asset: %s", assetPath.c_str());
            return;
        }

        auto rawSize = static_cast<size_t>(AAsset_getLength(asset));
        auto rawData = static_cast<const unsigned char *>(AAsset_getBuffer(asset));

        if (rawData && rawSize > 44) {
            parseWav(rawData, rawSize);
        } else {
            TRACE("WavDecoder: asset too small or null: %s (%zu bytes)", assetPath.c_str(), rawSize);
        }

        AAsset_close(asset);

        if (targetRate > 0 && m_SampleRate != targetRate && !m_Data.empty()) {
            resampleTo(targetRate);
        }
    }

    void WavDecoder::parseWav(const unsigned char *rawData, size_t rawSize) {
        // Validate RIFF header
        if (memcmp(rawData, "RIFF", 4) != 0 || memcmp(rawData + 8, "WAVE", 4) != 0) {
            TRACE("WavDecoder: not a valid WAV file");
            return;
        }

        // Find fmt and data chunks
        int sampleRate = 0;
        int bitsPerSample = 0;
        int numChannels = 0;
        int audioFormat = 0;
        const unsigned char *dataChunk = nullptr;
        uint32_t dataSize = 0;

        size_t pos = 12; // skip RIFF header
        while (pos + 8 <= rawSize) {
            char chunkId[5] = {0};
            memcpy(chunkId, rawData + pos, 4);
            uint32_t chunkSize;
            memcpy(&chunkSize, rawData + pos + 4, 4);

            if (memcmp(chunkId, "fmt ", 4) == 0 && pos + 8 + chunkSize <= rawSize) {
                memcpy(&audioFormat, rawData + pos + 8, 2);
                memcpy(&numChannels, rawData + pos + 10, 2);
                numChannels &= 0xFFFF;
                memcpy(&sampleRate, rawData + pos + 12, 4);
                memcpy(&bitsPerSample, rawData + pos + 22, 2);
                bitsPerSample &= 0xFFFF;
            } else if (memcmp(chunkId, "data", 4) == 0) {
                dataChunk = rawData + pos + 8;
                dataSize = chunkSize;
                if (pos + 8 + dataSize > rawSize) {
                    dataSize = static_cast<uint32_t>(rawSize - pos - 8);
                }
                break;
            }

            pos += 8 + chunkSize;
            if (chunkSize % 2 != 0) pos++; // pad byte
        }

        if (!dataChunk || sampleRate == 0 || numChannels == 0 || bitsPerSample == 0) {
            TRACE("WavDecoder: incomplete WAV (sr=%d ch=%d bits=%d data=%p)",
                  sampleRate, numChannels, bitsPerSample, dataChunk);
            return;
        }

        m_OriginalSampleRate = sampleRate;
        m_SampleRate = sampleRate;

        // Calculate number of sample frames
        int bytesPerSample = bitsPerSample / 8;
        int bytesPerFrame = bytesPerSample * numChannels;
        int numFrames = static_cast<int>(dataSize / bytesPerFrame);

        m_Data.resize(numFrames);

        // Convert to mono float32
        for (int i = 0; i < numFrames; i++) {
            float sample = 0.0f;
            for (int ch = 0; ch < numChannels; ch++) {
                const unsigned char *src = dataChunk + (i * bytesPerFrame) + (ch * bytesPerSample);

                float chSample = 0.0f;
                if (bitsPerSample == 8) {
                    // PCM8 is unsigned (0-255, 128 = silence)
                    chSample = (static_cast<float>(src[0]) - 128.0f) / 128.0f;
                } else if (bitsPerSample == 16) {
                    int16_t s;
                    memcpy(&s, src, 2);
                    chSample = static_cast<float>(s) / 32768.0f;
                } else if (bitsPerSample == 32 && audioFormat == 3) {
                    // IEEE float
                    memcpy(&chSample, src, 4);
                } else if (bitsPerSample == 32) {
                    // PCM32
                    int32_t s;
                    memcpy(&s, src, 4);
                    chSample = static_cast<float>(s) / 2147483648.0f;
                } else if (bitsPerSample == 24) {
                    // PCM24 little-endian
                    int32_t s = (src[0] | (src[1] << 8) | (src[2] << 16));
                    if (s & 0x800000) s |= 0xFF000000; // sign extend
                    chSample = static_cast<float>(s) / 8388608.0f;
                }
                sample += chSample;
            }
            m_Data[i] = sample / static_cast<float>(numChannels); // downmix
        }
    }

    void WavDecoder::resampleTo(int targetRate) {
        if (m_SampleRate == targetRate || m_Data.empty()) return;

        double ratio = static_cast<double>(m_SampleRate) / static_cast<double>(targetRate);
        int outFrames = static_cast<int>(static_cast<double>(m_Data.size()) / ratio);

        std::vector<float> resampled(outFrames);

        for (int i = 0; i < outFrames; i++) {
            double srcPos = i * ratio;
            int srcIdx = static_cast<int>(srcPos);
            float frac = static_cast<float>(srcPos - srcIdx);

            if (srcIdx + 1 < static_cast<int>(m_Data.size())) {
                resampled[i] = m_Data[srcIdx] * (1.0f - frac) + m_Data[srcIdx + 1] * frac;
            } else if (srcIdx < static_cast<int>(m_Data.size())) {
                resampled[i] = m_Data[srcIdx];
            } else {
                resampled[i] = 0.0f;
            }
        }

        m_Data = std::move(resampled);
        m_SampleRate = targetRate;
    }

} // soundscape
