#include "AudioEngine.h"
#include "AudioBeacon.h"
#include "GeoUtils.h"
#include "Trace.h"

#include <thread>
#include <memory>
#include <mutex>
#include <android/log.h>
#include <jni.h>
#include <cassert>

namespace soundscape {

const BeaconDescriptor AudioEngine::msc_BeaconDescriptors[] =
{
        {
                "Original",
                2,
                {
                        {"file:///android_asset/Classic/Classic_OnAxis.wav", 22.5},
                        {"file:///android_asset/Classic/Classic_OffAxis.wav", 180.0},
                }
        },
        {
                "Current",
                6,
                {
                        {"file:///android_asset/New/Current_A+.wav", 15.0},
                        {"file:///android_asset/New/Current_A.wav", 55.0},
                        {"file:///android_asset/New/Current_B.wav", 125.0},
                        {"file:///android_asset/New/Current_Behind.wav", 180.0},
                }
        },
        {
                "Tactile",
                6,
                {
                        {"file:///android_asset/Tactile/Tactile_OnAxis.wav", 15.0},
                        {"file:///android_asset/Tactile/Tactile_OffAxis.wav", 125.0},
                        {"file:///android_asset/Tactile/Tactile_Behind.wav", 180.0}
                }
        },
        {
                "Flare",
                6,
                {
                        {"file:///android_asset/Flare/Flare_A+.wav", 15.0},
                        {"file:///android_asset/Flare/Flare_A.wav", 55.0},
                        {"file:///android_asset/Flare/Flare_B.wav", 125.0},
                        {"file:///android_asset/Flare/Flare_Behind.wav", 180.0}
                }
        },
        {
                "Shimmer",
                6,
                {
                        {"file:///android_asset/Shimmer/Shimmer_A+.wav", 15.0},
                        {"file:///android_asset/Shimmer/Shimmer_A.wav", 55.0},
                        {"file:///android_asset/Shimmer/Shimmer_B.wav", 125.0},
                        {"file:///android_asset/Shimmer/Shimmer_Behind.wav", 180.0}
                }
        },
        {
                "Ping",
                6,
                {
                        {"file:///android_asset/Ping/Ping_A+.wav", 15.0},
                        {"file:///android_asset/Ping/Ping_A.wav", 55.0},
                        {"file:///android_asset/Ping/Ping_B.wav", 125.0},
                        {"file:///android_asset/Tactile/Tactile_Behind.wav", 180.0}
                }
        },
        {
                "Drop",
                6,
                {
                        {"file:///android_asset/Drop/Drop_A+.wav", 15.0},
                        {"file:///android_asset/Drop/Drop_A.wav", 55.0},
                        {"file:///android_asset/Drop/Drop_Behind.wav", 180.0}
                }
        },
        {
                "Signal",
                6,
                {
                        {"file:///android_asset/Signal/Signal_A+.wav", 15.0},
                        {"file:///android_asset/Signal/Signal_A.wav", 55.0},
                        {"file:///android_asset/Drop/Drop_Behind.wav", 180.0}
                }
        },
        {
                "Signal Slow",
                12,
                {
                        {"file:///android_asset/Signal Slow/Signal_Slow_A+.wav", 15.0},
                        {"file:///android_asset/Signal Slow/Signal_Slow_A.wav", 55.0},
                        {"file:///android_asset/Signal Slow/Signal_Slow_Behind.wav", 180.0}
                }
        },
        {
                "Signal Very Slow",
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
                "Mallet",
                6,
                {
                        {"file:///android_asset/Mallet/Mallet_A+.wav", 15.0},
                        {"file:///android_asset/Mallet/Mallet_A.wav", 55.0},
                        {"file:///android_asset/Mallet/Mallet_Behind.wav", 180.0}
                }
        },
        {
                "Mallet Slow",
                12,
                {
                        {"file:///android_asset/Mallet Slow/Mallet_Slow_A+.wav", 15.0},
                        {"file:///android_asset/Mallet Slow/Mallet_Slow_A.wav", 55.0},
                        {"file:///android_asset/Mallet Slow/Mallet_Slow_Behind.wav", 180.0}
                }
        },
        {
                "Mallet Very Slow",
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
        FMOD::System *system = nullptr;
        FMOD::System_Create(&system);
        m_pSystem = system;

        // As of FMOD 2.03.06 Android spatial audio is supported. Prior to that version we set the
        // setSoftwareFormat with FMOD_SPEAKERMODE_SURROUND, but since spatial audio support was
        // added the behaviour changed and the audio wasn't always appearing in the correct
        // location. Intersection callouts in particular all seemed to be called out from directly
        // ahead. We now set it to FMOD_SPEAKERMODE_STEREO. In future we can investigate using
        // Android spatial audio.
        result = m_pSystem->setSoftwareFormat(22050, FMOD_SPEAKERMODE_STEREO, 0);
        ERROR_CHECK(result);

        result = m_pSystem->init(32, FMOD_INIT_NORMAL, nullptr);
        ERROR_CHECK(result);

        result = m_pSystem->set3DSettings(1.0, FMOD_DISTANCE_FACTOR, 1.0f);
        ERROR_CHECK(result);

        // Create the channel groups
        result = system->createChannelGroup("beacons", &m_pBeaconChannelGroup);
        ERROR_CHECK(result);
        result = system->createChannelGroup("speech", &m_pSpeechChannelGroup);
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

            // Clear the queued up audio
            ClearQueue();

            // Deleting the PositionedAudio calls RemoveBeacon which removes it from m_Beacons
            while(!m_Beacons.empty())
            {
                delete *m_Beacons.begin();
            }
        }

        TRACE("Release groups");
        m_pBeaconChannelGroup->release();
        m_pSpeechChannelGroup->release();

        TRACE("System release");
        auto result = m_pSystem->release();
        ERROR_CHECK(result);
        m_pSystem = nullptr;

        // We rely on clearBeaconEventsListener having been called prior to this call
        assert(m_pJvm == nullptr);
        assert(m_jBeaconListener == nullptr);
    }

    void AudioEngine::SetBeaconEventsListener(JNIEnv *env, jobject listener_obj) {
        // Store the JavaVM pointer
        if (env->GetJavaVM(&m_pJvm) != JNI_OK) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to get JavaVM");
            m_pJvm = nullptr;
            return;
        }

        // Create a global reference to the listener object (NativeAudioEngine instance)
        // Make sure to delete this global reference when the listener is no longer needed
        // (e.g., in a ClearBeaconEventsListener method or destructor)
        if (m_jBeaconListener != nullptr) {
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
        }
        m_jBeaconListener = env->NewGlobalRef(listener_obj);
        if (m_jBeaconListener == nullptr) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to create global ref for listener");
            return;
        }

        // Get the class of the listener object
        jclass listener_class = env->GetObjectClass(m_jBeaconListener);
        if (listener_class == nullptr) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to get listener class");
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
            return;
        }

        // Get the method ID for the callback method in NativeAudioEngine.kt
        // The method signature "()V" means a method with no arguments that returns void.
        m_jMethodId_onAllBeaconsCleared = env->GetMethodID(listener_class, "onAllBeaconsCleared", "()V");
        if (m_jMethodId_onAllBeaconsCleared == nullptr) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to get method ID for onAllBeaconsCleared");
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
            // No need to delete listener_class, it's a local reference.
            return;
        }
        TRACE("AudioEngine::SetBeaconEventsListener - Successfully set up listener.");
    }

    void AudioEngine::ClearBeaconEventsListener(JNIEnv *env) {
        if (m_jBeaconListener != nullptr) {
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
            m_pJvm = nullptr;
            m_jMethodId_onAllBeaconsCleared = nullptr;
            TRACE("AudioEngine::ClearBeaconEventsListener - Listener cleared.");
        }
    }

    void AudioEngine::NotifyAllBeaconsCleared(int line) {
        if (m_pJvm == nullptr || m_jBeaconListener == nullptr || m_jMethodId_onAllBeaconsCleared == nullptr) {
            TRACE("NotifyAllBeaconsCleared: JNI listener not set up, cannot notify Kotlin.");
            return;
        }

        JNIEnv *env;
        bool didAttach = false;
        // Check if the current thread is attached to the JVM
        int getEnvStat = m_pJvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);

        if (getEnvStat == JNI_EDETACHED) {
            // If not attached, attach it
            if (m_pJvm->AttachCurrentThread(&env, nullptr) != 0) {
                TRACE("NotifyAllBeaconsCleared: Failed to attach current thread to JVM");
                return;
            }
            didAttach = true;
        } else if (getEnvStat == JNI_EVERSION) {
            TRACE("NotifyAllBeaconsCleared: JNI version not supported");
            return;
        } else if (getEnvStat == JNI_OK) {
            // Already attached
        }

        // Call the Kotlin method
        env->CallVoidMethod(m_jBeaconListener, m_jMethodId_onAllBeaconsCleared);

        // Check for exceptions from the JNI call (good practice)
        if (env->ExceptionCheck()) {
            TRACE("NotifyAllBeaconsCleared: Exception occurred calling Kotlin method");
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        // If we attached the thread, detach it
        if (didAttach) {
            m_pJvm->DetachCurrentThread();
        }
        TRACE("NotifyAllBeaconsCleared from %d: Kotlin notified.", line);
    }

    void
    AudioEngine::BeaconDestroyed() {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        if (m_Beacons.empty()) {
            NotifyAllBeaconsCleared(__LINE__);
        }
    }

    void
    AudioEngine::UpdateGeometry(double listenerLatitude, double listenerLongitude,
                                double listenerHeading, bool focusGained, bool duckingAllowed) {
        const FMOD_VECTOR up = {0.0f, 1.0f, 0.0f};

        //
        // What action do we take if we have no heading available? (i.e. locked phone not laying flat)
        //
        // Beacons need to be 'dim', but we also need relative positioning of TTS so that we can at
        // least do some of the callouts. The iOS docs suggest that My Location, Nearby Markers,
        // Around Me and  Ahead of Me should all still play out.
        //
        if (listenerHeading > 10000.0)
            listenerHeading = NAN;
        if(focusGained) {
            // Drop volume if we have no valid heading
            m_pSpeechChannelGroup->setVolume(1.0);
            if (listenerHeading == NAN) {
                m_pBeaconChannelGroup->setVolume(0.2);
            } else {
                m_pBeaconChannelGroup->setVolume(1.0);
            }
        } else {
            // We have lost audio focus. If we're allowed to duck our audio, drop the volume,
            // otherwise we have to mute.
            if(duckingAllowed) {
                m_pBeaconChannelGroup->setVolume(0.1);
                m_pSpeechChannelGroup->setVolume(0.2);
            } else {
                m_pBeaconChannelGroup->setVolume(0.0);
                m_pSpeechChannelGroup->setVolume(0.0);
            }
        }

        // store pos for next time
        m_LastLatitude = listenerLatitude;
        m_LastLongitude = listenerLongitude;
        m_LastHeading = listenerHeading;

        // Set listener direction
        FMOD_VECTOR forward = {0.0f, 0.0f, 1.0f};
        if(!isnan(listenerHeading))
        {
            auto rads = static_cast<float>((listenerHeading * M_PI) / 180.0);
            forward = {sin(rads), 0.0f, cos(rads)};
        }
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

            bool wasEmpty = m_Beacons.empty();

            auto it = m_Beacons.begin();
            bool start_next = false;
            while(it != m_Beacons.end()) {
                if((*it)->IsEof()) {
                    if(*m_QueuedBeacons.begin() == *it) {
                        //TRACE("EOF from first queued beacon");
                        // The EOF is from the head of the list of queued beacons so start the next one
                        m_QueuedBeacons.pop_front();
                        start_next = true;
                        m_QueuedBeaconPlaying = false;
                    }

                    //TRACE("Remove EOF beacon");
                    auto id = (long long)*it;
                    delete *it;
                    it = m_Beacons.begin();
                    Eof(id);
                    continue;
                }

                (*it)->UpdateGeometry(listenerHeading, listenerLatitude, listenerLongitude);
                ++it;
            }
            if(!m_QueuedBeacons.empty()) {
                auto queued_beacon = *m_QueuedBeacons.begin();
                if (start_next && queued_beacon->CanStart()) {
                    //TRACE("PlayNow on next queued beacon");
                    m_Beacons.insert(queued_beacon);
                    queued_beacon->PlayNow();
                    m_QueuedBeaconPlaying = true;
                }
                else if(!m_QueuedBeaconPlaying) {
                    //TRACE("No queued beacon playing");
                    if(queued_beacon->CanStart()) {
                        //TRACE("PlayNow on CanStart beacon");
                        m_Beacons.insert(queued_beacon);
                        queued_beacon->PlayNow();
                        m_QueuedBeaconPlaying = true;
                    }
                }
            }

            // Check if m_Beacons is empty *after* all removals and potential additions from queue
            if (m_Beacons.empty() && !wasEmpty && m_QueuedBeacons.empty()) {
                NotifyAllBeaconsCleared(__LINE__);
            }
        }
        //TRACE("Queues: %d and %d", m_Beacons.size(), m_QueuedBeacons.size());

        // We're not going to include velocity in our audio modelling, set it to 0.0 (no doppler!)
        FMOD_VECTOR vel = {0.0, 0.0, 0.0};
        auto listener_position = TranslateToFmodVector(listenerLongitude, listenerLatitude);
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

    void AudioEngine::ClearQueue(){
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        TRACE("ClearQueue %zu + %zu", m_Beacons.size(), m_QueuedBeacons.size());
        for(const auto &queued_beacon: m_QueuedBeacons)
        {
            delete queued_beacon;
        }
        m_QueuedBeacons.clear();
        m_QueuedBeaconPlaying = false;
    }

    unsigned int AudioEngine::GetQueueDepth() {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        return m_QueuedBeacons.size();
    }

    void AudioEngine::UpdateAudioConfig(std::string &utterance_id,
                                        int sample_rate,
                                        int audio_format,
                                        int channel_count) {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        for(const auto &queued_beacon: m_QueuedBeacons)
        {
            if(queued_beacon->m_UtteranceId == utterance_id) {
                queued_beacon->UpdateAudioConfig(sample_rate, audio_format, channel_count);
            }
        }
    }

    void AudioEngine::AddBeacon(PositionedAudio *beacon, bool queued)
    {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        if(queued)
        {
            if(m_QueuedBeacons.empty() && beacon->CanStart()) {
                TRACE("First beacon in queue - PlayNow");
                beacon->PlayNow();
                m_Beacons.insert(beacon);
                m_QueuedBeaconPlaying = true;
            }
            m_QueuedBeacons.push_back(beacon);
            //TRACE("Queue of %zu", m_QueuedBeacons.size());
        }
        else
        {
            beacon->Mute(m_BeaconMute);
            m_Beacons.insert(beacon);
            TRACE("AddBeacon -> %zu beacons", m_Beacons.size());

        }
    }

    void AudioEngine::RemoveBeacon(PositionedAudio *beacon)
    {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        m_Beacons.erase(beacon);
    }

    bool AudioEngine::ToggleBeaconMute() {
        // Toggle the mute state
        m_BeaconMute ^= true;

        // Update beacons
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        for(const auto &beacon: m_Beacons) {
            beacon->Mute(m_BeaconMute);
        }

        return m_BeaconMute;
    }

    FMOD_VECTOR AudioEngine::TranslateToFmodVector(double longitude, double latitude)
    {
        // For the translation from longitude/latitude into FMOD coordinates we want the origin to
        // be close by as that improves accuracy. We're just going to use the first location that
        // we get.
        if(m_FmodOriginLatitude == 0.0 && m_FmodOriginLongitude == 0.0) {
            m_FmodOriginLatitude = latitude;
            m_FmodOriginLongitude = longitude;
        }
        double x, y;
        translateLocationForFmod(latitude, longitude,
                                 m_FmodOriginLatitude, m_FmodOriginLongitude,
                                 x, y);
        return FMOD_VECTOR{(float)x, 0.0f, (float)y};
    }

    void AudioEngine::Eof(long long id) {
        // This could be used to generate callbacks to the kotlin code
        // to indicate that some audio has finished. Currently it does
        // nothing.
        //TRACE("EOF tts %lld", id);
    }

} // soundscape

extern "C"
JNIEXPORT jlong JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_create(JNIEnv *env MAYBE_UNUSED, jobject thiz MAYBE_UNUSED) {
    auto ae = std::make_unique<soundscape::AudioEngine>();

    if (not ae) {
        TRACE("Failed to create audio engine");
        ae.reset(nullptr);
    }

    return reinterpret_cast<jlong>(ae.release());
}

extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_destroy(JNIEnv *env MAYBE_UNUSED,
                                                                     jobject thiz MAYBE_UNUSED,
                                                                     jlong engine_handle) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    delete ae;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_updateGeometry(JNIEnv *env MAYBE_UNUSED,
                                                                           jobject thiz MAYBE_UNUSED,
                                                                           jlong engine_handle,
                                                                           jdouble latitude,
                                                                           jdouble longitude,
                                                                           jdouble heading,
                                                                           jboolean focus_gained,
                                                                           jboolean ducking_allowed) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);

    if (ae) {
        ae->UpdateGeometry(
                latitude,
                longitude,
                heading,
                focus_gained,
                ducking_allowed);
    } else {
        TRACE("UpdateGeometry failed - no AudioEngine");
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_setBeaconType(JNIEnv *env MAYBE_UNUSED, jobject thiz MAYBE_UNUSED,
                                                                          jlong engine_handle,
                                                                          jstring beacon_type) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);

    if (ae) {
        const char * name = env->GetStringUTFChars(beacon_type, nullptr);
        std::string beacon_string(name);
        env->ReleaseStringUTFChars(beacon_type, name);

        int number_of_beacons = sizeof(soundscape::AudioEngine::msc_BeaconDescriptors)/sizeof(soundscape::BeaconDescriptor);
        for(auto beacon = 0; beacon < number_of_beacons; ++beacon) {
            if(beacon_string == soundscape::AudioEngine::msc_BeaconDescriptors[beacon].m_Name)
            ae->SetBeaconType(beacon);
        }
    } else {
        TRACE("SetBeaconType failed - no AudioEngine");
    }
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_getListOfBeacons(JNIEnv *env, jobject thiz MAYBE_UNUSED){

    jobjectArray array_to_return;

    int number_of_beacons = sizeof(soundscape::AudioEngine::msc_BeaconDescriptors)/sizeof(soundscape::BeaconDescriptor);
    array_to_return = (jobjectArray)env->NewObjectArray(number_of_beacons,
                                                        env->FindClass("java/lang/String"), nullptr);
    for(auto beacon = 0; beacon < number_of_beacons; ++beacon)
    {
        env->SetObjectArrayElement(array_to_return, beacon, env->NewStringUTF(soundscape::AudioEngine::msc_BeaconDescriptors[beacon].m_Name.c_str()));
    }

    return(array_to_return);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_createNativeBeacon(JNIEnv *env MAYBE_UNUSED,
                                     jobject thiz MAYBE_UNUSED,
                                     jlong engine_handle,
                                     jint mode,
                                     jdouble latitude,
                                     jdouble longitude,
                                     jdouble heading) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {

        auto beacon = std::make_unique<soundscape::Beacon>(
                ae,
                soundscape::PositioningMode(
                        static_cast<soundscape::PositioningMode::Type>(mode),
                        latitude,
                        longitude,
                        heading
                )
        );
        if (not beacon) {
            TRACE("Failed to create audio beacon");
            beacon.reset(nullptr);
        }
        return reinterpret_cast<jlong>(beacon.release());
    }
    return 0L;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_getQueueDepth(JNIEnv *env MAYBE_UNUSED,
                                                                                jobject thiz MAYBE_UNUSED,
                                                                                jlong engine_handle) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {

        return static_cast<jlong>(ae->GetQueueDepth());
    }
    return 0L;
}
extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_destroyNativeBeacon(JNIEnv *env MAYBE_UNUSED,
                                                                                jobject thiz MAYBE_UNUSED,
                                                                                jlong beacon_handle) {
    auto beacon = reinterpret_cast<soundscape::Beacon*>(beacon_handle);
    auto ae = beacon->m_pEngine;
    delete beacon;
    if(ae) {
        ae->BeaconDestroyed();
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_toggleNativeBeaconMute(JNIEnv *env MAYBE_UNUSED,
                                                                                 jobject thiz MAYBE_UNUSED,
                                                                                 jlong engine_handle) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {
        return ae->ToggleBeaconMute();
    }
    return false;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_createNativeTextToSpeech(JNIEnv *env MAYBE_UNUSED,
                                           jobject thiz MAYBE_UNUSED,
                                           jlong engine_handle,
                                           jint mode,
                                           jdouble latitude,
                                           jdouble longitude,
                                           jdouble heading,
                                           jint tts_socket,
                                           jstring utterance_id) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {

        const char * id = env->GetStringUTFChars(utterance_id, nullptr);
        std::string id_string(id);
        env->ReleaseStringUTFChars(utterance_id, id);

        auto tts = std::make_unique<soundscape::TextToSpeech>(
                ae,
                soundscape::PositioningMode(
                        static_cast<soundscape::PositioningMode::Type>(mode),
                        latitude,
                        longitude,
                        heading),
                tts_socket,
                id_string
        );
        if (not tts) {
            TRACE("Failed to create text to speech");
            tts.reset(nullptr);
        }
        auto ret = reinterpret_cast<jlong>(tts.release());
        return ret;
    }
    return 0L;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_clearNativeTextToSpeechQueue(JNIEnv *env MAYBE_UNUSED,
                                                                                          jobject thiz MAYBE_UNUSED,
                                                                                          jlong engine_handle) {

    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {
        ae->ClearQueue();
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_createNativeEarcon(JNIEnv *env MAYBE_UNUSED,
                                                                                      jobject thiz MAYBE_UNUSED,
                                                                                      jlong engine_handle,
                                                                                      jstring earcon_asset,
                                                                                      jint mode,
                                                                                      jdouble latitude,
                                                                                      jdouble longitude,
                                                                                      jdouble heading) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {
        const char * asset = env->GetStringUTFChars(earcon_asset, nullptr);
        std::string earcon_string(asset);
        env->ReleaseStringUTFChars(earcon_asset, asset);

        auto earcon = std::make_unique<soundscape::Earcon>(
                ae,
                asset,
                soundscape::PositioningMode(
                        static_cast<soundscape::PositioningMode::Type>(mode),
                        latitude,
                        longitude,
                        heading
                )
        );
        if (not earcon) {
            TRACE("Failed to create Earcon");
            earcon.reset(nullptr);
        }
        auto ret = reinterpret_cast<jlong>(earcon.release());
        //TRACE("Created earcon %lld", ret);
        return ret;
    }
    return 0L;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_setBeaconEventsListener(
        JNIEnv *env,
        jobject thiz, /* this is the NativeAudioEngine instance from Kotlin */
        jlong engine_handle) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if (ae) {
        ae->SetBeaconEventsListener(env, thiz); // Pass 'thiz' as the listener object
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_clearBeaconEventsListener(
        JNIEnv *env,
        jobject thiz, /* unused here but part of JNI signature for non-static native methods */
        jlong engine_handle) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if (ae) {
        ae->ClearBeaconEventsListener(env);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_audioConfigTextToSpeech(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jlong engine_handle,
                                                                                     jstring utterance_id,
                                                                                     jint sample_rate,
                                                                                     jint format,
                                                                                     jint channel_count) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if (ae) {
        // Update the audio source with our audio configuration values
        const char * id = env->GetStringUTFChars(utterance_id, nullptr);
        std::string id_string(id);
        env->ReleaseStringUTFChars(utterance_id, id);

        ae->UpdateAudioConfig(id_string, sample_rate, format, channel_count);
    }
}