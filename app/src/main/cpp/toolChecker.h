extern "C" {

#include <jni.h>

jint Java_com_jacktor_rootchecker_RootCheckerNative_setLogDebugMessages([[maybe_unused]] JNIEnv* env,
                                                                        [[maybe_unused]] jobject thiz, jboolean debug);

int Java_com_jacktor_rootchecker_RootCheckerNative_checkForRoot(JNIEnv* env,
                                                                [[maybe_unused]] jobject thiz , jobjectArray pathsArray );

}
