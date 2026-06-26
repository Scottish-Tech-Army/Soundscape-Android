#pragma once

#include <string>
#include <atomic>
#include <vector>
#include <memory>
#include <android/asset_manager.h>

#include "AudioSourceBase.h"
#include "BeaconDescriptor.h"
#include "WavDecoder.h"
#include "SimpleResampler.h"

namespace soundscape {

    class PositionedAudio;
    class AudioEngine;

    class BeaconBuffer {
    public:
        BeaconBuffer(AAssetManager *mgr, const std::string &filename,
                     double max_angle, int targetSampleRate);

        ~BeaconBuffer();

        // Read float32 samples at target sample rate. Returns frames read.
        unsigned int Read(float *data, unsigned int numFrames, unsigned long pos,
                          bool pad_with_silence);

        [[nodiscard]] unsigned int GetNumFrames() const {
            return m_Decoder ? m_Decoder->numFrames() : 0;
        }

        [[nodiscard]] bool CheckIsActive(double degrees_off_axis) const;

    private:
        double m_MaxAngle;
        std::string m_Name;
        std::unique_ptr<WavDecoder> m_Decoder;
    };

    class BeaconAudioSource : public AudioSourceBase {
    public:
        explicit BeaconAudioSource(PositionedAudio *parent,
                                   double degrees_off_axis);

        ~BeaconAudioSource() override = default;

        enum SourceMode {
            DIRECTION_MODE,
            FAR_MODE,
            NEAR_MODE,
            TOO_FAR_MODE
        };

        virtual void UpdateGeometry(double degrees_off_axis, SourceMode mode);

        bool isAudible() const override {
            return !isFinished() && !muted.load() && m_Mode.load() != TOO_FAR_MODE;
        }

        void UpdateAudioConfig(int sample_rate, int audio_format, int channel_count) {
            m_SrcSampleRate = sample_rate;
            m_SrcAudioFormat = audio_format;
            m_SrcChannelCount = channel_count;
        }

    protected:
        PositionedAudio *m_pParent;

        int m_SrcSampleRate = 44100;
        int m_SrcAudioFormat = 1;   // 0=PCM8, 1=PCM16, 2=PCMFLOAT
        int m_SrcChannelCount = 1;

        std::atomic<double> m_DegreesOffAxis;
        std::atomic<BeaconAudioSource::SourceMode> m_Mode = DIRECTION_MODE;
    };

    class BeaconBufferGroup : public BeaconAudioSource {
    public:
        BeaconBufferGroup(AAssetManager *mgr,
                          const BeaconDescriptor *beacon_descriptor,
                          PositionedAudio *parent,
                          double degrees_off_axis,
                          int targetSampleRate);

        ~BeaconBufferGroup() override;

        // AudioSourceBase interface
        int readPcm(float *outMono, int numFrames) override;
        bool isFinished() const override;

    private:
        void UpdateCurrentBufferFromHeadingAndLocation();

        enum PlayState {
            PLAYING_INTRO,
            PLAYING_BEACON,
            PLAYING_OUTRO,
            PLAYING_COMPLETE
        };
        PlayState m_PlayState = PLAYING_BEACON;

        const BeaconDescriptor *m_pDescription;
        std::unique_ptr<BeaconBuffer> m_pIntro;
        std::unique_ptr<BeaconBuffer> m_pOutro;
        std::vector<std::unique_ptr<BeaconBuffer>> m_pBuffers;
        BeaconBuffer *m_pCurrentBuffer = nullptr;
        unsigned long m_FramePos = 0;
    };

    class TtsAudioSource : public BeaconAudioSource {
    public:
        TtsAudioSource(PositionedAudio *parent,
                       int tts_socket,
                       int sampleRate,
                       int audioFormat,
                       int channelCount);

        ~TtsAudioSource() override;

        // AudioSourceBase interface
        int readPcm(float *outMono, int numFrames) override;
        bool isFinished() const override;

    private:
        int m_TtsSocket;
        int m_ReadsWithoutData = 0;
        int m_SourceSocketForDebug;
        std::atomic<bool> m_Finished{false};
        SimpleResampler m_Resampler;

        // Intermediate buffer for reading raw socket data
        std::vector<unsigned char> m_RawBuf;
        std::vector<float> m_SrcBuf;
    };

    class EarconSource : public BeaconAudioSource {
    public:
        EarconSource(PositionedAudio *parent, std::string &asset,
                     AAssetManager *mgr, int targetSampleRate);
        ~EarconSource() override = default;

        // AudioSourceBase interface
        int readPcm(float *outMono, int numFrames) override;
        bool isFinished() const override;

        void UpdateGeometry(double degrees_off_axis, SourceMode mode) override;

    private:
        std::unique_ptr<WavDecoder> m_Decoder;
        unsigned long m_FramePos = 0;
    };
}
