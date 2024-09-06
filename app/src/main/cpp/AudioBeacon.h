#pragma once
#include "AudioBeaconBuffer.h"

namespace soundscape {

    class AudioEngine;

    class PositionedAudio {
    public:
        PositionedAudio(AudioEngine *engine,
                        double latitude, double longitude);

        virtual ~PositionedAudio();

        void UpdateGeometry(double heading, double latitude, double longitude);

        // CreateAudioSource returns whether or not the audio source should
        // be placed in the list of queued beacons.
        virtual bool CreateAudioSource(double degrees_off_axis) = 0;
        bool IsEof() { return m_Eof; }
        void Eof() { m_Eof = true; }
        void PlayNow();

    protected:
        void Init(double degrees_off_axis);
        void InitFmodSound();

        double GetHeadingOffset(double heading, double latitude, double longitude) const;

        // We're going to assume that the beacons are close enough that the earth is effectively flat
        double m_Latitude = 0.0;
        double m_Longitude = 0.0;

        std::atomic<bool> m_Eof;

        std::unique_ptr<BeaconAudioSource> m_pAudioSource;
        FMOD::System *m_pSystem = nullptr;
        FMOD::Sound *m_pSound = nullptr;
        FMOD::Channel *m_pChannel = nullptr;
        AudioEngine *m_pEngine;
    };

    class Beacon : public PositionedAudio {
    public:
        Beacon(AudioEngine *engine, double latitude, double longitude)
         : PositionedAudio(engine, latitude, longitude)
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
        TextToSpeech(AudioEngine *engine, double latitude, double longitude, int tts_socket)
                : m_TtsSocket(tts_socket),
                  PositionedAudio(engine, latitude, longitude)
        {
            Init(0.0);
        }

    protected:
        bool CreateAudioSource(double degrees_off_axis) final
        {
            m_pAudioSource = std::make_unique<TtsAudioSource>(m_pEngine, this, m_TtsSocket);
            // Text to speech audio are queued to play one after the other
            return true;
        }

        int m_TtsSocket;
    };
}