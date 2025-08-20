#include <string>
#include <utility>
#include <jni.h>

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
    m_pSystem = engine->GetFmodSystem();
    m_UtteranceId =  std::move(utterance_id);
}

PositionedAudio::~PositionedAudio() {
//    TRACE("%s %p", __FUNCTION__, this);
    m_pEngine->RemoveBeacon(this);

    if(m_pSound) {
        auto result = m_pSound->release();
        ERROR_CHECK(result);
    }

//    TRACE("%s %p done", __FUNCTION__, this);
}

void PositionedAudio::InitFmodSound() {
    FMOD_RESULT result;

    m_pAudioSource->CreateSound(m_pSystem, &m_pSound, m_Mode);
    if(!m_pSound)
        return;

    double heading, current_latitude, current_longitude;
    m_pEngine->GetListenerPosition(heading, current_latitude, current_longitude);

    result = m_pSound->set3DMinMaxDistance(10.0f * FMOD_DISTANCE_FACTOR,
                                           5000.0f * FMOD_DISTANCE_FACTOR);
    ERROR_CHECK(result);

    {
        // Create paused sound channel using appropriate channel group
        FMOD::ChannelGroup *channelGroup;
        if(m_Dimmable)
            channelGroup = m_pEngine->GetBeaconGroup();
        else
            channelGroup = m_pEngine->GetSpeechGroup();

        result = m_pSystem->playSound(m_pSound, channelGroup, true, &m_pChannel);
        ERROR_CHECK(result);

        switch(m_Mode.m_Type) {
            default:
            case PositioningMode::STANDARD:
                break;
            case PositioningMode::LOCALIZED:
                if(!isnan(m_Mode.m_Latitude) && !isnan(m_Mode.m_Longitude)) {
                    // Only set the 3D position if the latitude and longitude are valid
                    FMOD_VECTOR pos =m_pEngine->TranslateToFmodVector(m_Mode.m_Longitude, m_Mode.m_Latitude);
                    FMOD_VECTOR vel = {0.0f, 0.0f, 0.0f};
                    result = m_pChannel->set3DAttributes(&pos, &vel);
                }
                ERROR_CHECK(result);
                break;
            case PositioningMode::RELATIVE: {
                // The position is relative, so use the heading
                auto radians = toRadians(m_Mode.m_Heading);
                auto pos = FMOD_VECTOR{(float)sin(radians), 0.0f, (float)cos(radians)};
                FMOD_VECTOR vel = {0.0f, 0.0f, 0.0f};
                result = m_pChannel->set3DAttributes(&pos, &vel);
                ERROR_CHECK(result);
                break;
            }
            case PositioningMode::COMPASS: {
                // Make up a position using the current position and the heading
                double lat, lon;
                getDestinationCoordinate(current_latitude, current_longitude, m_Mode.m_Heading, &lat, &lon);

                FMOD_VECTOR pos =m_pEngine->TranslateToFmodVector(lon, lat);
                FMOD_VECTOR vel = {0.0f, 0.0f, 0.0f};
                result = m_pChannel->set3DAttributes(&pos, &vel);
                ERROR_CHECK(result);
                break;
            }
        }

        // Start the channel playing
        result = m_pChannel->setPaused(false);
        ERROR_CHECK(result);
    }
}

void PositionedAudio::Init(double degrees_off_axis,
                           int sampleRate,
                           int audioFormat,
                           int channelCount)
{
    bool queued = CreateAudioSource(degrees_off_axis,
                                    sampleRate,
                                    audioFormat,
                                    channelCount);

    //TRACE("%s %p", __FUNCTION__, this);

    if(!queued)
        InitFmodSound();

    m_pEngine->AddBeacon(this, queued);
}

void PositionedAudio::PlayNow()
{
    InitFmodSound();
}

double PositionedAudio::GetHeadingOffset(double heading, double latitude, double longitude) const {
    // Calculate how far off axis the beacon is given this new heading

    // Calculate the beacon heading
    auto beacon_heading = bearingFromTwoPoints(m_Mode.m_Latitude, m_Mode.m_Longitude, latitude, longitude);
    auto degrees_off_axis = beacon_heading - heading;
    if (degrees_off_axis > 180)
        degrees_off_axis -= 360;
    else if (degrees_off_axis < -180)
        degrees_off_axis += 360;

    return degrees_off_axis;
}
void PositionedAudio::UpdateGeometry(double heading, double latitude, double longitude) {
    if(isnan(heading)) {
        // If dimmable, the audio is placed behind us if there's no heading
        m_pAudioSource->UpdateGeometry(m_Dimmable ? 180.0 : 0.0);
    } else {
        auto degrees_off_axis = GetHeadingOffset(heading, latitude, longitude);
        m_pAudioSource->UpdateGeometry(degrees_off_axis);
    }
    //TRACE("%f %f -> %f (%f %f), %dm", heading, beacon_heading, degrees_off_axis, lat_delta, long_delta, dist)
}

void PositionedAudio::Mute(bool mute) {
    m_pChannel->setMute(mute);
}

void PositionedAudio::UpdateAudioConfig(int sample_rate, int audio_format, int channel_count)
{
    m_pAudioSource->UpdateAudioConfig(sample_rate, audio_format, channel_count);
    m_AudioConfigured = true;
}
