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

unsigned int BeaconBuffer::Read(void *data, unsigned int data_length, unsigned long pos) {
    unsigned int remainder = 0;
    auto *dest =(unsigned char *)data;
    pos %= m_BufferSize;
    if((m_BufferSize - pos) < data_length) {
        remainder = data_length - (m_BufferSize - pos);
        data_length = m_BufferSize - pos;
    }
    memcpy(dest, m_pBuffer.get() + pos, data_length);
    if(remainder)
        memcpy(dest + data_length, m_pBuffer.get() + pos + data_length, remainder);

    return data_length;
}

//
//
//
BeaconBufferGroup::BeaconBufferGroup(const AudioEngine *ae, PositionedAudio *parent, double degrees_off_axis)
: BeaconAudioSource(parent, degrees_off_axis)
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
}

BeaconBufferGroup::~BeaconBufferGroup()
{
    TRACE("~BeaconBufferGroup %p", this);
}

void BeaconBufferGroup::CreateSound(FMOD::System *system, FMOD::Sound **sound)
{
    TRACE("BeaconBufferGroup CreateSound %p", this);

    FMOD_CREATESOUNDEXINFO extra_info;
    memset(&extra_info, 0, sizeof(FMOD_CREATESOUNDEXINFO));
    extra_info.cbsize = sizeof(FMOD_CREATESOUNDEXINFO);  /* Required. */
    extra_info.numchannels = 1;
    extra_info.defaultfrequency = 44100;
    extra_info.length = m_pBuffers[0]->GetBufferSize();                         /* Length of PCM data in bytes of whole song (for Sound::getLength) */
    extra_info.decodebuffersize = extra_info.length / (2 * m_pDescription->m_BeatsInPhrase);       /* Chunk size of stream update in samples. This will be the amount of data passed to the user callback. */
    extra_info.format = FMOD_SOUND_FORMAT_PCM16;                    /* Data format of sound. */
    extra_info.pcmreadcallback = StaticPcmReadCallback;             /* User callback for reading. */
    extra_info.userdata = this;

    auto result = system->createSound(nullptr,
                                      FMOD_OPENUSER | FMOD_LOOP_NORMAL | FMOD_3D |
                                      FMOD_CREATESTREAM,
                                      &extra_info,
                                      sound);
    ERROR_CHECK(result);
}

void BeaconBufferGroup::UpdateCurrentBufferFromHeading()
{
    for(const auto &buffer: m_pBuffers)
    {
        if(buffer->CheckIsActive(m_DegreesOffAxis)) {
            m_pCurrentBuffer = buffer.get();
            break;
        }
    }
    if(m_pCurrentBuffer == nullptr)
        m_pCurrentBuffer = m_pBuffers[0].get();
}

FMOD_RESULT F_CALLBACK BeaconBufferGroup::PcmReadCallback(void *data, unsigned int data_length)
{
    UpdateCurrentBufferFromHeading();

    unsigned int bytes_read = m_pCurrentBuffer->Read(data, data_length, m_BytePos);
    m_BytePos += bytes_read;
    //TRACE("BBG callback %d: %u @ %lu", m_CurrentBuffer, bytes_read, m_BytePos);

    return FMOD_OK;
}

//
//
//

TtsAudioSource::TtsAudioSource(PositionedAudio *parent, int tts_socket)
              : BeaconAudioSource(parent, 0)

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

void TtsAudioSource::CreateSound(FMOD::System *system, FMOD::Sound **sound)
{
    FMOD_CREATESOUNDEXINFO extra_info;

    memset(&extra_info, 0, sizeof(FMOD_CREATESOUNDEXINFO));
    extra_info.cbsize = sizeof(FMOD_CREATESOUNDEXINFO);

    extra_info.numchannels = 1;
    extra_info.defaultfrequency = 22050;
    extra_info.length = extra_info.defaultfrequency;
    extra_info.decodebuffersize = extra_info.defaultfrequency / 10;

    extra_info.format = FMOD_SOUND_FORMAT_PCM16;                    /* Data format of sound. */
    extra_info.pcmreadcallback = StaticPcmReadCallback;             /* User callback for reading. */
    extra_info.userdata = this;

    auto result = system->createSound(nullptr,
                                      FMOD_OPENUSER | FMOD_LOOP_OFF | FMOD_3D |
                                      FMOD_CREATESTREAM,
                                      &extra_info,
                                      sound);
    ERROR_CHECK(result);

}
FMOD_RESULT F_CALLBACK TtsAudioSource::PcmReadCallback(void *data, unsigned int data_length)
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
FMOD_RESULT F_CALLBACK BeaconAudioSource::StaticPcmReadCallback(FMOD_SOUND* sound, void *data, unsigned int data_length) {
    BeaconAudioSource *bg;
    ((FMOD::Sound*)sound)->getUserData((void **)&bg);
    bg->PcmReadCallback(data, data_length);

    return FMOD_OK;
}

void BeaconAudioSource::UpdateGeometry(double degrees_off_axis)
{
    m_DegreesOffAxis = degrees_off_axis;
}

//
//
//
EarconSource::EarconSource(PositionedAudio *parent, std::string &asset)
        : BeaconAudioSource(parent, 0.0),
          m_Asset(asset)
{
}


EarconSource::~EarconSource()
{
    m_pSound->release();
}

void EarconSource::CreateSound(FMOD::System *system, FMOD::Sound **sound)
{
    auto result = system->createSound(m_Asset.c_str(), FMOD_DEFAULT, nullptr, &m_pSound);
    ERROR_CHECK(result);
    system->playSound(m_pSound, nullptr, false, nullptr);
}

void EarconSource::UpdateGeometry(double degrees_off_axis)
{
    FMOD_OPENSTATE state;
    m_pSound->getOpenState(&state, nullptr, nullptr, nullptr);
    if(state == FMOD_OPENSTATE_READY)
        m_pParent->Eof();
}
