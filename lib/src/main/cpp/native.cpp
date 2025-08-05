/*
Java native interface to C++ encoder and decoder

Copyright 2022 Ahmet Inan <inan@aicodix.de> and other authors
*/

#include <jni.h>
#include <cstdint>
#define assert(expr) do {} while (0)
#include "encoder.hh"
#include "decoder.hh"

extern "C" JNIEXPORT jlong JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_EncoderImpl_createNewEncoder(
	JNIEnv *,
	jobject,
	jint sampleRate) {
	EncoderInterface *encoder;
	switch (sampleRate) {
		case 8000:
			encoder = new(std::nothrow) Encoder<8000>();
			break;
		case 16000:
			encoder = new(std::nothrow) Encoder<16000>();
			break;
		case 32000:
			encoder = new(std::nothrow) Encoder<32000>();
			break;
		case 44100:
			encoder = new(std::nothrow) Encoder<44100>();
			break;
		case 48000:
			encoder = new(std::nothrow) Encoder<48000>();
			break;
		default:
			encoder = nullptr;
	}
	return reinterpret_cast<jlong>(encoder);
}

extern "C" JNIEXPORT void JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_EncoderImpl_destroyEncoder(
	JNIEnv *,
	jobject,
	jlong encoderHandle) {
	EncoderInterface* encoder = reinterpret_cast<EncoderInterface*>(encoderHandle);
	delete encoder;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_EncoderImpl_produceEncoder(
	JNIEnv *env,
	jobject,
	jlong encoderHandle,
	jshortArray JNI_audioBuffer,
	jint channelSelect) {
	EncoderInterface* encoder = reinterpret_cast<EncoderInterface*>(encoderHandle);
	if (!encoder)
		return false;

	jshort *audioBuffer = env->GetShortArrayElements(JNI_audioBuffer, nullptr);
	jboolean okay = false;
	if (audioBuffer)
		okay = encoder->produce(audioBuffer, channelSelect);
	env->ReleaseShortArrayElements(JNI_audioBuffer, audioBuffer, 0);
	return okay;
}

extern "C" JNIEXPORT void JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_EncoderImpl_configureEncoder(
	JNIEnv *env,
	jobject,
	jlong encoderHandle,
	jbyteArray JNI_payload,
	jbyteArray JNI_callSign,
	jint carrierFrequency,
	jint noiseSymbols,
	jboolean fancyHeader) {
	EncoderInterface* encoder = reinterpret_cast<EncoderInterface*>(encoderHandle);
	if (!encoder)
		return;

	jbyte *payload, *callSign;
	payload = env->GetByteArrayElements(JNI_payload, nullptr);
	if (!payload)
		goto payloadFail;
	callSign = env->GetByteArrayElements(JNI_callSign, nullptr);
	if (!callSign)
		goto callSignFail;

	encoder->configure(
		reinterpret_cast<uint8_t *>(payload),
		reinterpret_cast<int8_t *>(callSign),
		carrierFrequency,
		noiseSymbols,
		fancyHeader);

	env->ReleaseByteArrayElements(JNI_callSign, callSign, JNI_ABORT);
	callSignFail:
	env->ReleaseByteArrayElements(JNI_payload, payload, JNI_ABORT);
	payloadFail:;
}

extern "C" JNIEXPORT void JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_destroyDecoder(
	JNIEnv *,
	jobject,
	jlong decoderHandle) {
	if (decoderHandle != 0) {
        DecoderInterface* decoder = reinterpret_cast<DecoderInterface*>(decoderHandle);
        delete decoder;
	}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_createNewDecoder(
	JNIEnv *,
	jobject,
	jint sampleRate) {
	DecoderInterface *decoder;
	switch (sampleRate) {
		case 8000:
			decoder = new(std::nothrow) Decoder<8000>();
			break;
		case 16000:
			decoder = new(std::nothrow) Decoder<16000>();
			break;
		case 32000:
			decoder = new(std::nothrow) Decoder<32000>();
			break;
		case 44100:
			decoder = new(std::nothrow) Decoder<44100>();
			break;
		case 48000:
			decoder = new(std::nothrow) Decoder<48000>();
			break;
		default:
			decoder = nullptr;
	}
	return reinterpret_cast<jlong>(decoder);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_fetchDecoder(
	JNIEnv *env,
	jobject,
	jlong decoderHandle,
	jbyteArray JNI_payload) {
	DecoderInterface* decoder = reinterpret_cast<DecoderInterface*>(decoderHandle);
	jint status = -1;
	if (decoder) {
		jbyte *payload = env->GetByteArrayElements(JNI_payload, nullptr);
		if (payload)
			status = decoder->fetch(reinterpret_cast<uint8_t *>(payload));
		env->ReleaseByteArrayElements(JNI_payload, payload, 0);
	}
	return status;
}

extern "C" JNIEXPORT void JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_stagedDecoder(
	JNIEnv *env,
	jobject,
	jlong decoderHandle,
	jfloatArray JNI_carrierFrequencyOffset,
	jintArray JNI_operationMode,
	jbyteArray JNI_callSign) {

	DecoderInterface* decoder = reinterpret_cast<DecoderInterface*>(decoderHandle);
	if (!decoder)
		return;

	jint *operationMode;
	jfloat *carrierFrequencyOffset;
	jbyte *callSign;
	carrierFrequencyOffset = env->GetFloatArrayElements(JNI_carrierFrequencyOffset, nullptr);
	if (!carrierFrequencyOffset)
		goto carrierFrequencyOffsetFail;
	operationMode = env->GetIntArrayElements(JNI_operationMode, nullptr);
	if (!operationMode)
		goto operationModeFail;
	callSign = env->GetByteArrayElements(JNI_callSign, nullptr);
	if (!callSign)
		goto callSignFail;

	decoder->staged(
		reinterpret_cast<float *>(carrierFrequencyOffset),
		reinterpret_cast<int32_t *>(operationMode),
		reinterpret_cast<uint8_t *>(callSign));

	env->ReleaseByteArrayElements(JNI_callSign, callSign, 0);
	callSignFail:
	env->ReleaseIntArrayElements(JNI_operationMode, operationMode, 0);
	operationModeFail:
	env->ReleaseFloatArrayElements(JNI_carrierFrequencyOffset, carrierFrequencyOffset, 0);
	carrierFrequencyOffsetFail:;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_feedDecoder(
	JNIEnv *env,
	jobject,
	jlong decoderHandle,
	jshortArray JNI_audioBuffer,
	jint sampleCount,
	jint channelSelect) {

	DecoderInterface* decoder = reinterpret_cast<DecoderInterface*>(decoderHandle);
	jboolean status = false;

	if (!decoder)
		return status;

	jshort *audioBuffer;
	audioBuffer = env->GetShortArrayElements(JNI_audioBuffer, nullptr);
	if (!audioBuffer)
		goto audioBufferFail;

	status = decoder->feed(
		reinterpret_cast<int16_t *>(audioBuffer),
		sampleCount, channelSelect);

	env->ReleaseShortArrayElements(JNI_audioBuffer, audioBuffer, JNI_ABORT);
	audioBufferFail:

	return status;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_processDecoder(
	JNIEnv *,
	jobject,
	jlong decoderHandle) {
	DecoderInterface* decoder = reinterpret_cast<DecoderInterface*>(decoderHandle);

	if (!decoder)
		return STATUS_HEAP;

	return decoder->process();
}

extern "C" JNIEXPORT void JNICALL
Java_com_github_observant_1sun_rattlegram_encoding_DecoderImpl_spectrumDecoder(
	JNIEnv *env,
	jobject,
	jlong decoderHandle,
	jintArray JNI_spectrumPixels,
	jintArray JNI_spectrogramPixels,
	jint spectrumTint) {

	DecoderInterface* decoder = reinterpret_cast<DecoderInterface*>(decoderHandle);

	if (!decoder)
		return;

	jint *spectrumPixels, *spectrogramPixels;
	spectrumPixels = env->GetIntArrayElements(JNI_spectrumPixels, nullptr);
	if (!spectrumPixels)
		goto spectrumFail;
	spectrogramPixels = env->GetIntArrayElements(JNI_spectrogramPixels, nullptr);
	if (!spectrogramPixels)
		goto spectrogramFail;

	decoder->spectrum(
		reinterpret_cast<uint32_t *>(spectrumPixels),
		reinterpret_cast<uint32_t *>(spectrogramPixels),
		spectrumTint);

	env->ReleaseIntArrayElements(JNI_spectrogramPixels, spectrogramPixels, 0);
	spectrogramFail:
	env->ReleaseIntArrayElements(JNI_spectrumPixels, spectrumPixels, 0);
	spectrumFail:;
}

