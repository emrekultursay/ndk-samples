#include <jni.h>
#include <string>

static void triggerNullCppReferenceHelper() {
    int* ptr = nullptr;
    ptr[123] = 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_nativecrash_NativeCrash_triggerNullCppReference(JNIEnv *env, jobject thiz) {
    triggerNullCppReferenceHelper();
}

static void triggerSigabortHelper() {
    raise(SIGABRT);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_nativecrash_NativeCrash_triggerSigabort(JNIEnv *env, jobject thiz) {
    triggerSigabortHelper();
}