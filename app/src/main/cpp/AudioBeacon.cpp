#include <string>
#include <jni.h>

#include "GeoUtils.h"
#include "Trace.h"
#include "AudioBeacon.h"
#include "AudioEngine.h"
using namespace soundscape;

PositionedAudio::PositionedAudio(AudioEngine *engine, PositioningMode mode)
                : m_Mode(mode),
                  m_Eof(false)
{
    m_pEngine = engine;
    m_pSystem = engine->GetFmodSystem();
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

    result = m_pSound->set3DMinMaxDistance(10.0f * FMOD_DISTANCE_FACTOR,
                                           5000.0f * FMOD_DISTANCE_FACTOR);
    ERROR_CHECK(result);

    {
        // Create paused sound channel
        result = m_pSystem->playSound(m_pSound, nullptr, true, &m_pChannel);
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
                double heading, current_latitude, current_longitude;
                m_pEngine->GetListenerPosition(heading, current_latitude, current_longitude);
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

void PositionedAudio::Init(double degrees_off_axis)
{
    bool queued = CreateAudioSource(degrees_off_axis);

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
        // We don't currently have a heading, so we want to make the beacon more quiet and play
        // the sound it makes if it's behind us
        m_pChannel->setVolume(0.1);
        m_pAudioSource->UpdateGeometry(180.0);
    } else {
        m_pChannel->setVolume(1.0);
        auto degrees_off_axis = GetHeadingOffset(heading, latitude, longitude);
        m_pAudioSource->UpdateGeometry(degrees_off_axis);
    }


    //TRACE("%f %f -> %f (%f %f), %dm", heading, beacon_heading, degrees_off_axis, lat_delta, long_delta, dist)
}

void PositionedAudio::Mute(bool mute) {
    m_pChannel->setMute(mute);
}
