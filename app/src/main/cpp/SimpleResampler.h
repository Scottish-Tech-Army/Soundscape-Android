#pragma once

namespace soundscape {

    // Real-time linear interpolation resampler for streaming audio (e.g. TTS).
    // Maintains state across calls for seamless streaming.
    class SimpleResampler {
    public:
        SimpleResampler() = default;

        void setRates(int sourceRate, int targetRate);
        void reset();

        // Resample from input to output. Returns frames written to output.
        // inputFramesConsumed is set to how many input frames were used.
        int process(const float *input, int inputFrames,
                    float *output, int maxOutputFrames,
                    int &inputFramesConsumed);

        // Estimate how many input frames are needed for N output frames
        int inputFramesNeeded(int outputFrames) const;

        bool needsResampling() const { return m_Ratio != 1.0; }

    private:
        double m_Ratio = 1.0;      // sourceRate / targetRate
        double m_SrcPos = 0.0;     // fractional position in input
        float m_PrevSample = 0.0f; // last sample for interpolation across calls
        bool m_HasPrev = false;
    };

} // soundscape
