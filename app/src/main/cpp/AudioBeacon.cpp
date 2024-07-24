#include <string>
#include <jni.h>

#include "GeoUtils.h"
#include "Trace.h"
#include "AudioBeacon.h"
#include "AudioEngine.h"
using namespace soundscape;

PositionedAudio::PositionedAudio(AudioEngine *engine,
                                 double latitude, double longitude)
                :m_Eof(false)
{
    m_Latitude = latitude;
    m_Longitude = longitude;

    m_pEngine = engine;
    m_pSystem = engine->GetFmodSystem();
}

PositionedAudio::~PositionedAudio() {
    TRACE("%s %p", __FUNCTION__, this);
    m_pEngine->RemoveBeacon(this);

    if(m_pSound) {
        auto result = m_pSound->release();
        ERROR_CHECK(result);
    }

    TRACE("%s %p done", __FUNCTION__, this);
}

void PositionedAudio::InitFmodSound() {
    FMOD_RESULT result;

    m_pAudioSource->CreateSound(m_pSystem, &m_pSound);

    result = m_pSound->set3DMinMaxDistance(10.0f * FMOD_DISTANCE_FACTOR,
                                           5000.0f * FMOD_DISTANCE_FACTOR);
    ERROR_CHECK(result);

    result = m_pSound->setMode(FMOD_LOOP_NORMAL);
    ERROR_CHECK(result);

    {
        FMOD_VECTOR pos = {(float) m_Longitude, 0.0f, (float) m_Latitude};
        FMOD_VECTOR vel = {0.0f, 0.0f, 0.0f};

        result = m_pSystem->playSound(m_pSound, nullptr, false, &m_pChannel);
        ERROR_CHECK(result);

        result = m_pChannel->set3DAttributes(&pos, &vel);
        ERROR_CHECK(result);
    }
}

void PositionedAudio::Init()
{
    bool queued = CreateAudioSource();

    TRACE("%s %p", __FUNCTION__, this);

    if(!queued)
        InitFmodSound();

    m_pEngine->AddBeacon(this, queued);
}

void PositionedAudio::PlayNow()
{
    InitFmodSound();
}

void PositionedAudio::UpdateGeometry(double heading, double latitude, double longitude) {
    // Calculate how far off axis the beacon is given this new heading

    // Calculate the beacon heading
    auto beacon_heading = bearingFromTwoPoints(m_Latitude, m_Longitude, latitude, longitude);
    auto degrees_off_axis = beacon_heading - heading;
    if(degrees_off_axis > 180)
        degrees_off_axis -= 360;
    else if(degrees_off_axis < -180)
        degrees_off_axis += 360;

    int dist = (int)distance(latitude, longitude, m_Latitude, m_Longitude);
    m_pAudioSource->UpdateGeometry(degrees_off_axis, dist);

    //TRACE("%f %f -> %f (%f %f), %dm", heading, beacon_heading, degrees_off_axis, lat_delta, long_delta, dist)
}
