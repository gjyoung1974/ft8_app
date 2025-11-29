LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ft8
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ft8_lib
LOCAL_SRC_FILES := \
    ft8_jni.c \
    ft8_lib/ft8/constants.c \
    ft8_lib/ft8/crc.c \
    ft8_lib/ft8/decode.c \
    ft8_lib/ft8/encode.c \
    ft8_lib/ft8/ldpc.c \
    ft8_lib/ft8/message.c \
    ft8_lib/ft8/text.c \
    ft8_lib/common/audio.c \
    ft8_lib/common/monitor.c \
    ft8_lib/common/wave.c \
    ft8_lib/fft/kiss_fft.c \
    ft8_lib/fft/kiss_fftr.c

LOCAL_CFLAGS := -Wall -O3
LOCAL_LDLIBS := -lm -llog
include $(BUILD_SHARED_LIBRARY)
