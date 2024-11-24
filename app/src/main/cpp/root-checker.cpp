//
// Created by jacktor-stan on 24/11/2024.
//

#include <jni.h>
#include <string>
#include <fstream>
#include <sys/stat.h>

bool fileExists(const char *path) {
    struct stat info{};
    return stat(path, &info) == 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jacktor_batterylab_utilities_RootChecker_isDeviceRooted([[maybe_unused]] JNIEnv *env,
                                                                 jobject /* this */) {
    // Array of common root indicators
    const char *rootIndicators[] = {
            "/system/xbin/su",
            "/system/bin/su",
            "/system/app/Superuser.apk",
            "/system/xbin/busybox",
            "/system/sd/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su",
            "/magisk/.core",
            "/sbin/su"
    };

    // Check for each path
    for (const char *path: rootIndicators) {
        if (fileExists(path)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}
