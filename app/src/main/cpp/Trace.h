#pragma once

#include <android/log.h>

#define MAYBE_UNUSED __attribute__((unused))

// Logcat only. Use for noisy or per-utterance tracing, and anywhere on the real-time audio path.
#define TRACE(args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_DEBUG, "AudioEngine", args)

namespace soundscape {
    // Logcat, and also forwarded to the Kotlin support log so that it ends up in the log
    // attached to Contact Support emails. Makes a JNI call, so keep it off the per-buffer
    // audio path and use it only for rare events such as the output stream opening or closing.
    void SupportLog(const char *format, ...) __attribute__((format(printf, 1, 2)));
}

#define SUPPORT_LOG(args...) soundscape::SupportLog(args)
