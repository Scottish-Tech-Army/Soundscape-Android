#include "SteamAudioSpatializer.h"
#include "Trace.h"
#include <cstring>
#include <cmath>
#include <vector>

namespace soundscape {

    SteamAudioSpatializer::SteamAudioSpatializer(int sampleRate, int frameSize) {
        m_AudioSettings.samplingRate = sampleRate;
        m_AudioSettings.frameSize = frameSize;

        // Create context
        IPLContextSettings contextSettings{};
        contextSettings.version = STEAMAUDIO_VERSION;
        auto err = iplContextCreate(&contextSettings, &m_Context);
        if (err != IPL_STATUS_SUCCESS) {
            TRACE("SteamAudio: iplContextCreate failed: %d", err);
            m_Context = nullptr;
            return;
        }

        // Create HRTF
        IPLHRTFSettings hrtfSettings{};
        hrtfSettings.type = IPL_HRTFTYPE_DEFAULT;
        hrtfSettings.volume = 1.0f;
        hrtfSettings.normType = IPL_HRTFNORMTYPE_NONE;
        err = iplHRTFCreate(m_Context, &m_AudioSettings, &hrtfSettings, &m_Hrtf);
        if (err != IPL_STATUS_SUCCESS) {
            TRACE("SteamAudio: iplHRTFCreate failed: %d", err);
            iplContextRelease(&m_Context);
            m_Context = nullptr;
            return;
        }

        TRACE("SteamAudio: initialized (rate=%d, frameSize=%d)", sampleRate, frameSize);
    }

    SteamAudioSpatializer::~SteamAudioSpatializer() {
        // Destroy all remaining effects
        for (auto &pair : m_Effects) {
            if (pair.second.effect) {
                iplBinauralEffectRelease(&pair.second.effect);
            }
        }
        m_Effects.clear();

        if (m_Hrtf) {
            iplHRTFRelease(&m_Hrtf);
        }
        if (m_Context) {
            iplContextRelease(&m_Context);
        }
        TRACE("SteamAudio: destroyed");
    }

    int SteamAudioSpatializer::createSourceEffect() {
        if (!m_Context || !m_Hrtf) return -1;

        IPLBinauralEffectSettings effectSettings{};
        effectSettings.hrtf = m_Hrtf;

        IPLBinauralEffect effect = nullptr;
        auto err = iplBinauralEffectCreate(m_Context, &m_AudioSettings, &effectSettings, &effect);
        if (err != IPL_STATUS_SUCCESS) {
            TRACE("SteamAudio: iplBinauralEffectCreate failed: %d", err);
            return -1;
        }

        int id = m_NextId++;
        m_Effects[id] = {effect};
        return id;
    }

    void SteamAudioSpatializer::removeSourceEffect(int id) {
        auto it = m_Effects.find(id);
        if (it != m_Effects.end()) {
            if (it->second.effect) {
                iplBinauralEffectRelease(&it->second.effect);
            }
            m_Effects.erase(it);
        }
    }

    void SteamAudioSpatializer::spatialize(int effectId, const float *monoIn, float *stereoOut,
                                            int frames, float azimuth, float elevation) {
        auto it = m_Effects.find(effectId);
        if (it == m_Effects.end() || !it->second.effect) {
            // No effect - output silence
            memset(stereoOut, 0, frames * 2 * sizeof(float));
            return;
        }

        // Steam Audio uses right-handed: +x=right, +y=up, -z=forward
        // direction = unit vector from listener toward source
        IPLVector3 direction;
        direction.x = sinf(azimuth) * cosf(elevation);
        direction.y = sinf(elevation);
        direction.z = -cosf(azimuth) * cosf(elevation);

        // Set up deinterleaved input buffer (mono)
        float *inChannels[1] = {const_cast<float *>(monoIn)};
        IPLAudioBuffer inBuffer{};
        inBuffer.numChannels = 1;
        inBuffer.numSamples = frames;
        inBuffer.data = inChannels;

        // Set up deinterleaved output buffer (stereo)
        // We need separate L/R buffers then interleave
        std::vector<float> leftBuf(frames);
        std::vector<float> rightBuf(frames);
        float *outChannels[2] = {leftBuf.data(), rightBuf.data()};
        IPLAudioBuffer outBuffer{};
        outBuffer.numChannels = 2;
        outBuffer.numSamples = frames;
        outBuffer.data = outChannels;

        // Apply binaural effect
        IPLBinauralEffectParams params{};
        params.direction = direction;
        params.interpolation = IPL_HRTFINTERPOLATION_BILINEAR;
        params.spatialBlend = 1.0f;
        params.hrtf = m_Hrtf;
        params.peakDelays = nullptr;

        iplBinauralEffectApply(it->second.effect, &params, &inBuffer, &outBuffer);

        // Interleave to output
        for (int i = 0; i < frames; i++) {
            stereoOut[i * 2] = leftBuf[i];
            stereoOut[i * 2 + 1] = rightBuf[i];
        }
    }

} // soundscape
