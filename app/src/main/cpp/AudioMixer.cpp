#include "AudioMixer.h"
#include "Trace.h"
#include <cstring>
#include <algorithm>

namespace soundscape {

    AudioMixer::AudioMixer() {
        m_MonoBuf.resize(FRAME_SIZE);
        m_StereoBuf.resize(FRAME_SIZE * 2);
    }

    AudioMixer::~AudioMixer() {
        stop();
    }

    bool AudioMixer::start() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
                ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
                ->setSharingMode(oboe::SharingMode::Exclusive)
                ->setFormat(oboe::AudioFormat::Float)
                ->setChannelCount(oboe::ChannelCount::Stereo)
                ->setFramesPerCallback(FRAME_SIZE)
                ->setDataCallback(this);

        auto result = builder.openStream(m_Stream);
        if (result != oboe::Result::OK) {
            TRACE("AudioMixer: failed to open stream: %s", oboe::convertToText(result));
            return false;
        }

        m_SampleRate = m_Stream->getSampleRate();
        TRACE("AudioMixer: stream opened (rate=%d, framesPerCallback=%d, bufferCapacity=%d)",
              m_SampleRate, m_Stream->getFramesPerCallback(),
              m_Stream->getBufferCapacityInFrames());

        // Create spatializer with actual stream sample rate
        m_Spatializer = std::make_unique<SteamAudioSpatializer>(m_SampleRate, FRAME_SIZE);
        if (!m_Spatializer->isInitialized()) {
            TRACE("AudioMixer: spatializer init failed");
            m_Stream->close();
            m_Stream.reset();
            return false;
        }

        // Resize scratch buffers to handle any callback size
        m_MonoBuf.resize(m_Stream->getBufferCapacityInFrames());
        m_StereoBuf.resize(m_Stream->getBufferCapacityInFrames() * 2);

        result = m_Stream->requestStart();
        if (result != oboe::Result::OK) {
            TRACE("AudioMixer: failed to start stream: %s", oboe::convertToText(result));
            m_Stream->close();
            m_Stream.reset();
            return false;
        }

        TRACE("AudioMixer: stream started");
        return true;
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
        std::lock_guard<std::mutex> guard(m_SourcesMutex);

        source->setDeviceSampleRate(m_SampleRate);

        MixerSource ms;
        ms.source = source;

        if (source->needsSpatialize && m_Spatializer) {
            ms.effectId = m_Spatializer->createSourceEffect();
        }

        m_Sources.push_back(ms);
        TRACE("AudioMixer: addSource -> %zu sources", m_Sources.size());
    }

    void AudioMixer::removeSource(AudioSourceBase *source) {
        std::lock_guard<std::mutex> guard(m_SourcesMutex);

        for (auto it = m_Sources.begin(); it != m_Sources.end(); ++it) {
            if (it->source == source) {
                if (it->effectId >= 0 && m_Spatializer) {
                    m_Spatializer->removeSourceEffect(it->effectId);
                }
                m_Sources.erase(it);
                break;
            }
        }
    }

    oboe::DataCallbackResult AudioMixer::onAudioReady(
            oboe::AudioStream *stream, void *audioData, int32_t numFrames) {

        auto *output = static_cast<float *>(audioData);

        // Clear output
        memset(output, 0, numFrames * 2 * sizeof(float));

        // Try to lock sources - if contention, output silence this frame
        std::unique_lock<std::mutex> lock(m_SourcesMutex, std::try_to_lock);
        if (!lock.owns_lock()) {
            return oboe::DataCallbackResult::Continue;
        }

        float beaconVol = m_BeaconVolume.load();
        float speechVol = m_SpeechVolume.load();

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

            if (src->needsSpatialize && ms.effectId >= 0 && m_Spatializer) {
                // Spatialize: mono -> stereo HRTF
                float az = src->azimuth.load();
                float el = src->elevation.load();

                m_Spatializer->spatialize(ms.effectId, m_MonoBuf.data(),
                                          m_StereoBuf.data(), numFrames, az, el);

                // Mix into output with volume
                for (int i = 0; i < numFrames * 2; i++) {
                    output[i] += m_StereoBuf[i] * vol;
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
