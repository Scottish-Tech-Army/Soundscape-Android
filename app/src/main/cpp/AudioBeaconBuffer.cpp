#include <unistd.h>
#include <thread>
#include <filesystem>
#include <cassert>
#include <android/log.h>
#include <fcntl.h>
#include "GeoUtils.h"
#include "Trace.h"
#include <cmath>
#include <jni.h>
#include "AudioBeaconBuffer.h"
#include "BeaconDescriptor.h"
#include "AudioBeacon.h"

using namespace soundscape;

FMOD_SOUND_FORMAT AudioFormatToFmodFormat(int audioFormat) {
    switch (audioFormat) {
        case 0:
            return FMOD_SOUND_FORMAT_PCM8;
        default:
        case 1:
            return FMOD_SOUND_FORMAT_PCM16;
        case 2:
            return FMOD_SOUND_FORMAT_PCMFLOAT;
    }
}

BeaconBuffer::BeaconBuffer(FMOD::System *system, const std::string &filename, double max_angle)
            : m_MaxAngle(max_angle),
              m_Name(filename)
{
        FMOD::Sound* sound;

        auto result = system->createSound(filename.c_str(), FMOD_DEFAULT | FMOD_OPENONLY, nullptr, &sound);
        ERROR_CHECK(result);

        result = sound->getLength(&m_BufferSize, FMOD_TIMEUNIT_RAWBYTES);
        ERROR_CHECK(result);

        m_pBuffer = std::make_unique<unsigned char[]>(m_BufferSize);

        unsigned int bytes_read;
        result = sound->readData(m_pBuffer.get(), m_BufferSize, &bytes_read);
        ERROR_CHECK(result);

        result = sound->release();
        ERROR_CHECK(result);
}

BeaconBuffer::~BeaconBuffer() {
    TRACE("~BeaconBuffer");
}

bool BeaconBuffer::CheckIsActive(double degrees_off_axis) const
{
    if(fabs(degrees_off_axis) <= m_MaxAngle)
        return true;

    return false;
}

unsigned int BeaconBuffer::Read(void *data, unsigned int data_length, unsigned long pos, bool pad_with_silence) {
    unsigned int remainder = 0;
    auto *dest =(unsigned char *)data;
    pos %= m_BufferSize;
    if((m_BufferSize - pos) < data_length) {
        remainder = data_length - (m_BufferSize - pos);
        data_length = m_BufferSize - pos;
    }
    memcpy(dest, m_pBuffer.get() + pos, data_length);
    if(remainder) {
        if(pad_with_silence) {
            // Set the remainder of the buffer to 0 (silence)
            memset(dest + data_length, 0, remainder);
        } else {
            // Loop and copy from the start of the buffer
            memcpy(dest + data_length, m_pBuffer.get(), remainder);
        }
    }

    return data_length;
}

//
//
//
BeaconBufferGroup::BeaconBufferGroup(const AudioEngine *ae,
                                     PositionedAudio *parent,
                                     double degrees_off_axis,
                                     int sample_rate,
                                     int audio_format,
                                     int channel_count)
: BeaconAudioSource(parent,
                    degrees_off_axis,
                    sample_rate,
                    audio_format,
                    channel_count)
{
    TRACE("Create BeaconBufferGroup %p", this);
    m_pDescription = ae->GetBeaconDescriptor();

    auto system = ae->GetFmodSystem();
    for(const auto &asset: m_pDescription->m_Beacons) {
        auto buffer = std::make_unique<BeaconBuffer>(system,
                                                                  asset.m_Filename,
                                                                 asset.m_MaxAngle);
        m_pBuffers.push_back(std::move(buffer));
    }

    m_pIntro = std::make_unique<BeaconBuffer>(system,
                                              "file:///android_asset/Route/Route_Start.wav",
                                              180.0);
    m_pOutro = std::make_unique<BeaconBuffer>(system,
                                              "file:///android_asset/Route/Route_End.wav",
                                              180.0);
    m_pNear = std::make_unique<BeaconBuffer>(system,
                                              "file:///android_asset/Route/Proximity_Close.wav",
                                              180.0);
    m_pFar = std::make_unique<BeaconBuffer>(system,
                                              "file:///android_asset/Route/Proximity_Far.wav",
                                              180.0);
}

BeaconBufferGroup::~BeaconBufferGroup()
{
    TRACE("~BeaconBufferGroup %p", this);
}

void BeaconBufferGroup::Stop() {
    // TODO: Call this to play the outro and then stop.
    m_PlayState = PLAYING_OUTRO;
}

void BeaconBufferGroup::CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode)
{
    TRACE("BeaconBufferGroup CreateSound %p", this);

    FMOD_CREATESOUNDEXINFO extra_info;
    memset(&extra_info, 0, sizeof(FMOD_CREATESOUNDEXINFO));
    extra_info.cbsize = sizeof(FMOD_CREATESOUNDEXINFO);  /* Required. */
    extra_info.numchannels = m_ChannelCount;
    extra_info.defaultfrequency = m_SampleRate;
    extra_info.length = m_pBuffers[0]->GetBufferSize();                         /* Length of PCM data in bytes of whole song (for Sound::getLength) */
    extra_info.decodebuffersize = extra_info.length / (2 * m_pDescription->m_BeatsInPhrase);       /* Chunk size of stream update in samples. This will be the amount of data passed to the user callback. */
    extra_info.format = AudioFormatToFmodFormat(m_AudioFormat);
    extra_info.pcmreadcallback = StaticPcmReadCallback;             /* User callback for reading. */
    extra_info.userdata = this;

    auto result = system->createSound(nullptr,
                                      FMOD_OPENUSER | FMOD_LOOP_NORMAL | mode.Get3DFlags() |
                                      FMOD_CREATESTREAM,
                                      &extra_info,
                                      sound);
    ERROR_CHECK(result);
}

void BeaconBufferGroup::UpdateCurrentBufferFromHeadingAndLocation()
{
    if(m_PlayState == PLAYING_INTRO) {
        m_pCurrentBuffer = m_pIntro.get();
        return;
    } else if (m_PlayState == PLAYING_OUTRO) {
        m_pCurrentBuffer = m_pOutro.get();
        return;
    }

    switch(m_Mode) {
        case BeaconAudioSource::DIRECTION_MODE: {
            for (const auto &buffer: m_pBuffers) {
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
            m_pCurrentBuffer = m_pNear.get();
            break;
        }
        case BeaconAudioSource::FAR_MODE: {
            m_pCurrentBuffer = m_pFar.get();
            break;
        }
    }
}

FMOD_RESULT F_CALL BeaconBufferGroup::PcmReadCallback(void *data, unsigned int data_length)
{
    if(m_PlayState == PLAYING_COMPLETE) {
        return FMOD_ERR_FILE_EOF;
    }
    UpdateCurrentBufferFromHeadingAndLocation();

    unsigned int bytes_read = m_pCurrentBuffer->Read(data, data_length, m_BytePos, m_PlayState != PLAYING_BEACON);
    m_BytePos += bytes_read;
    if(m_PlayState == PLAYING_INTRO) {
        if(m_BytePos >= m_pIntro->GetBufferSize()) {
            m_PlayState = PLAYING_BEACON;
            m_BytePos = 0;
        }
    } else if(m_PlayState == PLAYING_OUTRO) {
        if(m_BytePos >= m_pIntro->GetBufferSize()) {
            m_PlayState = PLAYING_COMPLETE;
        }
    }
    // TRACE("BBG callback %p: %u @ %lu into %u", m_pCurrentBuffer, bytes_read, m_BytePos, data_length);

    return FMOD_OK;
}

//
//
//

TtsAudioSource::TtsAudioSource(PositionedAudio *parent,
                               int tts_socket,
                               int sampleRate, int audioFormat, int channelCount)
              : BeaconAudioSource(parent, 0, sampleRate, audioFormat, channelCount)

{
    // The file descriptor is owned by the object in Kotlin, so use a duplicate.
    m_TtsSocket = dup(tts_socket);
    m_SourceSocketForDebug = tts_socket;

    // Set it to non-blocking
    int flags = fcntl(m_TtsSocket, F_GETFL, 0);
    fcntl(m_TtsSocket, F_SETFL, flags | O_NONBLOCK);
}

TtsAudioSource::~TtsAudioSource()
{
    //TRACE("~TtsAudioSource close socket %d", m_SourceSocketForDebug);
    close(m_TtsSocket);
}

void TtsAudioSource::CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode)
{
    FMOD_CREATESOUNDEXINFO extra_info;

    memset(&extra_info, 0, sizeof(FMOD_CREATESOUNDEXINFO));
    extra_info.cbsize = sizeof(FMOD_CREATESOUNDEXINFO);

    extra_info.numchannels = m_ChannelCount;
    extra_info.defaultfrequency = m_SampleRate;
    extra_info.length = extra_info.defaultfrequency;
    extra_info.decodebuffersize = extra_info.defaultfrequency / 10;
    extra_info.format = AudioFormatToFmodFormat(m_AudioFormat);
    extra_info.pcmreadcallback = StaticPcmReadCallback;             /* User callback for reading. */
    extra_info.userdata = this;

    auto result = system->createSound(nullptr,
                                      FMOD_OPENUSER | FMOD_LOOP_NORMAL | mode.Get3DFlags() |
                                      FMOD_CREATESTREAM,
                                      &extra_info,
                                      sound);
    ERROR_CHECK(result);

}
FMOD_RESULT F_CALL TtsAudioSource::PcmReadCallback(void *data, unsigned int data_length)
{
    // The text to speech data is sent over a socket from Kotlin. The socket is closed on the
    // Kotlin end when the speech has been fully synthesised. However, the onDone appears to be
    // unreliable and so we also timeout if no data is read after TIMEOUT_READS_WITHOUT_DATA calls.
    // Each read is reading 100ms (decodebuffersize set in CreateSound), so timeout after 500ms.
#define TIMEOUT_READS_WITHOUT_DATA 5

    ssize_t total_bytes_read = 0;
    ssize_t bytes_read;
    auto write_ptr = (unsigned char *)data;
    while(data_length > 0) {
        bytes_read = read(m_TtsSocket, write_ptr, data_length);
        //TRACE("%d: read %zd/%zd/%u heading %f", m_SourceSocketForDebug,
        //                                        bytes_read,
        //                                        total_bytes_read,
        //                                        data_length,
        //                                        m_DegreesOffAxis.load());
        if(bytes_read == 0) {
            if(total_bytes_read == 0) {
                TRACE("TTS EOF socket %d", m_SourceSocketForDebug);
                m_pParent->Eof();
                return FMOD_ERR_FILE_EOF;
            }
            break;
        }
        else if(bytes_read == -1) {
            // No data - socket is non-blocking
            ++m_ReadsWithoutData;
            //TRACE("m_ReadsWithoutData %d (%d bytes)", m_ReadsWithoutData, data_length);
            if(m_ReadsWithoutData > TIMEOUT_READS_WITHOUT_DATA) {
                //TRACE("TTS Timed out socket %d", m_SourceSocketForDebug);
                m_pParent->Eof();
                return FMOD_ERR_FILE_EOF;
            }
            break;
        }
        m_ReadsWithoutData = 0;

        write_ptr += bytes_read;
        data_length -= bytes_read;
        total_bytes_read += bytes_read;
    }

    //TRACE("TTS callback on socket %d %zd/%u", m_SourceSocketForDebug, total_bytes_read, data_length);
    memset(write_ptr, 0, data_length);

    return FMOD_OK;
}


//
//
//
FMOD_RESULT F_CALL BeaconAudioSource::StaticPcmReadCallback(FMOD_SOUND* sound, void *data, unsigned int data_length) {
    BeaconAudioSource *bg;
    ((FMOD::Sound*)sound)->getUserData((void **)&bg);
    bg->PcmReadCallback(data, data_length);

    return FMOD_OK;
}

void BeaconAudioSource::UpdateGeometry(double degrees_off_axis, BeaconAudioSource::SourceMode mode)
{
    m_DegreesOffAxis = degrees_off_axis;
    m_Mode = mode;
}

//
//
//
EarconSource::EarconSource(PositionedAudio *parent, std::string &asset)
        : BeaconAudioSource(parent, 0.0, -1, -1 , -1),
          m_Asset(asset)
{
}

void EarconSource::CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode) {
    auto result = system->createSound(
            m_Asset.c_str(),
            FMOD_DEFAULT | mode.Get3DFlags(),
            nullptr,
            sound);
    ERROR_CHECK(result);
    // Remember sound for checking for completion
    m_pSound = *sound;
}

void EarconSource::UpdateGeometry(double degrees_off_axis, BeaconAudioSource::SourceMode mode)
{
    if(m_pSound != nullptr) {
        FMOD_OPENSTATE state;
        m_pSound->getOpenState(&state, nullptr, nullptr, nullptr);
        if (state == FMOD_OPENSTATE_READY)
            m_pParent->Eof();
    }
}
