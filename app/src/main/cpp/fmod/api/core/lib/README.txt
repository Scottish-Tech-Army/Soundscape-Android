The FMOD libraries have been moved and are now in the app/src/main/jniLibs
directory. This works around a problem on some Mac installs where the libraries
weren't appearing in the APK despite being linked during the C++ build step.
