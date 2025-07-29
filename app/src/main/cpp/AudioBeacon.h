#pragma once
#include <utility>

#include "AudioBeaconBuffer.h"

namespace soundscape {

    class AudioEngine;

    class PositionedAudio {
    public:
        PositionedAudio(AudioEngine *engine, PositioningMode mode, bool dimmable = false);

        virtual ~PositionedAudio();

        void UpdateGeometry(double heading, double latitude, double longitude);

        // CreateAudioSource returns whether or not the audio source should
        // be placed in the list of queued beacons.
        virtual bool CreateAudioSource(double degrees_off_axis) = 0;
        bool IsEof() { return m_Eof; }
        void Eof() { m_Eof = true; }
        void PlayNow();
        void Mute(bool mute);

        AudioEngine *m_pEngine;
    protected:
        void Init(double degrees_off_axis);
        void InitFmodSound();

        double GetHeadingOffset(double heading, double latitude, double longitude) const;

        PositioningMode m_Mode;

        std::atomic<bool> m_Eof;

        std::unique_ptr<BeaconAudioSource> m_pAudioSource;
        FMOD::System *m_pSystem = nullptr;
        FMOD::Sound *m_pSound = nullptr;
        FMOD::Channel *m_pChannel = nullptr;
        bool m_Dimmable = false;
    };

    class Beacon : public PositionedAudio {
    public:
        Beacon(AudioEngine *engine, PositioningMode mode)
         : PositionedAudio(engine, mode, true)
        {
            // Get the current position and heading of the listener
            double listener_heading;
            double listener_latitude;
            double listener_longitude;
            engine->GetListenerPosition(listener_heading, listener_latitude, listener_longitude);

            // Update the geometry first so that the initial audio sample is the correct one
            // for the direction
            auto degrees_off_axis = GetHeadingOffset(listener_heading, listener_latitude, listener_longitude);
            Init(degrees_off_axis);
        }

    protected:
        bool CreateAudioSource(double degrees_off_axis) final
        {
            m_pAudioSource = std::make_unique<BeaconBufferGroup>(m_pEngine, this, degrees_off_axis);
            // Not queued
            return false;
        }
    };

    class TextToSpeech : public PositionedAudio {
    public:
        TextToSpeech(AudioEngine *engine, PositioningMode mode, int tts_socket)
                : m_TtsSocket(tts_socket),
                  PositionedAudio(engine, mode)
        {
            Init(0.0);
        }

    protected:
        bool CreateAudioSource(double degrees_off_axis) final
        {
            m_pAudioSource = std::make_unique<TtsAudioSource>(this, m_TtsSocket);
            // Text to speech audio are queued to play one after the other
            return true;
        }

        int m_TtsSocket;
    };

    class Earcon : public PositionedAudio {
    public:
        Earcon(AudioEngine *engine,
               std::string asset,
               PositioningMode mode)
                : PositionedAudio(engine, mode),
                  m_Asset(std::move(asset))
        {
            Init(0.0);
        }

    protected:
        bool CreateAudioSource(double degrees_off_axis) final
        {
            m_pAudioSource = std::make_unique<EarconSource>(this, m_Asset);
            // Earcons are queued along with the TextToSpeech audio
            return true;
        }

        std::string m_Asset;
    };
}