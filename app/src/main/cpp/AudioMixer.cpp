#include "AudioMixer.h"
#include "Trace.h"
#include <cstring>
#include <algorithm>
#include <cmath>
#include <cstdint>

namespace soundscape {

    AudioMixer::AudioMixer() {
        m_MonoBuf.resize(FRAME_SIZE);
        m_StereoBuf.resize(FRAME_SIZE * 2);
    }

    AudioMixer::~AudioMixer() {
        stop();
    }

    bool AudioMixer::openStream() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
                ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                ->setSharingMode(oboe::SharingMode::Exclusive)
                ->setFormat(oboe::AudioFormat::Float)
                ->setChannelCount(oboe::ChannelCount::Stereo)
                ->setFramesPerDataCallback(FRAME_SIZE)
                ->setDataCallback(this)
                ->setErrorCallback(this);

        auto result = builder.openStream(m_Stream);
        if (result != oboe::Result::OK) {
            TRACE("AudioMixer: failed to open stream: %s", oboe::convertToText(result));
            return false;
        }

        m_SampleRate = m_Stream->getSampleRate();
        TRACE("AudioMixer: stream opened (rate=%d, framesPerCallback=%d, bufferCapacity=%d)",
              m_SampleRate, m_Stream->getFramesPerCallback(),
              m_Stream->getBufferCapacityInFrames());

        m_Spatializer = std::make_unique<SteamAudioSpatializer>(m_SampleRate, FRAME_SIZE);
        if (!m_Spatializer->isInitialized()) {
            TRACE("AudioMixer: spatializer init failed");
            m_Stream->close();
            m_Stream.reset();
            return false;
        }

        m_MonoBuf.resize(m_Stream->getBufferCapacityInFrames());
        m_StereoBuf.resize(m_Stream->getBufferCapacityInFrames() * 2);

        return true;
    }

    bool AudioMixer::startStream() {
        // Start the stream
        auto result = m_Stream->requestStart();
        if (result != oboe::Result::OK) {
            TRACE("AudioMixer: failed to start stream: %s", oboe::convertToText(result));
            m_Stream->close();
            m_Stream.reset();
            return false;
        }
        return true;
    }

    bool AudioMixer::start() {
        if(!openStream())
            return false;

        return startStream();
    }

    void AudioMixer::stop() {
        if (m_Stream) {
            m_Stream->requestStop();
            m_Stream->close();
            m_Stream.reset();
        }

        // Clean up spatializer effects
        {
            std::lock_guard<std::mutex> guard(m_SourcesMutex);
            for (auto &ms : m_Sources) {
                if (ms.effectId >= 0 && m_Spatializer) {
                    m_Spatializer->removeSourceEffect(ms.effectId);
                }
            }
            m_Sources.clear();
        }

        m_Spatializer.reset();
        TRACE("AudioMixer: stopped");
    }

    void AudioMixer::addSource(AudioSourceBase *source) {
        source->setDeviceSampleRate(m_SampleRate);

        MixerSource ms;
        ms.source = source;

        if (source->needsSpatialize && m_Spatializer) {
            ms.effectId = m_Spatializer->createSourceEffect();
        }
        {
            std::lock_guard<std::mutex> guard(m_SourcesMutex);
            m_Sources.push_back(ms);
        }
    }

    void AudioMixer::removeSource(AudioSourceBase *source) {
        int effectId = -1;
        {
            std::lock_guard<std::mutex> guard(m_SourcesMutex);

            for (auto it = m_Sources.begin(); it != m_Sources.end(); ++it) {
                if (it->source == source) {
                    if (it->effectId >= 0 && m_Spatializer)
                        effectId = it->effectId;
                    m_Sources.erase(it);
                    break;
                }
            }
        }
        if (effectId != -1)
            m_Spatializer->removeSourceEffect(effectId);
    }

    bool AudioMixer::restart() {
        TRACE("AudioMixer: restarting after disconnect");
        // Stream is already closed by Oboe before onErrorAfterClose fires; just drop the handle.
        m_Stream.reset();

        int prevRate = m_SampleRate;
        if (!openStream())
            return false;

        // Re-register all existing sources with the new spatializer.
        bool rateChanged = (m_SampleRate != prevRate);
        if (rateChanged)
            TRACE("AudioMixer: sample rate changed %d -> %d on restart", prevRate, m_SampleRate);
        {
            std::lock_guard<std::mutex> guard(m_SourcesMutex);
            for (auto &ms : m_Sources) {
                if (rateChanged)
                    ms.source->setDeviceSampleRate(m_SampleRate);
                ms.effectId = ms.source->needsSpatialize
                              ? m_Spatializer->createSourceEffect()
                              : -1;
            }
        }

        // Start the stream
        return startStream();
    }

    void AudioMixer::onErrorAfterClose(oboe::AudioStream * /*stream*/, oboe::Result result) {
        TRACE("AudioMixer: onErrorAfterClose: %s", oboe::convertToText(result));
        if (result == oboe::Result::ErrorDisconnected) {
            restart();
        }
    }

    oboe::DataCallbackResult AudioMixer::onAudioReady(
            oboe::AudioStream *stream, void *audioData, int32_t numFrames) {

        auto *output = static_cast<float *>(audioData);

        // Clear output
        memset(output, 0, numFrames * 2 * sizeof(float));

        std::lock_guard<std::mutex> guard(m_SourcesMutex);

        float beaconVol = m_BeaconVolume.load();
        float speechVol = m_SpeechVolume.load();
        if(m_Sources.size() > 1) {
            // We're mixing a beacon with speech. We want to dip the beacon under the speech, and we
            // want to avoid clipping.
            beaconVol /= 4;
            speechVol *= 3.0/4.0;
        }
        for (auto &ms : m_Sources) {
            auto *src = ms.source;

            if (src->isFinished() || src->muted.load()) {
                continue;
            }

            // Read mono audio from source
            int framesRead = src->readPcm(m_MonoBuf.data(), numFrames);
            if (framesRead <= 0) {
                continue;
            }

            // Pad with silence if needed
            if (framesRead < numFrames) {
                memset(m_MonoBuf.data() + framesRead, 0,
                       (numFrames - framesRead) * sizeof(float));
            }

            // Get volume for this source's category
            float vol = (src->category == AudioCategory::BEACON) ? beaconVol : speechVol;
            if (src->needsSpatialize && ms.effectId >= 0 && m_Spatializer && m_UseHrtf.load()) {
                // Spatialize: mono -> stereo HRTF
                float az = src->azimuth.load();
                float el = src->elevation.load();

                m_Spatializer->spatialize(ms.effectId, m_MonoBuf.data(),
                                          m_StereoBuf.data(), numFrames, az, el);

                // Mix into output with volume
                float rearFactor = 0.5f + (0.5f * 0.5f * (1.0f + cosf(az)));
                vol *= rearFactor;
                for (int i = 0; i < numFrames * 2; i++) {
                    output[i] += m_StereoBuf[i] * vol;
                }
            } else if (src->needsSpatialize && !m_UseHrtf.load()) {

                // Stereo pan over full 360°: sin(az) gives a smooth, periodic response with
                // no jumps. 0=center, +π/2=right, π=center(behind), -π/2=left.
                float az = src->azimuth.load();
                float pan = sinf(az);
                float panAngle = (pan + 1.0f) * (float)M_PI_4;

                // Rear attenuation: raised cosine from 1.0 (ahead) to 0.0 (behind)
                float rearFactor = 0.5f + (0.5f * 0.5f * (1.0f + cosf(az)));

                float attVol = vol * rearFactor;
                float leftGain  = cosf(panAngle) * attVol;
                float rightGain = sinf(panAngle) * attVol;
                for (int i = 0; i < numFrames; i++) {
                    output[i * 2]     += m_MonoBuf[i] * leftGain;
                    output[i * 2 + 1] += m_MonoBuf[i] * rightGain;
                }
            } else {
                // Non-spatialized: duplicate mono to stereo
                for (int i = 0; i < numFrames; i++) {
                    float s = m_MonoBuf[i] * vol;
                    output[i * 2] += s;
                    output[i * 2 + 1] += s;
                }
            }
        }

        // Clamp output to [-1, 1]
        for (int i = 0; i < numFrames * 2; i++) {
            output[i] = std::clamp(output[i], -1.0f, 1.0f);
        }

        return oboe::DataCallbackResult::Continue;
    }

} // soundscape
