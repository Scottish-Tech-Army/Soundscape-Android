#include "SimpleResampler.h"
#include <cmath>

namespace soundscape {

    void SimpleResampler::setRates(int sourceRate, int targetRate) {
        if (targetRate > 0 && sourceRate > 0) {
            m_Ratio = static_cast<double>(sourceRate) / static_cast<double>(targetRate);
        } else {
            m_Ratio = 1.0;
        }
    }

    void SimpleResampler::reset() {
        m_SrcPos = 0.0;
        m_PrevSample = 0.0f;
        m_HasPrev = false;
    }

    int SimpleResampler::process(const float *input, int inputFrames,
                                 float *output, int maxOutputFrames,
                                 int &inputFramesConsumed) {
        int outWritten = 0;
        inputFramesConsumed = 0;

        if (!needsResampling()) {
            // No resampling needed - direct copy
            int toCopy = (inputFrames < maxOutputFrames) ? inputFrames : maxOutputFrames;
            for (int i = 0; i < toCopy; i++) {
                output[i] = input[i];
            }
            inputFramesConsumed = toCopy;
            return toCopy;
        }

        while (outWritten < maxOutputFrames) {
            int srcIdx = static_cast<int>(m_SrcPos);
            float frac = static_cast<float>(m_SrcPos - srcIdx);

            if (srcIdx >= inputFrames) {
                break;
            }

            float s0;
            if (srcIdx == 0 && m_HasPrev) {
                // Interpolate between previous buffer's last sample and current buffer's first
                if (frac < 1e-6f) {
                    s0 = m_PrevSample;
                } else {
                    s0 = m_PrevSample;
                }
            } else if (srcIdx > 0) {
                s0 = input[srcIdx - 1];
            } else {
                s0 = input[0];
                frac = 0.0f;
            }

            float s1 = input[srcIdx];
            float nextSample;
            if (srcIdx + 1 < inputFrames) {
                nextSample = input[srcIdx] * (1.0f - frac) + input[srcIdx + 1] * frac;
            } else {
                nextSample = input[srcIdx];
            }

            output[outWritten++] = nextSample;
            m_SrcPos += m_Ratio;
        }

        inputFramesConsumed = static_cast<int>(m_SrcPos);
        if (inputFramesConsumed > inputFrames) {
            inputFramesConsumed = inputFrames;
        }

        // Save last sample for cross-buffer interpolation
        if (inputFrames > 0) {
            m_PrevSample = input[inputFrames - 1];
            m_HasPrev = true;
        }

        // Adjust position for next call
        m_SrcPos -= static_cast<double>(inputFramesConsumed);

        return outWritten;
    }

    int SimpleResampler::inputFramesNeeded(int outputFrames) const {
        return static_cast<int>(ceil(outputFrames * m_Ratio)) + 1;
    }

} // soundscape
