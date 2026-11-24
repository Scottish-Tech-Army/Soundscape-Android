#include "Trace.h"

#include <cstdarg>
#include <cstdio>
#include <jni.h>

namespace {
    JavaVM *g_Jvm = nullptr;
    jclass g_LogClass = nullptr;
    jmethodID g_LogMethod = nullptr;
}

// Resolve the Kotlin side once, here rather than lazily: FindClass only sees app classes from a
// thread with the app's class loader, and Oboe's callback threads don't have it.
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK)
        return JNI_VERSION_1_6;

    jclass cls = env->FindClass("org/scottishtecharmy/soundscape/audio/NativeAudioEngine");
    if (cls != nullptr) {
        jmethodID method = env->GetStaticMethodID(cls, "nativeLog", "(Ljava/lang/String;)V");
        if (method != nullptr) {
            g_LogClass = static_cast<jclass>(env->NewGlobalRef(cls));
            g_LogMethod = method;
            g_Jvm = vm;
        }
        env->DeleteLocalRef(cls);
    }
    if (env->ExceptionCheck())
        env->ExceptionClear();
    if (g_Jvm == nullptr)
        TRACE("SupportLog: NativeAudioEngine.nativeLog not found, native events go to logcat only");
    return JNI_VERSION_1_6;
}

namespace soundscape {
    void SupportLog(const char *format, ...) {
        char message[512];
        va_list args;
        va_start(args, format);
        vsnprintf(message, sizeof(message), format, args);
        va_end(args);

        __android_log_write(ANDROID_LOG_INFO, "AudioEngine", message);

        if (g_Jvm == nullptr)
            return;

        JNIEnv *env = nullptr;
        bool didAttach = false;
        int status = g_Jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (status == JNI_EDETACHED) {
            if (g_Jvm->AttachCurrentThread(&env, nullptr) != JNI_OK)
                return;
            didAttach = true;
        } else if (status != JNI_OK) {
            return;
        }

        jstring jMessage = env->NewStringUTF(message);
        if (jMessage != nullptr) {
            env->CallStaticVoidMethod(g_LogClass, g_LogMethod, jMessage);
            env->DeleteLocalRef(jMessage);
        }
        if (env->ExceptionCheck())
            env->ExceptionClear();

        // A native thread that exits while still attached aborts the process, so detach any
        // thread (such as Oboe's error callback thread) that we attached here.
        if (didAttach)
            g_Jvm->DetachCurrentThread();
    }
}
