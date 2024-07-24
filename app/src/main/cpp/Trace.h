#pragma once

#include <android/log.h>

#define MAYBE_UNUSED __attribute__((unused))

#define TRACE(args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_DEBUG, "AudioEngine", args)
#define ERROR_CHECK(a) if(a) TRACE("line %d, result %d", __LINE__, a)
