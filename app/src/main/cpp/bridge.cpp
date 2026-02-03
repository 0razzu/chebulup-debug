#include "ggwave/ggwave.h"

#include <jni.h>
#include <string>
#include <vector>


static ggwave_Instance g_ggwave;


extern "C"
JNIEXPORT void JNICALL Java_com_example_myapplication_GgWaveBridge_init(JNIEnv * env, jobject obj) {
    ggwave_Parameters parameters = ggwave_getDefaultParameters();
    parameters.sampleFormatInp = GGWAVE_SAMPLE_FORMAT_I16;
    parameters.sampleFormatOut = GGWAVE_SAMPLE_FORMAT_I16;
    parameters.sampleRateInp = 48000;
    g_ggwave = ggwave_init(parameters);
}


extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_example_myapplication_GgWaveBridge_encode(JNIEnv *env, jobject, jstring text) {
    const char* msg = env->GetStringUTFChars(text, nullptr);
    const int msgLen = env->GetStringUTFLength(text);

    const int needed = ggwave_encode(
        g_ggwave,
        msg,
        msgLen,
        GGWAVE_PROTOCOL_AUDIBLE_FAST,
        10,
        nullptr,
        1
    );
    std::vector<short> pcm(needed);
    const int produced = ggwave_encode(
        g_ggwave,
        msg,
        msgLen,
        GGWAVE_PROTOCOL_AUDIBLE_FAST,
        10,
        (char*)pcm.data(),
        0
    );

    env->ReleaseStringUTFChars(text, msg);

    jshortArray res = env->NewShortArray(produced);
    env->SetShortArrayRegion(res, 0, produced, pcm.data());

    return res;
}
