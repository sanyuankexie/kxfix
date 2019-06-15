//
// Created by Luke on 2019/5/29.
//

#include <jni.h>
#include <cstdlib>
#include <map>
#include <functional>
#include <utility>

using namespace std;


//
//class Converter {
//public:
//    Converter(function<jobject(JNIEnv *, jvalue *)> box,
//            function<void(JNIEnv *, jobject, void *ref)> unBox);
//
//    function<jobject(JNIEnv *, jvalue *)> box;
//    function<void(JNIEnv *, jobject, void *ref)> unBox;
//};
//
//Converter::Converter(function<jobject(JNIEnv *, jvalue *)> box,
//                     function<void(JNIEnv *, jobject, void *ref)> unBox)
//        : box(std::move(box)), unBox(std::move(unBox)) {}
//
//
//static class {
//
//    JavaVM*vm;
//    struct JClassComparator {
//        bool operator()(const jclass &k1, const jclass &k2) const {
//            JNIEnv *env = nullptr;
//            vm->GetEnv((void **) (&env), JNI_VERSION_1_6);
//            return ;
//        }
//    };
//public:
//    void LoadMapping(JavaVM*vm) {
//        this->vm = vm;
//        JNIEnv *env = nullptr;
//        vm->GetEnv((void **) (&env), JNI_VERSION_1_6);
//        auto Zclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Boolean"));
//        auto Iclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Integer"));
//        auto Jclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Long"));
//        auto Dclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Double"));
//        auto Fclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Float"));
//        auto Cclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Character"));
//        auto Sclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Short"));
//        auto Bclass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Byte"));
//
//        jmethodID Zmethod = env->GetStaticMethodID(Zclass, "valueOf", "(Z)Ljava/lang/Boolean;");
//        jmethodID Imethod = env->GetStaticMethodID(Iclass, "valueOf", "(I)Ljava/lang/Integer;");
//        jmethodID Jmethod = env->GetStaticMethodID(Jclass, "valueOf", "(J)Ljava/lang/Long;");
//        jmethodID Dmethod = env->GetStaticMethodID(Dclass, "valueOf", "(D)Ljava/lang/Double;");
//        jmethodID Fmethod = env->GetStaticMethodID(Fclass, "valueOf", "(F)Ljava/lang/Float;");
//        jmethodID Cmethod = env->GetStaticMethodID(Cclass, "valueOf", "(C)Ljava/lang/Character;");
//        jmethodID Smethod = env->GetStaticMethodID(Sclass, "valueOf", "(S)Ljava/lang/Short;");
//        jmethodID Bmethod = env->GetStaticMethodID(Bclass, "valueOf", "(B)Ljava/lang/Byte;");
//
//        jmethodID Zmethod2 = env->GetMethodID(Zclass, "booleanValue", "()Z");
//        jmethodID Imethod2 = env->GetMethodID(Iclass, "intValue", "()I");
//        jmethodID Jmethod2 = env->GetMethodID(Jclass, "longValue", "()J");
//        jmethodID Dmethod2 = env->GetMethodID(Dclass, "doubleValue", "()D");
//        jmethodID Fmethod2 = env->GetMethodID(Fclass, "floatValue", "()F");
//        jmethodID Cmethod2 = env->GetMethodID(Cclass, "charValue", "()C");
//        jmethodID Smethod2 = env->GetMethodID(Sclass, "shortValue", "()S");
//        jmethodID Bmethod2 = env->GetMethodID(Bclass, "byteValue", "()B");
//
//    }
//}boxMapping;
//
//extern "C"
//JNIEXPORT jint JNICALL
//JNI_OnLoad(JavaVM* vm, void *reserved) {
//    boxMapping.LoadMapping(vm);
//    return JNI_VERSION_1_6;
//}
//
//
//static jvalue* GetNativeParameter(JNIEnv *env,jobjectArray prams) {
//    jvalue *values = nullptr;
//    if (prams != nullptr) {
//        auto length = env->GetArrayLength(prams);
//        if (length > 0) {
//            values = new jvalue[length];
//            for (int i = 0; i < length; ++i) {
//                values[i].l = env->GetObjectArrayElement(prams, i);
//            }
//        }
//    }
//    return values;
//}
//
//extern "C"
//JNIEXPORT jobject JNICALL
//Java_org_kexie_android_hotfix_internal_ReflectEngine_invokeNonVirtual(
//        JNIEnv *env,
//        jclass type_,
//        jclass type,
//        jobject method,
//        jobjectArray pramTypes,
//        jclass returnType,
//        jobject object,
//        jobjectArray prams) {
//
//}
