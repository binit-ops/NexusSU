#include <jni.h>
#include <string>
#include <sys/prctl.h>
#include <android/log.h>

#define NEXUSSU_PRCTL_MAGIC 0x4E535553
#define CMD_GRANT_UID 1
#define CMD_REVOKE_UID 2
#define CMD_ESCALATE_SELF 3
#define CMD_GET_VERSION 4
#define CMD_REGISTER_MANAGER 5

#define LOG_TAG "NexusBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nexussu_manager_core_NexusEngine_registerManager(JNIEnv *env, jobject thiz) {
    int ret = prctl(NEXUSSU_PRCTL_MAGIC, CMD_REGISTER_MANAGER, 0, 0, 0);
    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nexussu_manager_core_NexusEngine_grantUidAccess(JNIEnv *env, jobject thiz, jint uid) {
    int ret = prctl(NEXUSSU_PRCTL_MAGIC, CMD_GRANT_UID, (unsigned long)uid, 0, 0);
    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nexussu_manager_core_NexusEngine_revokeUidAccess(JNIEnv *env, jobject thiz, jint uid) {
    int ret = prctl(NEXUSSU_PRCTL_MAGIC, CMD_REVOKE_UID, (unsigned long)uid, 0, 0);
    return ret == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nexussu_manager_core_NexusEngine_escalateSelf(JNIEnv *env, jobject thiz) {
    int ret = prctl(NEXUSSU_PRCTL_MAGIC, CMD_ESCALATE_SELF, 0, 0, 0);
    if (ret == 0) {
        LOGI("NexusSU Manager escalated to root via prctl");
        return JNI_TRUE;
    }
    LOGE("prctl escalation failed");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nexussu_manager_core_NexusEngine_getEngineVersion(JNIEnv *env, jobject thiz) {
    return prctl(NEXUSSU_PRCTL_MAGIC, CMD_GET_VERSION, 0, 0, 0);
}

#define CMD_CHECK_MANAGER 9
#define CMD_RESET_MANAGER 10

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nexussu_manager_core_NexusEngine_checkManager(JNIEnv *env, jobject thiz) {
    int ret = prctl(NEXUSSU_PRCTL_MAGIC, CMD_CHECK_MANAGER, 0, 0, 0);
    return ret == 1 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nexussu_manager_core_NexusEngine_resetManager(JNIEnv *env, jobject thiz) {
    prctl(NEXUSSU_PRCTL_MAGIC, CMD_RESET_MANAGER, 0, 0, 0);
}
