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
Java_com_example_myapplication_GgWaveBridge_encode(JNIEnv *env, jobject, jbyteArray data) {
    jbyte* msg = env->GetByteArrayElements(data, nullptr);
    jsize len = env->GetArrayLength(data);

    const int needed = ggwave_encode(
        g_ggwave,
        msg,
        len,
        GGWAVE_PROTOCOL_AUDIBLE_FAST,
        10,
        nullptr,
        1
    );
    std::vector<short> pcm(needed);
    const int produced = ggwave_encode(
        g_ggwave,
        msg,
        len,
        GGWAVE_PROTOCOL_AUDIBLE_FAST,
        10,
        (char*)pcm.data(),
        0
    );

    env->ReleaseByteArrayElements(data, msg, JNI_ABORT);

    jshortArray res = env->NewShortArray(needed);
    env->SetShortArrayRegion(res, 0, produced, pcm.data());

    return res;
}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_myapplication_GgWaveBridge_decode(
    JNIEnv *env,
    jobject,
    jshortArray pcm
) {
    jsize len = env->GetArrayLength(pcm);
    jshort* input = env->GetShortArrayElements(pcm, nullptr);

    char res[1024];

    int decodedSize = ggwave_decode(
        g_ggwave,
        input,
        len * sizeof(short),
        res
    );

    env->ReleaseShortArrayElements(pcm, input, JNI_ABORT);

    if (decodedSize <= 0) {
        return nullptr;
    }

    jbyteArray resBytes = env->NewByteArray(decodedSize);
    env->SetByteArrayRegion(
        resBytes,
        0,
        decodedSize,
        reinterpret_cast<jbyte*>(res)
    );

    return resBytes;
}
