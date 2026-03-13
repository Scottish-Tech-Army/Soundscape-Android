#pragma once

#include <atomic>

namespace soundscape {

    enum class AudioCategory {
        BEACON,
        SPEECH
    };

    class AudioSourceBase {
    public:
        virtual ~AudioSourceBase() = default;

        // Pull audio: write numFrames of mono float32 at device sample rate.
        // Returns number of frames actually written (0 means silence/finished).
        virtual int readPcm(float *outMono, int numFrames) = 0;

        // Returns true when this source has finished playing
        virtual bool isFinished() const = 0;

        // Spatial positioning (set from game thread, read from audio thread)
        std::atomic<float> azimuth{0.0f};      // radians, 0=ahead, positive=right
        std::atomic<float> elevation{0.0f};     // radians
        std::atomic<bool> muted{false};

        // Whether this source needs HRTF spatialization (false for STANDARD/2D audio)
        bool needsSpatialize = true;

        // Category for volume control
        AudioCategory category = AudioCategory::SPEECH;

        // True for the proximity/distance beacon (vs. the main heading beacon)
        bool isProximityBeacon = false;

        // Returns true if this source will produce audible output this callback.
        // Base implementation: not finished and not muted.
        virtual bool isAudible() const { return !isFinished() && !muted.load(); }

        void setDeviceSampleRate(int rate) { deviceSampleRate = rate; }
        int getDeviceSampleRate() const { return deviceSampleRate; }

    protected:
        int deviceSampleRate = 48000;
    };

} // soundscape
