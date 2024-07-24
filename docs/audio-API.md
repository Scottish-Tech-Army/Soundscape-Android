---
tags: [Mermaid]
mermaid: true
---

# Audio API for Soundscape
This document aims to describe the API for a library that can be exposed to the main Kotlin code in the Soundscape app. On Android this library will be in C++ so that it can directly call the [FMOD library](https://www.fmod.com/core) which allows positioning of audio within a 3D space.

## iOS Soundscape audio classes
The iOS app has a host of audio classes to support playback of beacons, text to speech and other sounds.

<div class="mermaid">
classDiagram
    class SoundBase{
        +SoundType type
        +Int layerCount
    }
    class Sound{
        nextBuffer(Int for_layer)
    }
    class DynamicSound{
        +AVAudioFormat commonFormat
        +BeaconAccents introAsset
        +BeaconAccents outroAsset
        asset(CLLocationDirection userHeading, CLLocation userLocation)
        buffer(AssetType asset)
    }
    class TTSSound{
    }
    class LayeredSound{
    }
    class SynchronouslyGeneratedSound{
    }
    class GenericSound{
    }
    class GlyphSound{
    }

    SoundBase <|-- Sound
    SoundBase <|-- DynamicSound
    Sound <|-- TTSSound
    Sound <|-- LayeredSound
    Sound <|-- SynchronouslyGeneratedSound
    Sound <|-- ConcatenatedSound
    DynamicSound <|-- BeaconSound
    SynchronouslyGeneratedSound <|-- GenericSound
    GenericSound <|-- GlyphSound

    class AudioPlayer{
        AudioPlayerIndentifier id
        SoundBase sound
        AudioPlayerState state
        Bool isPlaying
        Bool is3D 
        Float volume
        prepare(AVAudioEngine engine)
        updateConnectionState(AudioPlayerConnectionState state)
        play(Heading userHeading, CLLocation userLocation)
        resumeIfNecessary()
        stop()
    }
    AudioPlayer<|--BaseAudioPlayer
    BaseAudioPlayer<|--ContinuousAudioPlayer
    BaseAudioPlayer<|--DiscreteAudioPlayerDelegate
    AudioPlayer<|--DynamicAudioPlayer
    AVAudioPlayer<|--FadeableAudioPlayer
</div>

* **GlyphSound** are short audio files with specific meaning to the user e.g. app going offline/online, or entering/leaving a waypoint. I find them fairly indistinguishable, but perhaps with use and practice their meaning becomes obvious.
* **LayeredSound** seems to be for mixing sounds together. Each layer can have its own EQ parameters defined. 
* **ConcatenatedSound** simply joins sounds together to be played one after the other.
* **BeaconSound** vary with the user heading/location.

The classes can be user together to build complex sounds. An example playback from the Swift code is:


```
// Create a GlyphSound of the type poiSense:
let earcon = GlyphSound(.poiSense, compass: direction)

// Create a Text to Speech sound
let tts = TTSSound(approach, compass: direction)
            
// Concatenate them
let ttsSound = ConcatenatedSound(earcon, tts)

// And layer with a travelEnd sound
let layered = LayeredSound(ttsSound, GlyphSound(.travelEnd, compass: direction))
```

## 3D audio on Android
It's a fairly confusing situation on Android, but I think there's a path through it. Amongst other things, the FMOD Core audio engine gives us the ability to place mono audio sources within a 3D space, and configure a listener position and direction. By manipulating the listener direction we could in theory implement head tracking with Bluetooth head tracking events being fed into FMOD. This should work on all versions of Android. The main technical issue is the head tracking latency. However, FMOD is designed with this in mind, so perhaps it would be okay? A thread can update the listener direction every 30ms or so, so the latency depends on how much buffering is between the listener calculated audio and the output device. FMOD doesn't include HRTF calculations, so the output to the headphones would just be panned stereo, but that's likely 'good enough' at least as a fallback.

The two directions that can be available within Android are:

1. ```AHeadingEvent``` - the direction in which the device is pointing relative to true north in degrees.
2. ```AHeadTrackerEvent``` - a vector representing the direction of the head. This appeared in Android 12L (March 2022), prior to that there was no built in support (needs confirming).

From [Android 13 onwards, there's a Spatializer](https://source.android.com/docs/core/audio/spatial) which can take surround sound input and automatically add head tracking. It's not clear that all phone support this as the head tracking/spatializer link has to be implemented by the phone manufacturer. My Pixel 8 does have this, and I can turn it on and enable spatialization on my Bluetooth headphones. To utilize this, FMOD is switched to output in surround sound mode ```setSoftwareFormat(22050, FMOD_SPEAKERMODE_SURROUND, 0)```. The FMOD engine places the mono sound sources in the 5.0 speaker field and then plays that out. Android then passes that 5.0 output through its Spatializer to apply HRTFs and turn the audio into binaural for headphones. There are two possibilities for implementing head tracking:

1. Head tracking is done within the Spatializer. The 5.0 soundfield would be fixed e.g. as if the 'cinema' were oriented with the screen on the North wall and any head tracking would be done outwith FMOD. The upside of this should be that it's the lowest latency possible, but I'd worry that the spatial resolution may be poorer when facing South towards a PositionedAudio. In that case FMOD will be rendering the PositionedAudio on the rear right/left speakers and then the Spatializer would be using head tracking to turn that around. Needs some testing/investigation.
2. Head tracking remains within FMOD. This would still have the advantage that the Spatializer would be turning the 5.0 into binaural audio, and the user would always be facing the main LCR speakers. However, the latency would be worse than option 1. 

To support head tracking prior to Android 13 we have to try and implement option 2 anyway, so perhaps we do that first and then see how the latency is? An initial approach would be simply to use the phone direction (compass) in place of head tracking.


## Audio Engine Kotlin classes

There's [a great video](https://www.youtube.com/watch?v=Zwmhp7W6K6E) showing how to map Kotlin classes over to C++. The Kotlin class simply has a `long` which is the pointer to the C++ object and then this is passed into C wrapper functions which forward calls on to the C++ objects. The other useful bit is the `synchronized` keyword and the way that the Kotlin member functions are defined. With this knowledge we can then simply design Kotlin classes for the audio engine. The current classes we have in C++ from the proof of concept are:

<div class="mermaid">
classDiagram

    class BeaconBuffer{
        +void * m_pBuffer;
        +unsigned int m_BufferSize;

        BeaconBuffer(const std::string &filename)
        readData(void *data, unsigned int datalen, unsigned long pos)
        unsigned int GetBufferSize()    
    }
    
    class BeaconBufferGroup{
        BeaconBufferGroup()
        unsigned int pcmReadCallback(FMOD_SOUND *sound, void *data, unsigned int datalen)
        +vector BeaconBuffer m_BeaconBuffers // Buffers for different headings
        +unsigned int m_CurrentBuffer // Which buffer is currently playing
    }

    class BeaconAudioSource{
        void UpdateGeometry(double degrees_off_axis, double distance)
        virtual void CreateSound(FMOD::System *system, FMOD::Sound **sound)
    }

    class TtsAudioSource{
        TtsAudioSource(int tts_socket)
        unsigned int pcmReadCallback(FMOD_SOUND *sound, void *data, unsigned int datalen)
        +int m_TtsSocket
    }

    class PositionedAudio{
        PositionedAudio(AudioEngine *engine, double latitude, double longitude)
        UpdateGeometry(double heading, double latitude, double longitude)
        virtual void CreateAudioSource()
        +BeaconAudioSource m_AudioSource
        +double m_Latitude
        +double m_Longitude
        AudioEngine *m_pEngine
    }
    
    class AudioEngine {
        void UpdateGeometry(double listenerLatitude, double listenerLongitude, double listenerHeading);
        void SetBeaconType(int beaconType);
        const BeaconDescriptor *GetBeaconDescriptor() const;

        void AddBeacon(PositionedAudio *beacon);
        void RemoveBeacon(PositionedAudio *beacon);

        +FMOD::System * m_pSystem
        +std::recursive_mutex m_BeaconsMutex
        +std::set<PositionedAudio *> m_Beacons
    }

    class Beacon{
        void CreateAudioSource()
    }

    class TextToSpeech{
        void CreateAudioSource()
    }
    PositionedAudio *-- BeaconAudioSource
    BeaconBufferGroup *-- BeaconBuffer
    BeaconAudioSource <-- TtsAudioSource
    BeaconAudioSource <-- BeaconBufferGroup
    PositionedAudio <-- Beacon
    PositionedAudio <-- TextToSpeech
    AudioEngine *-- PositionedAudio
</div>

The only thing that Kotlin needs to be able to do is create and destroy Beacons. An `AudioEngine` class to wrap this behaviour up with audio initialization and destruction makes sense.

```
interface AudioEngine {
    fun createBeacon(latitude: Double, longitude: Double) : Long
    fun createTextToSpeech(latitude: Double, longitude: Double, text: String) : Long
    fun updateGeometry(listenerLatitude: Double, listenerLongitude: Double, listenerHeading: Double)
    fun setBeaconType(beaconType: Int)
}
```
