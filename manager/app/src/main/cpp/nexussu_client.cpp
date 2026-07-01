#include <jni.h>
#include <fcntl.h>
#include <sys/ioctl.h>

extern "C" JNIEXPORT jboolean JNICALL Java_com_nexussu_manager_core_NativeRoot_requestElevation(JNIEnv *env, jobject thiz) {
    int fd = open("/dev/nexussu", O_RDWR);
    if (fd < 0) return JNI_FALSE;
    int ret = ioctl(fd, 'n', 1); // Elevation
    close(fd);
    return (ret == 0) ? JNI_TRUE : JNI_FALSE;
}
