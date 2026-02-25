#include <string>
#include <utility>
#include <jni.h>
#include <cmath>

#include "GeoUtils.h"
#include "Trace.h"
#include "AudioBeacon.h"
#include "AudioEngine.h"
using namespace soundscape;

PositionedAudio::PositionedAudio(AudioEngine *engine,
                                 PositioningMode mode,
                                 bool dimmable,
                                 std::string utterance_id)
                : m_Mode(mode),
                  m_Eof(false),
                  m_Dimmable(dimmable)
{
    m_pEngine = engine;
    m_UtteranceId = std::move(utterance_id);
}

PositionedAudio::~PositionedAudio() {
    // Unregister from mixer
    if (m_pAudioSource) {
        auto *mixer = m_pEngine->GetMixer();
        if (mixer) {
            mixer->removeSource(m_pAudioSource.get());
        }
    }
    m_pEngine->RemoveBeacon(this);
}

void PositionedAudio::UpdateAzimuth(double heading, double latitude, double longitude) {
    if (m_Mode.m_AudioType == PositioningMode::RELATIVE) {
        m_pAudioSource->azimuth.store(static_cast<float>(toRadians(m_Mode.m_Heading)));
    } else if (m_Mode.m_AudioType == PositioningMode::COMPASS) {
        if (!isnan(heading))
            m_pAudioSource->azimuth.store(static_cast<float>(toRadians(m_Mode.m_Heading - heading)));
    } else if (m_Mode.m_AudioType == PositioningMode::LOCALIZED) {
        if (!isnan(heading) && !isnan(m_Mode.m_Latitude) && !isnan(m_Mode.m_Longitude)) {
            auto bearing = bearingFromTwoPoints(m_Mode.m_Latitude, m_Mode.m_Longitude,
                                               latitude, longitude);
            m_pAudioSource->azimuth.store(static_cast<float>(toRadians(bearing - heading)));
        }
    }
}

void PositionedAudio::RegisterWithMixer() {
    if (!m_pAudioSource)
        return;

    auto *mixer = m_pEngine->GetMixer();
    if (!mixer)
        return;

    m_pAudioSource->category = m_Dimmable ? AudioCategory::BEACON : AudioCategory::SPEECH;
    m_pAudioSource->needsSpatialize = (m_Mode.m_AudioType != PositioningMode::STANDARD);

    double heading, latitude, longitude;
    m_pEngine->GetListenerPosition(heading, latitude, longitude);
    UpdateAzimuth(heading, latitude, longitude);

    mixer->addSource(m_pAudioSource.get());
}

void PositionedAudio::Init(double degrees_off_axis,
                           bool proximityBeacon,
                           int sampleRate,
                           int audioFormat,
                           int channelCount)
{
    bool queued = CreateAudioSource(degrees_off_axis,
                                    sampleRate,
                                    audioFormat,
                                    channelCount,
                                    proximityBeacon);

    if(!queued)
        RegisterWithMixer();

    m_pEngine->AddBeacon(this, queued);
}

void PositionedAudio::PlayNow()
{
    RegisterWithMixer();
}

double PositionedAudio::GetHeadingOffset(double heading, double latitude, double longitude) const {
    auto beacon_heading = bearingFromTwoPoints(m_Mode.m_Latitude, m_Mode.m_Longitude, latitude, longitude);
    auto degrees_off_axis = beacon_heading - heading;
    if (degrees_off_axis > 180)
        degrees_off_axis -= 360;
    else if (degrees_off_axis < -180)
        degrees_off_axis += 360;

    return degrees_off_axis;
}

void PositionedAudio::UpdateGeometry(double listenerLatitude, double listenerLongitude,
                                     double heading, double latitude, double longitude,
                                     double proximityNear) {
    BeaconAudioSource::SourceMode mode = BeaconAudioSource::DIRECTION_MODE;

    if(m_Mode.m_AudioMode == PositioningMode::PROXIMITY) {
        auto d = distance(listenerLatitude, listenerLongitude, m_Mode.m_Latitude,
                          m_Mode.m_Longitude);
        if (d < proximityNear) {
            mode = BeaconAudioSource::NEAR_MODE;
        } else if (d < (2 * proximityNear)) {
            mode = BeaconAudioSource::FAR_MODE;
        } else {
            mode = BeaconAudioSource::TOO_FAR_MODE;
        }
    }

    double degrees_off_axis;
    if(isnan(heading)) {
        degrees_off_axis = m_Dimmable ? 180.0 : 0.0;
    } else {
        degrees_off_axis = GetHeadingOffset(heading, latitude, longitude);
    }

    if(m_pAudioSource) {
        m_pAudioSource->UpdateGeometry(degrees_off_axis, mode);
        if (m_Mode.m_AudioType != PositioningMode::STANDARD)
            UpdateAzimuth(heading, latitude, longitude);
    }
}

void PositionedAudio::Mute(bool mute) {
    if (m_pAudioSource) {
        m_pAudioSource->muted.store(mute);
    }
}

void PositionedAudio::UpdateAudioConfig(int sample_rate, int audio_format, int channel_count)
{
    if(m_pAudioSource) {
        m_pAudioSource->UpdateAudioConfig(sample_rate, audio_format, channel_count);
    }
    m_AudioConfigured = true;
}

//
// Beacon
//
Beacon::Beacon(AudioEngine *engine, PositioningMode mode)
    : PositionedAudio(engine, mode, true)
{
    double listener_heading;
    double listener_latitude;
    double listener_longitude;
    engine->GetListenerPosition(listener_heading, listener_latitude, listener_longitude);

    auto degrees_off_axis = GetHeadingOffset(listener_heading, listener_latitude, listener_longitude);
    Init(degrees_off_axis, mode.m_AudioMode == PositioningMode::PROXIMITY);
}

bool Beacon::CreateAudioSource(double degrees_off_axis,
                               int sampleRate,
                               int audioFormat,
                               int channelCount,
                               bool proximityBeacon)
{
    auto *mgr = m_pEngine->GetAssetManager();
    int targetRate = m_pEngine->GetMixer() ? m_pEngine->GetMixer()->getSampleRate() : 48000;

    m_pAudioSource = std::make_unique<BeaconBufferGroup>(mgr,
                                                          proximityBeacon ?
                                                              &msc_ProximityDescriptor : m_pEngine->GetBeaconDescriptor(),
                                                          this,
                                                          degrees_off_axis,
                                                          targetRate);
    m_pAudioSource->isProximityBeacon = proximityBeacon;
    m_pAudioSource->UpdateGeometry(0.0, BeaconAudioSource::TOO_FAR_MODE);
    // Not queued
    return false;
}

//
// TextToSpeech
//
TextToSpeech::TextToSpeech(AudioEngine *engine,
                           PositioningMode mode,
                           int tts_socket,
                           std::string &utterance_id)
        : m_TtsSocket(tts_socket),
          PositionedAudio(engine, mode, false, utterance_id)
{
    Init(0.0);
}

bool TextToSpeech::CreateAudioSource(double degrees_off_axis,
                                      int sampleRate,
                                      int audioFormat,
                                      int channelCount,
                                      bool proximityBeacon)
{
    m_pAudioSource = std::make_unique<TtsAudioSource>(this,
                                                       m_TtsSocket,
                                                       sampleRate,
                                                       audioFormat,
                                                       channelCount);
    // Text to speech audio are queued to play one after the other
    return true;
}

//
// Earcon
//
Earcon::Earcon(AudioEngine *engine,
               std::string asset,
               PositioningMode mode)
        : PositionedAudio(engine, mode),
          m_Asset(std::move(asset))
{
    Init(0.0);
}

bool Earcon::CreateAudioSource(double degrees_off_axis,
                                int sampleRate,
                                int audioFormat,
                                int channelCount,
                                bool proximityBeacon)
{
    auto *mgr = m_pEngine->GetAssetManager();
    int targetRate = m_pEngine->GetMixer() ? m_pEngine->GetMixer()->getSampleRate() : 48000;

    m_pAudioSource = std::make_unique<EarconSource>(this, m_Asset, mgr, targetRate);
    // Earcons are queued along with the TextToSpeech audio
    return true;
}
