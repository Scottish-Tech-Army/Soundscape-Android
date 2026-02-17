#pragma once
#include <utility>

#include "AudioBeaconBuffer.h"
#include "AudioEngine.h"

namespace soundscape {

    enum {
        NEAR_INDEX = 0,
        FAR_INDEX = 1
    };
    const BeaconDescriptor msc_ProximityDescriptor =
    {
        "Proximity",
        36,
        {
            {"file:///android_asset/Route/Proximity_Close.wav" ,0}, // NEAR_INDEX
            {"file:///android_asset/Route/Proximity_Far.wav" ,0},   // FAR_INDEX
        }
    };

    class PositionedAudio {
    public:
        PositionedAudio(AudioEngine *engine, PositioningMode mode, bool dimmable = false, std::string utterance_id = "");

        virtual ~PositionedAudio();

        void UpdateGeometry(double listenerLatitude, double listenerLongitude,
                            double heading, double latitude, double longitude,
                            double proximityNear);

        // CreateAudioSource returns whether or not the audio source should
        // be placed in the list of queued beacons.
        virtual bool CreateAudioSource(double degrees_off_axis,
                                       int sampleRate,
                                       int audioFormat,
                                       int channelCount,
                                       bool proximityBeacon) = 0;
        bool IsEof() { return m_Eof; }
        void Eof() { m_Eof = true; }
        void PlayNow();
        void Mute(bool mute);
        virtual bool CanStart() = 0;

        void UpdateAudioConfig(int sample_rate, int audio_format, int channel_count);

        AudioEngine *m_pEngine;
        std::string m_UtteranceId;

    protected:
        void Init(double degrees_off_axis,
                  bool proximityBeacon = false,
                  int sampleRate = 44100,
                  int audioFormat = 1,
                  int channelCount = 1);
        void RegisterWithMixer();

        double GetHeadingOffset(double heading, double latitude, double longitude) const;

        PositioningMode m_Mode;

        std::atomic<bool> m_Eof;

        std::unique_ptr<BeaconAudioSource> m_pAudioSource;
        bool m_Dimmable = false;

        bool m_AudioConfigured = false;
    };

    class Beacon : public PositionedAudio {
    public:
        Beacon(AudioEngine *engine, PositioningMode mode);

    protected:
        bool CanStart() override { return true; }
        bool CreateAudioSource(double degrees_off_axis,
                               int sampleRate,
                               int audioFormat,
                               int channelCount,
                               bool proximityBeacon) final;
    };

    class BeaconWithProximity {
    public:
        BeaconWithProximity(AudioEngine *engine, PositioningMode mode, bool heading_only) :
            m_HeadingBeacon(engine, mode)
        {
            if(!heading_only) {
                mode.m_AudioMode = PositioningMode::PROXIMITY;
                mode.m_AudioType = PositioningMode::STANDARD;
                m_pProximityBeacon = std::make_unique<soundscape::Beacon>(engine, mode);
            }
        }

        Beacon m_HeadingBeacon;
        std::unique_ptr<Beacon> m_pProximityBeacon;
    };


    class TextToSpeech : public PositionedAudio {
    public:
        bool CanStart() override { return m_AudioConfigured; }
        TextToSpeech(AudioEngine *engine,
                     PositioningMode mode,
                     int tts_socket,
                     std::string &utterance_id);

    protected:
        bool CreateAudioSource(double degrees_off_axis,
                               int sampleRate,
                               int audioFormat,
                               int channelCount,
                               bool proximityBeacon) final;

        int m_TtsSocket;
    };

    class Earcon : public PositionedAudio {
    public:
        Earcon(AudioEngine *engine,
               std::string asset,
               PositioningMode mode);

    protected:
        bool CanStart() override { return true; }
        bool CreateAudioSource(double degrees_off_axis,
                               int sampleRate,
                               int audioFormat,
                               int channelCount,
                               bool proximityBeacon) final;

        std::string m_Asset;
    };
}
