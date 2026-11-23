# SonicDemixer

Android app scaffold for offline audio stem separation, using Kotlin and ONNX Runtime on Android.

- Select WAV PCM 16-bit stereo audio (currently requires 44.1 kHz).
- HT-Demucs ONNX model is downloaded once, then inference is intended to run locally.
- The debug APK build workflow runs on pushes to `main` and uploads the APK as a workflow artifact.

**Status:** source code and CI build are present, but runtime model compatibility and separation quality have not yet been verified on a device.