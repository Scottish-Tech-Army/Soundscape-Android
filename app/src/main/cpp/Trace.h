#pragma once

#include <android/log.h>
#include <fmod_errors.h>

#define MAYBE_UNUSED __attribute__((unused))

#define TRACE(args...) \
__android_log_print(android_LogPriority::ANDROID_LOG_DEBUG, "AudioEngine", args)
#define ERROR_CHECK(a) if(a) TRACE("line %d, result %d (%s)", __LINE__, a, FMOD_ErrorString(a))
