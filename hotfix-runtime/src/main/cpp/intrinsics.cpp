//
// Created by Luke on 2019/5/29.
//

#include <jni.h>


JNIEXPORT jobject JNICALL
Java_org_keixe_android_hotfix_internal_Intrinsics_invokeNonVirtual(
        JNIEnv *env,
        jclass _,
        jclass type,
        jobject target,
        jobject object,
        jobjectArray pram) {
    auto methodId = env->FromReflectedMethod(target);
    auto length = env->GetArrayLength(pram);
    auto *values = new jvalue[length];
    for (int i = 0; i < length; ++i) {
        values[i].l = env->GetObjectArrayElement(pram, i);
    }
    jobject result = env->CallNonvirtualObjectMethodA(object, type, methodId, values);
    delete values;
    jthrowable ex = env->ExceptionOccurred();
    if (ex != nullptr) {
        env->ExceptionClear();
        env->Throw(ex);
    }
    return result;
}