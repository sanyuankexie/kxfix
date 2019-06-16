//
// Created by Luke on 2019/5/29.
//

#include <jni.h>
#include <unordered_map>
#include <functional>

using namespace std;

static JavaVM *javaVM = nullptr;
static jmethodID hashCode = nullptr;

using UnBoxer = function<void(JNIEnv *, jobject, jvalue *)>;
using Invoker = function<jobject(JNIEnv *, jclass, jmethodID, jobject, jvalue *)>;

struct HashCode {
    size_t operator()(const jclass &k) const noexcept {
        JNIEnv *env = nullptr;
        javaVM->GetEnv((void **) (&env), JNI_VERSION_1_6);
        return (size_t) env->CallIntMethod(k, hashCode);
    }
};

struct Equals {
    bool operator()(const jclass &k1, const jclass &k2) const {
        JNIEnv *env = nullptr;
        javaVM->GetEnv((void **) (&env), JNI_VERSION_1_6);
        return env->IsSameObject(k1, k2);
    }
};

static unordered_map<jclass, Invoker, HashCode, Equals> invokeMapping;
static unordered_map<jclass, UnBoxer, HashCode, Equals> unBoxMapping;

static jclass javaLangObjectClass = nullptr;

static void LoadMapping(JNIEnv *env) {

    function<jclass(const char *)> findClass = [env](const char *name) {
        return (jclass) env->NewGlobalRef(env->FindClass(name));
    };

    javaLangObjectClass = findClass("java/lang/Object");
    hashCode = env->GetMethodID(javaLangObjectClass, "hashCode", "()I");

    jclass zWrapper = findClass("java/lang/Boolean");
    jclass iWrapper = findClass("java/lang/Integer");
    jclass jWrapper = findClass("java/lang/Long");
    jclass dWrapper = findClass("java/lang/Double");
    jclass fWrapper = findClass("java/lang/Float");
    jclass cWrapper = findClass("java/lang/Character");
    jclass sWrapper = findClass("java/lang/Short");
    jclass bWrapper = findClass("java/lang/Byte");

    jmethodID zBox = env->GetStaticMethodID(zWrapper, "valueOf", "(Z)Ljava/lang/Boolean;");
    jmethodID iBox = env->GetStaticMethodID(iWrapper, "valueOf", "(I)Ljava/lang/Integer;");
    jmethodID jBox = env->GetStaticMethodID(jWrapper, "valueOf", "(J)Ljava/lang/Long;");
    jmethodID dBox = env->GetStaticMethodID(dWrapper, "valueOf", "(D)Ljava/lang/Double;");
    jmethodID fBox = env->GetStaticMethodID(fWrapper, "valueOf", "(F)Ljava/lang/Float;");
    jmethodID cBox = env->GetStaticMethodID(cWrapper, "valueOf", "(C)Ljava/lang/Character;");
    jmethodID sBox = env->GetStaticMethodID(sWrapper, "valueOf", "(S)Ljava/lang/Short;");
    jmethodID bBox = env->GetStaticMethodID(bWrapper, "valueOf", "(B)Ljava/lang/Byte;");

    jmethodID zUnBox = env->GetMethodID(zWrapper, "booleanValue", "()Z");
    jmethodID iUnBox = env->GetMethodID(iWrapper, "intValue", "()I");
    jmethodID jUnBox = env->GetMethodID(jWrapper, "longValue", "()J");
    jmethodID dUnBox = env->GetMethodID(dWrapper, "doubleValue", "()D");
    jmethodID fUnBox = env->GetMethodID(fWrapper, "floatValue", "()F");
    jmethodID cUnBox = env->GetMethodID(cWrapper, "charValue", "()C");
    jmethodID sUnBox = env->GetMethodID(sWrapper, "shortValue", "()S");
    jmethodID bUnBox = env->GetMethodID(bWrapper, "byteValue", "()B");

    jmethodID returnType = env->GetMethodID(env->FindClass("java/lang/reflect/Method"),
                                            "getReturnType", "()Ljava/lang/Class;");

    function<jclass(jclass, jmethodID)> getRealType =
            [env, returnType](jclass clazz, jmethodID methodId) {
                jobject method = env->ToReflectedMethod(clazz, methodId, JNI_FALSE);
                jobject type = env->CallObjectMethod(method, returnType);
                return (jclass) env->NewGlobalRef(type);
            };
    jclass zClass = getRealType(zWrapper, zUnBox);
    jclass iClass = getRealType(iWrapper, iUnBox);
    jclass jClass = getRealType(jWrapper, jUnBox);
    jclass dClass = getRealType(dWrapper, dUnBox);
    jclass fClass = getRealType(fWrapper, fUnBox);
    jclass cClass = getRealType(cWrapper, cUnBox);
    jclass sClass = getRealType(sWrapper, sUnBox);
    jclass bClass = getRealType(bWrapper, bUnBox);

    unBoxMapping[zClass] = [zUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->z = env->CallBooleanMethod(obj, zUnBox);
    };
    invokeMapping[zClass] = [zWrapper, zBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jboolean r = env->CallNonvirtualBooleanMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(zWrapper, zBox, r);
    };

    unBoxMapping[iClass] = [iUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->i = env->CallIntMethod(obj, iUnBox);
    };
    invokeMapping[iClass] = [iWrapper, iBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jint r = env->CallNonvirtualIntMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(iWrapper, iBox, r);
    };

    unBoxMapping[jClass] = [jUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->j = env->CallLongMethod(obj, jUnBox);
    };
    invokeMapping[jClass] = [jWrapper, jBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jlong r = env->CallNonvirtualLongMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(jWrapper, jBox, r);
    };

    unBoxMapping[dClass] = [dUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->d = env->CallDoubleMethod(obj, dUnBox);
    };
    invokeMapping[dClass] = [dWrapper, dBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jdouble r = env->CallNonvirtualDoubleMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(dWrapper, dBox, r);
    };

    unBoxMapping[fClass] = [fUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->f = env->CallFloatMethod(obj, fUnBox);
    };
    invokeMapping[fClass] = [fWrapper, fBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jfloat r = env->CallNonvirtualFloatMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(fWrapper, fBox, r);
    };

    unBoxMapping[cClass] = [cUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->c = env->CallCharMethod(obj, cUnBox);
    };
    invokeMapping[cClass] = [cWrapper, cBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jchar r = env->CallNonvirtualCharMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(cWrapper, cBox, r);
    };

    unBoxMapping[sClass] = [sUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->s = env->CallShortMethod(obj, sUnBox);
    };
    invokeMapping[sClass] = [sWrapper, sBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jshort r = env->CallNonvirtualShortMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(sWrapper, sBox, r);
    };

    unBoxMapping[bClass] = [bUnBox](JNIEnv *env, jobject obj, jvalue *value) {
        value->b = env->CallByteMethod(obj, bUnBox);
    };
    invokeMapping[bClass] = [bWrapper, bBox](JNIEnv *env, jclass type, jmethodID id, jobject obj,
                                             jvalue *values) {
        jbyte r = env->CallNonvirtualByteMethodA(obj, type, id, values);
        return env->CallStaticObjectMethod(bWrapper, bBox, r);
    };
}

static jobject invokeNonVirtual(
        JNIEnv *env,
        jclass type,
        jmethodID methodId,
        jclass returnType,
        jobject object,
        jvalue *values) {
    auto it = invokeMapping.find(returnType);
    if (it != invokeMapping.end()) {
        return it->second(env, type, methodId, object, values);
    } else if (env->IsAssignableFrom(javaLangObjectClass, returnType)) {
        return env->CallNonvirtualObjectMethodA(object, type, methodId, values);
    } else {
        env->CallNonvirtualVoidMethodA(object, type, methodId, values);
        return nullptr;
    }
}

static void CheckUnBox(JNIEnv *env, jclass clazz, jobject obj, jvalue *out) {
    auto it = unBoxMapping.find(clazz);
    if (it != unBoxMapping.end()) {
        it->second(env, obj, out);
    } else {
        out->l = obj;
    }
}

static jvalue *GetNativeParameter(
        JNIEnv *env,
        jobjectArray pramTypes,
        jobjectArray prams) {
    jvalue *values = nullptr;
    if (pramTypes != nullptr) {
        auto length = env->GetArrayLength(pramTypes);
        if (length > 0) {
            values = new jvalue[length];
            for (int i = 0; i < length; ++i) {
                auto clazz = (jclass) env->GetObjectArrayElement(pramTypes, i);
                jobject obj = env->GetObjectArrayElement(prams, i);
                CheckUnBox(env, clazz, obj, &values[i]);
            }
        }
    }
    return values;
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env = nullptr;
    javaVM->GetEnv((void **) (&env), JNI_VERSION_1_6);
    LoadMapping(env);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_kexie_android_hotfix_internal_ReflectEngine_invokeNonVirtual(
        JNIEnv *env,
        jclass _,
        jclass type,
        jobject method,
        jobjectArray pramTypes,
        jclass returnType,
        jobject object,
        jobjectArray prams) {
    jmethodID methodId = env->FromReflectedMethod(method);
    jvalue *values = GetNativeParameter(env, pramTypes, prams);
    jobject result = invokeNonVirtual(env, type, methodId, returnType, object, values);
    delete values;
    jthrowable ex = env->ExceptionOccurred();
    if (ex != nullptr) {
        env->Throw(ex);
        env->ExceptionClear();
        return nullptr;
    }
    return result;
}