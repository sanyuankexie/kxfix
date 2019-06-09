//
// Created by Luke on 2019/5/29.
//

#include <jni.h>
#include <stdlib.h>


JNIEXPORT jobject JNICALL
Java_org_kexie_android_hotfix_internal_ReflectOperation_invokeNonVirtual(
        JNIEnv *env,
        jclass _,
        jclass type,
        jobject target,
        jobject object,
        jobjectArray prams) {
    auto methodId = env->FromReflectedMethod(target);
    auto length = env->GetArrayLength(prams);
    auto *values = (jvalue*)alloca(sizeof(jvalue)*length);
    for (int i = 0; i < length; ++i) {
        values[i].l = env->GetObjectArrayElement(prams, i);
    }
    jobject result = env->CallNonvirtualObjectMethodA(object, type, methodId, values);
    jthrowable ex = env->ExceptionOccurred();
    if (ex != nullptr) {
        env->ExceptionClear();
        env->Throw(ex);
    }
    return result;
}