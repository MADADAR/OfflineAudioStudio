#include <jni.h>
extern "C" JNIEXPORT jint JNICALL
Java_com_sonicdemixer_MainActivity_nativeEngineVersion(JNIEnv*, jobject) { return 1; }