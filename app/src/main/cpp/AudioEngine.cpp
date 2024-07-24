#include "AudioEngine.h"
#include "AudioBeacon.h"
#include "GeoUtils.h"
#include "Trace.h"

#include <thread>
#include <memory>
#include <mutex>
#include <android/log.h>
#include <jni.h>

namespace soundscape {

const BeaconDescriptor AudioEngine::msc_BeaconDescriptors[] =
{
        {
                2,
                {
                        {"file:///android_asset/Classic/Classic_OnAxis.wav", 22.5},
                        {"file:///android_asset/Classic/Classic_OffAxis.wav", 180.0},
                }
        },
        {
                6,
                {
                        {"file:///android_asset/New/Current_A+.wav", 15.0},
                        {"file:///android_asset/New/Current_A.wav", 55.0},
                        {"file:///android_asset/New/Current_B.wav", 125.0},
                        {"file:///android_asset/New/Current_Behind.wav", 180.0},
                }
        },
        {
                6,
                {
                        {"file:///android_asset/Tactile/Tactile_OnAxis.wav", 15.0},
                        {"file:///android_asset/Tactile/Tactile_OffAxis.wav", 125.0},
                        {"file:///android_asset/Tactile/Tactile_Behind.wav", 180.0}
                }
        },
        {
                6,
                {
                        {"file:///android_asset/Flare/Flare_A+.wav", 15.0},
                        {"file:///android_asset/Flare/Flare_A.wav", 55.0},
                        {"file:///android_asset/Flare/Flare_B.wav", 125.0},
                        {"file:///android_asset/Flare/Flare_Behind.wav", 180.0}
                }
        },
        {
                6,
                {
                        {"file:///android_asset/Shimmer/Shimmer_A+.wav", 15.0},
                        {"file:///android_asset/Shimmer/Shimmer_A.wav", 55.0},
                        {"file:///android_asset/Shimmer/Shimmer_B.wav", 125.0},
                        {"file:///android_asset/Shimmer/Shimmer_Behind.wav", 180.0}
                }
        },
        {
                6,
                {
                        {"file:///android_asset/Ping/Ping_A+.wav", 15.0},
                        {"file:///android_asset/Ping/Ping_A.wav", 55.0},
                        {"file:///android_asset/Ping/Ping_B.wav", 125.0},
                        {"file:///android_asset/Tactile/Tactile_Behind.wav", 180.0}
                }
        },
        {
                6,
                {
                        {"file:///android_asset/Drop/Drop_A+.wav", 15.0},
                        {"file:///android_asset/Drop/Drop_A.wav", 55.0},
                        {"file:///android_asset/Drop/Drop_Behind.wav", 180.0}
                }
        },
        {
                6,
                {
                        {"file:///android_asset/Signal/Signal_A+.wav", 15.0},
                        {"file:///android_asset/Signal/Signal_A.wav", 55.0},
                        {"file:///android_asset/Drop/Drop_Behind.wav", 180.0}
                }
        },
        {
                12,
                {
                        {"file:///android_asset/Signal Slow/Signal_Slow_A+.wav", 15.0},
                        {"file:///android_asset/Signal Slow/Signal_Slow_A.wav", 55.0},
                        {"file:///android_asset/Signal Slow/Signal_Slow_Behind.wav", 180.0}
                }
        },
        {
                18,
                {
                        {"file:///android_asset/Signal Very Slow/Signal_Very_Slow_A+.wav",
                                15.0},
                        {"file:///android_asset/Signal Very Slow/Signal_Very_Slow_A.wav",
                                55.0},
                        {"file:///android_asset/Signal Very Slow/Signal_Very_Slow_Behind.wav",
                                180.0
                        }
                },
        },
        {
                6,
                {
                        {"file:///android_asset/Mallet/Mallet_A+.wav", 15.0},
                        {"file:///android_asset/Mallet/Mallet_A.wav", 55.0},
                        {"file:///android_asset/Mallet/Mallet_Behind.wav", 180.0}
                }
        },
        {
                12,
                {
                        {"file:///android_asset/Mallet Slow/Mallet_Slow_A+.wav", 15.0},
                        {"file:///android_asset/Mallet Slow/Mallet_Slow_A.wav", 55.0},
                        {"file:///android_asset/Mallet Slow/Mallet_Slow_Behind.wav", 180.0}
                }
        },
        {
                18,
                {
                        {"file:///android_asset/Mallet Very Slow/Mallet_Very_Slow_A+.wav", 15.0},
                        {"file:///android_asset/Mallet Very Slow/Mallet_Very_Slow_A.wav", 55.0},
                        {"file:///android_asset/Mallet Very Slow/Mallet_Very_Slow_Behind.wav", 180.0}
                }
        }
};

#if 0
    static FMOD_RESULT F_CALLBACK LoggingCallback(FMOD_DEBUG_FLAGS flags,
                                                  const char *file,
                                                  int line,
                                                  const char *func,
                                                  const char *message)
    {
        TRACE("%d of %s: %s", line, file, message);
        return FMOD_OK;
    }
#endif

    AudioEngine::AudioEngine() noexcept
               : m_BeaconTypeIndex(1) {
        FMOD_RESULT result;

        TRACE("%s %p", __FUNCTION__, this);

        // Create a System object and initialize
        FMOD::System *system;
        FMOD::System_Create(&system);
        m_pSystem = system;

        result = m_pSystem->setSoftwareFormat(22050, FMOD_SPEAKERMODE_SURROUND, 0);
        ERROR_CHECK(result);

        result = m_pSystem->init(32, FMOD_INIT_NORMAL, nullptr);
        ERROR_CHECK(result);

        result = m_pSystem->set3DSettings(1.0, FMOD_DISTANCE_FACTOR, 1.0f);
        ERROR_CHECK(result);
#if 0
        int numdrivers = 0;
        result = m_pSystem->getNumDrivers(&numdrivers);
        ERROR_CHECK(result);
        for(int id = 0; id < numdrivers; ++id) {
            char name[256];
            FMOD_GUID guid;
            int systemrate;
            FMOD_SPEAKERMODE speakermode;
            int speakermodechannels;

            result = m_pSystem->getDriverInfo(id,
                    name,
                    sizeof(name),
                    &guid,
                    &systemrate,
                    &speakermode,
                    &speakermodechannels);
            ERROR_CHECK(result);

            TRACE("Audio driver: %s  %d %d %d", name, systemrate, speakermode, speakermodechannels);
        }
#endif
    }

    AudioEngine::~AudioEngine() {

        TRACE("%s %p", __FUNCTION__, this);

        {
            std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
            // Deleting the PositionedAudio calls RemoveBeacon which removes it from m_Beacons
            while(!m_Beacons.empty())
            {
                delete *m_Beacons.begin();
            }
        }

        TRACE("System release");
        auto result = m_pSystem->release();
        ERROR_CHECK(result);
        m_pSystem = nullptr;
    }

    void
    AudioEngine::UpdateGeometry(double listenerLatitude, double listenerLongitude,
                                double listenerHeading) {
        const FMOD_VECTOR up = {0.0f, 1.0f, 0.0f};

        // Set listener position
        FMOD_VECTOR listener_position;
        listener_position.x = static_cast<float>(listenerLongitude);
        listener_position.y = 0.0f;
        listener_position.z = static_cast<float>(listenerLatitude);

        // ********* NOTE ******* READ NEXT COMMENT!!!!!
        // vel = how far we moved last FRAME (m/f), then time compensate it to SECONDS (m/s).
        // TODO: replace INTERFACE_UPDATE_TIME with calculated time difference
        const int INTERFACE_UPDATE_TIME = 50;

        FMOD_VECTOR vel;
        vel.x = static_cast<float>((listener_position.x - m_LastPos.x) * (1000.0 / INTERFACE_UPDATE_TIME));
        vel.y = static_cast<float>((listener_position.y - m_LastPos.y) * (1000.0 / INTERFACE_UPDATE_TIME));
        vel.z = static_cast<float>((listener_position.z - m_LastPos.z) * (1000.0 / INTERFACE_UPDATE_TIME));

        // store pos for next time
        m_LastPos = listener_position;

        // Set listener direction
        auto rads = static_cast<float>((listenerHeading * M_PI) / 180.0);
        FMOD_VECTOR forward = {sin(rads), 0.0f, cos(rads)};

        //TRACE("heading: %d %f, %f %f", heading, rads, forward.x, forward.z)
        {
            // Each time through we need to:
            //
            // 1. Check for any EOF and delete those Beacons. If the beacon was in the list of
            //    queued beacons, then we should also start playback of the next queued beacon if
            //    there is one.
            // 2. Update the listener location and heading in each active Beacon. This allows
            //    beacons to switch the audio being played when the listener is pointing away from
            //    the beacon.
            //
            std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
            auto it = m_Beacons.begin();
            bool start_next = false;
            while(it != m_Beacons.end()) {
                if((*it)->IsEof()) {
                    if(*m_QueuedBeacons.begin() == *it) {
                        // The EOF is from the head of the list of queued beacons so start the next one
                        m_QueuedBeacons.pop_front();
                        start_next = true;
                    }

                    TRACE("Remove EOF beacon");
                    delete *it;
                    it = m_Beacons.begin();
                    continue;
                }

                (*it)->UpdateGeometry(listenerHeading, listenerLatitude, listenerLongitude);
                ++it;
            }
            if(start_next && !m_QueuedBeacons.empty())
            {
                TRACE("PlayNow on next queued beacon");
                (*m_QueuedBeacons.begin())->PlayNow();
            }
        }

        auto result = m_pSystem->set3DListenerAttributes(0, &listener_position, &vel, &forward, &up);
        ERROR_CHECK(result);

        result = m_pSystem->update();
        ERROR_CHECK(result);
    }

    void AudioEngine::SetBeaconType(int beaconType)
    {
        if(beaconType < (sizeof(msc_BeaconDescriptors)/sizeof(BeaconDescriptor))) {
            m_BeaconTypeIndex = beaconType;

            // TODO: This call only has any effect when made prior to Beacon creation. Any Beacons
            //  which are already sounding will not currently be affected. To support this we need
            //  to reinitialize Beacons with the new beacon type.
            return;
        }
        TRACE("BeaconType failed, invalid type: %d", beaconType);
    }

    const BeaconDescriptor *AudioEngine::GetBeaconDescriptor() const
    {
        return &msc_BeaconDescriptors[m_BeaconTypeIndex];
    }

    void AudioEngine::AddBeacon(PositionedAudio *beacon, bool queued)
    {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        m_Beacons.insert(beacon);
        TRACE("AddBeacon -> %zu beacons", m_Beacons.size());
        if(queued)
        {
            if(m_QueuedBeacons.empty()) {
                TRACE("First beacon in queue - PlayNow");
                beacon->PlayNow();
            }
            m_QueuedBeacons.push_back(beacon);
            TRACE("Queue of %zu", m_QueuedBeacons.size());
        }
    }

    void AudioEngine::RemoveBeacon(PositionedAudio *beacon)
    {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        m_Beacons.erase(beacon);

        TRACE("RemoveBeacon -> %zu beacons", m_Beacons.size());
    }



} // soundscape

extern "C"
JNIEXPORT jlong JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_create(JNIEnv *env MAYBE_UNUSED, jobject thiz MAYBE_UNUSED) {
    auto ae = std::make_unique<soundscape::AudioEngine>();

    if (not ae) {
        TRACE("Failed to create audio engine");
        ae.reset(nullptr);
    }

    return reinterpret_cast<jlong>(ae.release());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_destroy(JNIEnv *env MAYBE_UNUSED,
                                                                     jobject thiz MAYBE_UNUSED,
                                                                     jlong engine_handle) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    delete ae;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_updateGeometry(JNIEnv *env MAYBE_UNUSED,
                                                                           jobject thiz MAYBE_UNUSED,
                                                                           jlong engine_handle,
                                                                           jdouble latitude,
                                                                           jdouble longitude,
                                                                           jdouble heading) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);

    if (ae) {
        ae->UpdateGeometry(latitude, longitude, heading);
    } else {
        TRACE("UpdateGeometry failed - no AudioEngine");
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_setBeaconType(JNIEnv *env MAYBE_UNUSED, jobject thiz MAYBE_UNUSED,
                                                                          jlong engine_handle,
                                                                          jint beacon_type) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);

    if (ae) {
        ae->SetBeaconType(beacon_type);
    } else {
        TRACE("SetBeaconType failed - no AudioEngine");
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_createNativeBeacon(JNIEnv *env MAYBE_UNUSED,
                                                                               jobject thiz MAYBE_UNUSED,
                                                                               jlong engine_handle,
                                                                               jdouble latitude,
                                                                               jdouble longitude) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {

        auto beacon = std::make_unique<soundscape::Beacon>(ae, latitude, longitude);
        if (not beacon) {
            TRACE("Failed to create audio beacon");
            beacon.reset(nullptr);
        }
        return reinterpret_cast<jlong>(beacon.release());
    }
    return 0L;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_destroyNativeBeacon(JNIEnv *env MAYBE_UNUSED,
                                                                                jobject thiz MAYBE_UNUSED,
                                                                                jlong beacon_handle) {
    auto beacon = reinterpret_cast<soundscape::Beacon*>(beacon_handle);
    delete beacon;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_scottishtecharmy_soundscape_audio_NativeAudioEngine_createNativeTextToSpeech(JNIEnv *env MAYBE_UNUSED,
                                                                                     jobject thiz MAYBE_UNUSED,
                                                                                     jlong engine_handle,
                                                                                     jdouble latitude,
                                                                                     jdouble longitude,
                                                                                     jint tts_socket) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {

        auto tts = std::make_unique<soundscape::TextToSpeech>(ae, latitude, longitude, tts_socket);
        if (not tts) {
            TRACE("Failed to create text to speech");
            tts.reset(nullptr);
        }
        return reinterpret_cast<jlong>(tts.release());
    }
    return 0L;
}