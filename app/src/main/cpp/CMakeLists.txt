
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.

cmake_minimum_required(VERSION 3.22.1)

project("soundscape-audio")

# Find Oboe (provided via prefab by the Android dependency)
find_package(oboe REQUIRED CONFIG)

# Steam Audio imported library
set(STEAMAUDIO_INC ${CMAKE_CURRENT_SOURCE_DIR}/steamaudio/include)
set(STEAMAUDIO_LIB ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libphonon.so)
add_library(phonon SHARED IMPORTED)
set_target_properties(phonon PROPERTIES
    IMPORTED_LOCATION ${STEAMAUDIO_LIB}
    INTERFACE_INCLUDE_DIRECTORIES ${STEAMAUDIO_INC}
)

# Main shared library
add_library(${CMAKE_PROJECT_NAME} SHARED
    AudioEngine.cpp
    AudioBeacon.cpp
    AudioBeaconBuffer.cpp
    WavDecoder.cpp
    SimpleResampler.cpp
    SteamAudioSpatializer.cpp
    AudioMixer.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${STEAMAUDIO_DIR}/include
)

target_link_libraries(${CMAKE_PROJECT_NAME}
    android
    log
    oboe::oboe
    phonon)
