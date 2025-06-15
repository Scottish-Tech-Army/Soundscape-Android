#pragma once

#include "fmod.hpp"
#include "fmod.h"
#include <string>
#include <atomic>

#include "AudioEngine.h"
#include "BeaconDescriptor.h"

namespace soundscape {

    class BeaconBuffer {
    public:
        BeaconBuffer(FMOD::System *system,
                     const std::string &filename,
                     double max_angle);

        virtual ~BeaconBuffer();

        unsigned int Read(void *data, unsigned int data_length, unsigned long pos, bool pad_with_silence);

        [[nodiscard]] unsigned int GetBufferSize() const { return m_BufferSize; }

        [[nodiscard]] bool CheckIsActive(double degrees_off_axis) const;

    private:
        double m_MaxAngle;
        std::string m_Name;

        unsigned int m_BufferSize = 0;
        std::unique_ptr<unsigned char[]> m_pBuffer;
    };

    class BeaconAudioSource {
    public:
        explicit BeaconAudioSource(PositionedAudio *parent, double degrees_off_axis) :
                m_pParent(parent),
                m_DegreesOffAxis(degrees_off_axis) {}

        virtual ~BeaconAudioSource() = default;

        virtual void CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode) = 0;

        virtual FMOD_RESULT F_CALL PcmReadCallback(void *data, unsigned int data_length) { return FMOD_ERR_BADCOMMAND; };

        virtual void UpdateGeometry(double degrees_off_axis);

    protected:
        PositionedAudio *m_pParent;

        static FMOD_RESULT F_CALL
        StaticPcmReadCallback(FMOD_SOUND *sound, void *data, unsigned int data_length);

        std::atomic<double> m_DegreesOffAxis;
    };

    class BeaconBufferGroup : public BeaconAudioSource {
    public:
        BeaconBufferGroup(const AudioEngine *ae, PositionedAudio *parent, double degrees_off_axis);

        ~BeaconBufferGroup() override;

        void CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode) override;
        void Stop();

        FMOD_RESULT F_CALL PcmReadCallback(void *data, unsigned int data_length) override;

    private:
        void UpdateCurrentBufferFromHeading();

        enum PlayState {
            PLAYING_INTRO,
            PLAYING_BEACON,
            PLAYING_OUTRO,
            PLAYING_COMPLETE
        };
        PlayState m_PlayState = PLAYING_BEACON;     // TODO: Switch to allow playing Intro

        const BeaconDescriptor *m_pDescription;
        std::unique_ptr<BeaconBuffer> m_pIntro;
        std::unique_ptr<BeaconBuffer> m_pOutro;
        std::vector<std::unique_ptr<BeaconBuffer> > m_pBuffers;
        BeaconBuffer *m_pCurrentBuffer = nullptr;
        unsigned long m_BytePos = 0;
    };

    class TtsAudioSource : public BeaconAudioSource {
    public:
        TtsAudioSource(PositionedAudio *parent, int tts_socket);

        ~TtsAudioSource() override;

        void CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode) override;

        FMOD_RESULT F_CALL PcmReadCallback(void *data, unsigned int data_length) override;

    private:
        int m_TtsSocket;
        int m_ReadsWithoutData = 0;
        int m_SourceSocketForDebug;
    };

    class EarconSource : public BeaconAudioSource {
    public:
        EarconSource(PositionedAudio *parent, std::string &asset);
        ~EarconSource() override = default;

        void CreateSound(FMOD::System *system, FMOD::Sound **sound, const PositioningMode &mode) override;
        void UpdateGeometry(double degrees_off_axis) override;

    private:
        std::string m_Asset;
        FMOD::Sound* m_pSound = nullptr;
    };
}