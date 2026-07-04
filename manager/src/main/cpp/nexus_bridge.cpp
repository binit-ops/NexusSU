#include <jni.h>
#include <string>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <android/log.h>

#define NEXUSSU_IOC_MAGIC 'N'
#define NEXUSSU_ALLOW_UID _IOW(NEXUSSU_IOC_MAGIC, 1, uid_t)
#define NEXUSSU_GET_VERSION _IOR(NEXUSSU_IOC_MAGIC, 2, int)

#define LOG_TAG "NexusBridge"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_nexussu_manager_core_NexusEngine_grantUidAccess(JNIEnv *env, jobject thiz, jint uid) {
    
    int fd = open("/dev/nexussu", O_RDWR);
    if (fd < 0) {
        LOGE("Failed to open /dev/nexussu. Is the kernel patched?");
        return JNI_FALSE;
    }

    uid_t target_uid = static_cast<uid_t>(uid);
    int result = ioctl(fd, NEXUSSU_ALLOW_UID, &target_uid);
    close(fd);

    if (result < 0) {
        LOGE("ioctl NEXUSSU_ALLOW_UID failed for UID %d", target_uid);
        return JNI_FALSE;
    }

    LOGI("Successfully routed UID %d to kernel allowlist", target_uid);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nexussu_manager_core_NexusEngine_getEngineVersion(JNIEnv *env, jobject thiz) {
    
    int fd = open("/dev/nexussu", O_RDONLY);
    if (fd < 0) {
        return -1;
    }

    int version = -1;
    int result = ioctl(fd, NEXUSSU_GET_VERSION, &version);
    close(fd);

    if (result < 0) {
        return -1;
    }

    return version;
}
