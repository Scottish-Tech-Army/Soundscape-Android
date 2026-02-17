#include <unistd.h>
#include <thread>
#include <cassert>
#include <android/log.h>
#include <fcntl.h>
#include <cmath>
#include <jni.h>
#include <cstring>
#include "AudioBeaconBuffer.h"
#include "BeaconDescriptor.h"
#include "AudioBeacon.h"
#include "Trace.h"

using namespace soundscape;

//
// BeaconBuffer
//
BeaconBuffer::BeaconBuffer(AAssetManager *mgr, const std::string &filename,
                           double max_angle, int targetSampleRate)
        : m_MaxAngle(max_angle),
          m_Name(filename) {
    m_Decoder = std::make_unique<WavDecoder>(mgr, filename, targetSampleRate);
    if (!m_Decoder->isValid()) {
        TRACE("BeaconBuffer: failed to load %s", filename.c_str());
    }
}

BeaconBuffer::~BeaconBuffer() {
    TRACE("~BeaconBuffer");
}

bool BeaconBuffer::CheckIsActive(double degrees_off_axis) const {
    return fabs(degrees_off_axis) <= m_MaxAngle;
}

unsigned int BeaconBuffer::Read(float *data, unsigned int numFrames, unsigned long pos,
                                bool pad_with_silence) {
    if (!m_Decoder || !m_Decoder->isValid()) {
        memset(data, 0, numFrames * sizeof(float));
        return 0;
    }

    auto totalFrames = static_cast<unsigned int>(m_Decoder->numFrames());
    const float *src = m_Decoder->data();

    pos %= totalFrames;
    unsigned int remainder = 0;
    unsigned int toRead = numFrames;

    if ((totalFrames - pos) < numFrames) {
        remainder = numFrames - (totalFrames - pos);
        toRead = totalFrames - pos;
    }

    memcpy(data, src + pos, toRead * sizeof(float));

    if (remainder) {
        if (pad_with_silence) {
            memset(data + toRead, 0, remainder * sizeof(float));
        } else {
            // Loop from start
            memcpy(data + toRead, src, remainder * sizeof(float));
        }
    }

    return numFrames;
}

//
// BeaconAudioSource
//
BeaconAudioSource::BeaconAudioSource(PositionedAudio *parent, double degrees_off_axis)
        : m_pParent(parent),
          m_DegreesOffAxis(degrees_off_axis) {
}

void BeaconAudioSource::UpdateGeometry(double degrees_off_axis,
                                        BeaconAudioSource::SourceMode mode) {
    m_DegreesOffAxis = degrees_off_axis;
    m_Mode = mode;
}

//
// BeaconBufferGroup
//
BeaconBufferGroup::BeaconBufferGroup(AAssetManager *mgr,
                                     const BeaconDescriptor *beacon_descriptor,
                                     PositionedAudio *parent,
                                     double degrees_off_axis,
                                     int targetSampleRate)
        : BeaconAudioSource(parent, degrees_off_axis) {
    TRACE("Create BeaconBufferGroup %p", this);
    m_pDescription = beacon_descriptor;

    for (const auto &asset : m_pDescription->m_Beacons) {
        auto buffer = std::make_unique<BeaconBuffer>(mgr, asset.m_Filename,
                                                     asset.m_MaxAngle, targetSampleRate);
        m_pBuffers.push_back(std::move(buffer));
    }

    m_pIntro = std::make_unique<BeaconBuffer>(mgr,
                                               "file:///android_asset/Route/Route_Start.wav",
                                               180.0, targetSampleRate);
    m_pOutro = std::make_unique<BeaconBuffer>(mgr,
                                               "file:///android_asset/Route/Route_End.wav",
                                               180.0, targetSampleRate);
}

BeaconBufferGroup::~BeaconBufferGroup() {
    TRACE("~BeaconBufferGroup %p", this);
}

void BeaconBufferGroup::UpdateCurrentBufferFromHeadingAndLocation() {
    if (m_PlayState == PLAYING_INTRO) {
        m_pCurrentBuffer = m_pIntro.get();
        return;
    } else if (m_PlayState == PLAYING_OUTRO) {
        m_pCurrentBuffer = m_pOutro.get();
        return;
    }

    switch (m_Mode) {
        case BeaconAudioSource::DIRECTION_MODE: {
            for (const auto &buffer : m_pBuffers) {
                if (buffer->CheckIsActive(m_DegreesOffAxis)) {
                    m_pCurrentBuffer = buffer.get();
                    break;
                }
            }
            if (m_pCurrentBuffer == nullptr)
                m_pCurrentBuffer = m_pBuffers[0].get();
            break;
        }
        case BeaconAudioSource::NEAR_MODE: {
            m_pCurrentBuffer = m_pBuffers[0].get();
            break;
        }
        case BeaconAudioSource::FAR_MODE: {
            m_pCurrentBuffer = m_pBuffers[1].get();
            break;
        }
        case BeaconAudioSource::TOO_FAR_MODE: {
            m_pCurrentBuffer = nullptr;
            break;
        }
    }
}

int BeaconBufferGroup::readPcm(float *outMono, int numFrames) {
    if (m_PlayState == PLAYING_COMPLETE) {
        return 0;
    }

    UpdateCurrentBufferFromHeadingAndLocation();

    if (m_pCurrentBuffer == nullptr) {
        // Silence (e.g. TOO_FAR_MODE)
        memset(outMono, 0, numFrames * sizeof(float));
        return numFrames;
    }

    unsigned int framesRead = m_pCurrentBuffer->Read(outMono, numFrames, m_FramePos,
                                                      m_PlayState != PLAYING_BEACON);
    m_FramePos += framesRead;

    if (m_PlayState == PLAYING_INTRO) {
        if (m_FramePos >= m_pIntro->GetNumFrames()) {
            m_PlayState = PLAYING_BEACON;
            m_FramePos = 0;
        }
    } else if (m_PlayState == PLAYING_OUTRO) {
        if (m_FramePos >= m_pOutro->GetNumFrames()) {
            m_PlayState = PLAYING_COMPLETE;
        }
    }

    return numFrames;
}

bool BeaconBufferGroup::isFinished() const {
    return m_PlayState == PLAYING_COMPLETE;
}

//
// TtsAudioSource
//

TtsAudioSource::TtsAudioSource(PositionedAudio *parent,
                               int tts_socket,
                               int sampleRate, int audioFormat, int channelCount)
        : BeaconAudioSource(parent, 0) {
    m_SrcSampleRate = sampleRate;
    m_SrcAudioFormat = audioFormat;
    m_SrcChannelCount = channelCount;

    // The file descriptor is owned by the object in Kotlin, so use a duplicate.
    m_TtsSocket = dup(tts_socket);
    m_SourceSocketForDebug = tts_socket;

    // Set it to non-blocking
    int flags = fcntl(m_TtsSocket, F_GETFL, 0);
    fcntl(m_TtsSocket, F_SETFL, flags | O_NONBLOCK);

    // Allocate intermediate buffers
    // Each callback reads ~512 frames at device rate
    m_RawBuf.resize(4096);
    m_SrcBuf.resize(4096);
}

TtsAudioSource::~TtsAudioSource() {
    close(m_TtsSocket);
}

int TtsAudioSource::readPcm(float *outMono, int numFrames) {
    if (m_Finished.load()) {
        return 0;
    }

    // Set up resampler if needed (rate may change via UpdateAudioConfig)
    m_Resampler.setRates(m_SrcSampleRate, deviceSampleRate);

    // How many source frames do we need?
    int srcFramesNeeded = m_Resampler.inputFramesNeeded(numFrames);
    if (srcFramesNeeded > static_cast<int>(m_SrcBuf.size())) {
        m_SrcBuf.resize(srcFramesNeeded);
    }

    // Determine bytes per sample
    int bytesPerSample;
    switch (m_SrcAudioFormat) {
        case 0: bytesPerSample = 1; break;
        default:
        case 1: bytesPerSample = 2; break;
        case 2: bytesPerSample = 4; break;
    }
    int bytesPerFrame = bytesPerSample * m_SrcChannelCount;
    int rawBytesNeeded = srcFramesNeeded * bytesPerFrame;
    if (rawBytesNeeded > static_cast<int>(m_RawBuf.size())) {
        m_RawBuf.resize(rawBytesNeeded);
    }

    // Read raw PCM data from socket
    ssize_t totalBytesRead = 0;
    auto *writePtr = m_RawBuf.data();
    int remaining = rawBytesNeeded;

#define TIMEOUT_READS_WITHOUT_DATA 5

    while (remaining > 0) {
        ssize_t bytesRead = read(m_TtsSocket, writePtr, remaining);
        if (bytesRead == 0) {
            if (totalBytesRead == 0) {
                TRACE("TTS EOF socket %d", m_SourceSocketForDebug);
                m_Finished = true;
                m_pParent->Eof();
                return 0;
            }
            break;
        } else if (bytesRead == -1) {
            ++m_ReadsWithoutData;
            if (m_ReadsWithoutData > TIMEOUT_READS_WITHOUT_DATA) {
                m_Finished = true;
                m_pParent->Eof();
                return 0;
            }
            break;
        }
        m_ReadsWithoutData = 0;
        writePtr += bytesRead;
        remaining -= static_cast<int>(bytesRead);
        totalBytesRead += bytesRead;
    }

    // Convert raw bytes to float32 mono
    int srcFramesRead = static_cast<int>(totalBytesRead / bytesPerFrame);
    for (int i = 0; i < srcFramesRead; i++) {
        float sample = 0.0f;
        for (int ch = 0; ch < m_SrcChannelCount; ch++) {
            const auto *src = m_RawBuf.data() + (i * bytesPerFrame) + (ch * bytesPerSample);
            float chSample = 0.0f;
            if (m_SrcAudioFormat == 0) {
                chSample = (static_cast<float>(src[0]) - 128.0f) / 128.0f;
            } else if (m_SrcAudioFormat == 1) {
                int16_t s;
                memcpy(&s, src, 2);
                chSample = static_cast<float>(s) / 32768.0f;
            } else if (m_SrcAudioFormat == 2) {
                memcpy(&chSample, src, 4);
            }
            sample += chSample;
        }
        m_SrcBuf[i] = sample / static_cast<float>(m_SrcChannelCount);
    }

    // Resample to device rate
    if (m_Resampler.needsResampling() && srcFramesRead > 0) {
        int consumed;
        int outFrames = m_Resampler.process(m_SrcBuf.data(), srcFramesRead,
                                             outMono, numFrames, consumed);
        // Zero-fill remainder
        if (outFrames < numFrames) {
            memset(outMono + outFrames, 0, (numFrames - outFrames) * sizeof(float));
        }
        return outFrames > 0 ? numFrames : 0;
    } else {
        // No resampling - direct copy
        int toCopy = (srcFramesRead < numFrames) ? srcFramesRead : numFrames;
        memcpy(outMono, m_SrcBuf.data(), toCopy * sizeof(float));
        if (toCopy < numFrames) {
            memset(outMono + toCopy, 0, (numFrames - toCopy) * sizeof(float));
        }
        return toCopy > 0 ? numFrames : 0;
    }
}

bool TtsAudioSource::isFinished() const {
    return m_Finished.load();
}

//
// EarconSource
//
EarconSource::EarconSource(PositionedAudio *parent, std::string &asset,
                           AAssetManager *mgr, int targetSampleRate)
        : BeaconAudioSource(parent, 0.0) {
    m_Decoder = std::make_unique<WavDecoder>(mgr, asset, targetSampleRate);
    if (!m_Decoder->isValid()) {
        TRACE("EarconSource: failed to load %s", asset.c_str());
    }
}

int EarconSource::readPcm(float *outMono, int numFrames) {
    if (!m_Decoder || !m_Decoder->isValid()) {
        return 0;
    }

    int totalFrames = m_Decoder->numFrames();
    int remaining = totalFrames - static_cast<int>(m_FramePos);
    if (remaining <= 0) {
        return 0;
    }

    int toRead = (numFrames < remaining) ? numFrames : remaining;
    memcpy(outMono, m_Decoder->data() + m_FramePos, toRead * sizeof(float));
    m_FramePos += toRead;

    // Pad with silence if needed
    if (toRead < numFrames) {
        memset(outMono + toRead, 0, (numFrames - toRead) * sizeof(float));
    }

    return numFrames;
}

bool EarconSource::isFinished() const {
    if (!m_Decoder || !m_Decoder->isValid()) return true;
    return static_cast<int>(m_FramePos) >= m_Decoder->numFrames();
}

void EarconSource::UpdateGeometry(double degrees_off_axis,
                                   BeaconAudioSource::SourceMode mode) {
    if (isFinished()) {
        m_pParent->Eof();
    }
}
