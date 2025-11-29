#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>
#include <android/log.h>

#include "ft8_lib/ft8/encode.h"
#include "ft8_lib/ft8/decode.h"
#include "ft8_lib/ft8/message.h"
#include "ft8_lib/ft8/constants.h"

#define LOG_TAG "FT8_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define FT8_SAMPLE_RATE 12000

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Decoder state
static ftx_waterfall_t waterfall;
static ftx_decode_status_t decoder;

JNIEXPORT jfloatArray JNICALL
Java_com_gyoung_ft8_1app_FT8Lib_encodeMessage(JNIEnv *env, jobject obj,
                                              jstring message, jfloat freqHz) {
    const char *msg_text = (*env)->GetStringUTFChars(env, message, NULL);
    if (msg_text == NULL) {
        return NULL;
    }

    LOGD("Encoding message: %s at %.1f Hz", msg_text, freqHz);

    // Create message structure
    ftx_message_t msg;
    memset(&msg, 0, sizeof(ftx_message_t));

    // Encode the message
    ftx_message_rc_t rc = ftx_message_encode(&msg, NULL, msg_text);

    if (rc != FTX_MESSAGE_RC_OK) {
        LOGE("Failed to encode message: %s (error code: %d)", msg_text, rc);
        (*env)->ReleaseStringUTFChars(env, message, msg_text);
        return NULL;
    }

    // Generate symbols (tones) - ft8_encode expects the payload bytes
    uint8_t tones[FT8_NN];
    ft8_encode(msg.payload, tones);

    // Generate audio samples
    int num_samples = (int)(FT8_SLOT_TIME * FT8_SAMPLE_RATE);
    float *samples = (float *)malloc(num_samples * sizeof(float));

    if (samples == NULL) {
        LOGE("Failed to allocate memory for samples");
        (*env)->ReleaseStringUTFChars(env, message, msg_text);
        return NULL;
    }

    // Clear samples
    memset(samples, 0, num_samples * sizeof(float));

    // Synthesize the signal
    for (int i = 0; i < FT8_NN; i++) {
        float freq = freqHz + (tones[i] * 6.25f); // 6.25 Hz tone spacing for FT8
        int start_idx = (int)(i * FT8_SYMBOL_PERIOD * FT8_SAMPLE_RATE);
        int end_idx = (int)((i + 1) * FT8_SYMBOL_PERIOD * FT8_SAMPLE_RATE);

        for (int j = start_idx; j < end_idx && j < num_samples; j++) {
            float t = (float)j / FT8_SAMPLE_RATE;
            samples[j] += 0.9f * sinf(2.0f * (float)M_PI * freq * t);
        }
    }

    // Create Java float array
    jfloatArray result = (*env)->NewFloatArray(env, num_samples);
    if (result == NULL) {
        free(samples);
        (*env)->ReleaseStringUTFChars(env, message, msg_text);
        return NULL;
    }

    (*env)->SetFloatArrayRegion(env, result, 0, num_samples, samples);

    free(samples);
    (*env)->ReleaseStringUTFChars(env, message, msg_text);

    LOGD("Successfully generated %d samples", num_samples);

    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_gyoung_ft8_1app_FT8Lib_decodeMessages(JNIEnv *env, jobject obj,
                                               jfloatArray samples, jint numSamples) {
    // Get the samples from Java
    jfloat *sample_data = (*env)->GetFloatArrayElements(env, samples, NULL);
    if (sample_data == NULL) {
        return NULL;
    }

    LOGD("Decoding %d samples", numSamples);

    // For now, return empty array
    // Full decoding implementation requires:
    // 1. FFT to convert audio to waterfall (spectrogram)
    // 2. Call ft8_find_sync() to find candidate signals
    // 3. Call ft8_decode() on each candidate
    // 4. Convert decoded messages to Java objects

    (*env)->ReleaseFloatArrayElements(env, samples, sample_data, JNI_ABORT);

    // Create empty result array
    jclass messageClass = (*env)->FindClass(env, "com/gyoung/ft8_app/FT8Lib$FT8Message");
    jobjectArray result = (*env)->NewObjectArray(env, 0, messageClass, NULL);

    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_gyoung_ft8_1app_FT8Lib_packMessage(JNIEnv *env, jobject obj, jstring message) {
    const char *msg_text = (*env)->GetStringUTFChars(env, message, NULL);
    if (msg_text == NULL) {
        return NULL;
    }

    LOGD("Packing message: %s", msg_text);

    // Create message structure
    ftx_message_t msg;
    memset(&msg, 0, sizeof(ftx_message_t));

    // Encode the message
    ftx_message_rc_t rc = ftx_message_encode(&msg, NULL, msg_text);

    (*env)->ReleaseStringUTFChars(env, message, msg_text);

    if (rc != FTX_MESSAGE_RC_OK) {
        LOGE("Failed to pack message (error code: %d)", rc);
        return NULL;
    }

    // The payload is in msg.payload
    jbyteArray result = (*env)->NewByteArray(env, FTX_LDPC_K_BYTES);
    if (result == NULL) {
        return NULL;
    }

    (*env)->SetByteArrayRegion(env, result, 0, FTX_LDPC_K_BYTES, (jbyte *)msg.payload);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_gyoung_ft8_1app_FT8Lib_unpackMessage(JNIEnv *env, jobject obj, jbyteArray payload) {
    jbyte *packed = (*env)->GetByteArrayElements(env, payload, NULL);
    if (packed == NULL) {
        return NULL;
    }

    // Create message structure with the payload
    ftx_message_t msg;
    memset(&msg, 0, sizeof(ftx_message_t));
    memcpy(msg.payload, packed, FTX_LDPC_K_BYTES);

    char message[128];
    ftx_message_rc_t rc = ftx_message_decode(&msg, NULL, message, NULL);

    (*env)->ReleaseByteArrayElements(env, payload, packed, JNI_ABORT);

    if (rc != FTX_MESSAGE_RC_OK) {
        LOGE("Failed to unpack message (error code: %d)", rc);
        return NULL;
    }

    return (*env)->NewStringUTF(env, message);
}

JNIEXPORT void JNICALL
Java_com_gyoung_ft8_1app_FT8Lib_initDecoder(JNIEnv *env, jobject obj, jint maxCandidates) {
LOGD("Initializing decoder with max %d candidates", maxCandidates);

// Initialize waterfall structure
memset(&waterfall, 0, sizeof(ftx_waterfall_t));

// Initialize decoder status
memset(&decoder, 0, sizeof(ftx_decode_status_t));
}

JNIEXPORT void JNICALL
Java_com_gyoung_ft8_1app_FT8Lib_cleanupDecoder(JNIEnv *env, jobject obj) {
LOGD("Cleaning up decoder");
// Cleanup if needed
}
