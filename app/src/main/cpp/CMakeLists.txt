
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.

cmake_minimum_required(VERSION 3.22.1)

project("soundscape-audio")

# Oboe imported library. Vendored as prebuilts (see scripts/update-oboe.sh) rather than
# consumed via prefab: enabling prefab makes AGP validate the prefab metadata of every AAR
# on the classpath, and MapLibre 13.4.0+ declares stl=c++_static, which is rejected against
# this app's c++_shared build with CXX1211 - even though we never link against MapLibre.
set(OBOE_INC ${CMAKE_CURRENT_SOURCE_DIR}/oboe/include)
set(OBOE_LIB ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/liboboe.so)
add_library(oboe SHARED IMPORTED)
set_target_properties(oboe PROPERTIES
        IMPORTED_LOCATION ${OBOE_LIB}
        INTERFACE_INCLUDE_DIRECTORIES ${OBOE_INC}
)

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
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        oboe
        phonon)
