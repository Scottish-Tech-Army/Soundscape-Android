#include "AudioEngine.h"
#include "AudioBeacon.h"
#include "GeoUtils.h"
#include "Trace.h"

#include <thread>
#include <memory>
#include <mutex>
#include <android/log.h>
#include <android/asset_manager_jni.h>
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

    AudioEngine::AudioEngine(AAssetManager *assetManager) noexcept
               : m_pAssetManager(assetManager),
                 m_BeaconTypeIndex(1) {

        TRACE("%s %p", __FUNCTION__, this);

        // Create and start the audio mixer (Oboe + Steam Audio)
        m_pMixer = std::make_unique<AudioMixer>();
        if (!m_pMixer->start()) {
            TRACE("AudioEngine: mixer failed to start");
        }
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

        // Stop the mixer after all sources are removed
        if (m_pMixer) {
            m_pMixer->stop();
        }

        TRACE("AudioEngine destroyed");

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

        if (m_jBeaconListener != nullptr) {
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
        }
        m_jBeaconListener = env->NewGlobalRef(listener_obj);
        if (m_jBeaconListener == nullptr) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to create global ref for listener");
            return;
        }

        jclass listener_class = env->GetObjectClass(m_jBeaconListener);
        if (listener_class == nullptr) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to get listener class");
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
            return;
        }

        m_jMethodId_onAllBeaconsCleared = env->GetMethodID(listener_class, "onAllBeaconsCleared", "()V");
        if (m_jMethodId_onAllBeaconsCleared == nullptr) {
            TRACE("AudioEngine::SetBeaconEventsListener - Failed to get method ID for onAllBeaconsCleared");
            env->DeleteGlobalRef(m_jBeaconListener);
            m_jBeaconListener = nullptr;
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
        int getEnvStat = m_pJvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);

        if (getEnvStat == JNI_EDETACHED) {
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

        env->CallVoidMethod(m_jBeaconListener, m_jMethodId_onAllBeaconsCleared);

        if (env->ExceptionCheck()) {
            TRACE("NotifyAllBeaconsCleared: Exception occurred calling Kotlin method");
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        if (didAttach) {
            m_pJvm->DetachCurrentThread();
        }
        TRACE("NotifyAllBeaconsCleared from %d: Kotlin notified.", line);
    }

    void
    AudioEngine::BeaconDestroyed() {
        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        if (m_Beacons.empty() && m_QueuedBeacons.empty()) {
            NotifyAllBeaconsCleared(__LINE__);
        }
    }

    void
    AudioEngine::UpdateGeometry(double listenerLatitude, double listenerLongitude,
                                double listenerHeading, bool focusGained, bool duckingAllowed,
                                double proximityNear) {

        if (listenerHeading > 10000.0)
            listenerHeading = NAN;

        // Volume control via mixer
        if (m_pMixer) {
            if (focusGained) {
                m_pMixer->setSpeechVolume(1.0f);
                if (isnan(listenerHeading)) {
                    m_pMixer->setBeaconVolume(0.2f);
                } else {
                    m_pMixer->setBeaconVolume(1.0f);
                }
            } else {
                if (duckingAllowed) {
                    m_pMixer->setBeaconVolume(0.1f);
                    m_pMixer->setSpeechVolume(0.2f);
                } else {
                    m_pMixer->setBeaconVolume(0.0f);
                    m_pMixer->setSpeechVolume(0.0f);
                }
            }
        }

        // store pos for next time
        m_LastLatitude = listenerLatitude;
        m_LastLongitude = listenerLongitude;
        m_LastHeading = listenerHeading;

        {
            std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);

            bool wasEmpty = m_Beacons.empty();

            auto it = m_Beacons.begin();
            bool start_next = false;
            while(it != m_Beacons.end()) {
                if((*it)->IsEof()) {
                    if(*m_QueuedBeacons.begin() == *it) {
                        m_QueuedBeacons.pop_front();
                        start_next = true;
                        m_QueuedBeaconPlaying = false;
                    }

                    auto id = (long long)*it;
                    delete *it;
                    it = m_Beacons.begin();
                    Eof(id);
                    continue;
                }

                (*it)->UpdateGeometry(listenerLatitude, listenerLongitude,
                                      listenerHeading, listenerLatitude, listenerLongitude,
                                      proximityNear);
                ++it;
            }
            if(!m_QueuedBeacons.empty()) {
                auto queued_beacon = *m_QueuedBeacons.begin();
                if (start_next && queued_beacon->CanStart()) {
                    m_Beacons.insert(queued_beacon);
                    queued_beacon->PlayNow();
                    m_QueuedBeaconPlaying = true;
                }
                else if(!m_QueuedBeaconPlaying) {
                    if(queued_beacon->CanStart()) {
                        m_Beacons.insert(queued_beacon);
                        queued_beacon->PlayNow();
                        m_QueuedBeaconPlaying = true;
                    }
                }
            }

            if (m_Beacons.empty() && !wasEmpty && m_QueuedBeacons.empty()) {
                NotifyAllBeaconsCleared(__LINE__);
            }
        }
    }

    void AudioEngine::SetBeaconType(int beaconType)
    {
        if(beaconType < (sizeof(msc_BeaconDescriptors)/sizeof(BeaconDescriptor))) {
            m_BeaconTypeIndex = beaconType;
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
        m_BeaconMute ^= true;

        std::lock_guard<std::recursive_mutex> guard(m_BeaconsMutex);
        for(const auto &beacon: m_Beacons) {
            beacon->Mute(m_BeaconMute);
        }

        return m_BeaconMute;
    }

    void AudioEngine::Eof(long long id) {
        // This could be used to generate callbacks to the kotlin code
        // to indicate that some audio has finished.
    }

} // soundscape

extern "C"
JNIEXPORT jlong JNICALL
Java_org_scottishtecharmy_soundscape_audio_NativeAudioEngine_create(JNIEnv *env MAYBE_UNUSED,
                                                                     jobject thiz MAYBE_UNUSED,
                                                                     jobject asset_manager) {
    AAssetManager *mgr = AAssetManager_fromJava(env, asset_manager);
    if (!mgr) {
        TRACE("Failed to get AAssetManager from Java");
        return 0;
    }

    auto ae = std::make_unique<soundscape::AudioEngine>(mgr);

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
                               jboolean ducking_allowed,
                               jdouble proximity_near) {
    auto* ae =
            reinterpret_cast<soundscape::AudioEngine*>(engine_handle);

    if (ae) {
        ae->UpdateGeometry(
                latitude,
                longitude,
                heading,
                focus_gained,
                ducking_allowed,
                proximity_near);
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
                                     jint audio_type,
                                     jboolean heading_only,
                                     jdouble latitude,
                                     jdouble longitude,
                                     jdouble heading) {
    auto* ae = reinterpret_cast<soundscape::AudioEngine*>(engine_handle);
    if(ae) {

        auto beacon = std::make_unique<soundscape::BeaconWithProximity>(
                ae,
                soundscape::PositioningMode(
                        static_cast<soundscape::PositioningMode::AudioType>(audio_type),
                        soundscape::PositioningMode::HEADING,
                        latitude,
                        longitude,
                        heading
                ),
                heading_only
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
    auto beacon = reinterpret_cast<soundscape::BeaconWithProximity*>(beacon_handle);
    auto ae = beacon->m_HeadingBeacon.m_pEngine;
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
                        static_cast<soundscape::PositioningMode::AudioType>(mode),
                        soundscape::PositioningMode::HEADING,
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
                        static_cast<soundscape::PositioningMode::AudioType>(mode),
                        soundscape::PositioningMode::HEADING,
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
        const char * id = env->GetStringUTFChars(utterance_id, nullptr);
        std::string id_string(id);
        env->ReleaseStringUTFChars(utterance_id, id);

        ae->UpdateAudioConfig(id_string, sample_rate, format, channel_count);
    }
}
