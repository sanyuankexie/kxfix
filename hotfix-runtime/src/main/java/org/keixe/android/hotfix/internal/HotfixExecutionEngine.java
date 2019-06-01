package org.keixe.android.hotfix.internal;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.InitializerSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.AnnotatedElement;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import androidx.annotation.Keep;

/**
 * 热更新执行引擎
 */
@Keep
final class HotfixExecutionEngine
        extends ReflectExecutionEngine
        implements DynamicExecutionEngine,
        EntryPointHooker {

    static final HotfixExecutionEngine INSTANCE = new HotfixExecutionEngine();

    private static final AtomicReferenceFieldUpdater<HotfixExecutionEngine, Executable> sExecutableUpdater
            = AtomicReferenceFieldUpdater
            .newUpdater(HotfixExecutionEngine.class, Executable.class, "mExecutable");

    private volatile Executable mExecutable;

    @Override
    public final boolean isExecuteThat(Executable executable) {
        return executable == mExecutable;
    }

    @Override
    public final void apply(Executable executable) {
        sExecutableUpdater.set(this, executable);
    }

    @Override
    public final Object hook(ProceedingJoinPoint joinPoint) throws Throwable {
        Executable executable = mExecutable;
        if (executable != null && executable.isEntryPoint(getEntryPoint(joinPoint))) {
            CodeSignature signature = (CodeSignature) joinPoint.getSignature();
            try {
                return executable.receiveInvoke(
                        signature.getDeclaringType(),
                        signature.getName(),
                        signature.getParameterTypes(),
                        joinPoint.getTarget(),
                        joinPoint.getArgs()
                );
            } catch (Exception e) {
                e.printStackTrace();
                //补丁出错,降级处理↓
            }
        }
        return joinPoint.proceed();
    }

    private static AnnotatedElement getEntryPoint(JoinPoint joinPoint) {
        AnnotatedElement marker = null;
        Signature signature = joinPoint.getSignature();
        if (signature instanceof ConstructorSignature) {
            marker = ((ConstructorSignature) signature).getConstructor();
        } else if (signature instanceof MethodSignature) {
            marker = ((MethodSignature) signature).getMethod();
        } else if (signature instanceof InitializerSignature) {
            marker = signature.getDeclaringType();
        }
        return marker;
    }

    @Override
    public final Object invoke(
            Class type,
            String name,
            Class[] pramsTypes,
            Object target,
            Object[] prams)
            throws Throwable {
        Executable executable = mExecutable;
        if (executable == null) {
            return super.invoke(type, name, pramsTypes, target, prams);
        } else {
            return executable.receiveInvoke(type, name, pramsTypes, target, prams);
        }
    }

    @Override
    public final Object access(
            Class type,
            String name,
            Object target)
            throws Throwable {
        Executable executable = mExecutable;
        if (executable == null) {
            return super.access(type, name, target);
        } else {
            return executable.receiveAccess(type, name, target);
        }
    }

    @Override
    public final void modify(
            Class type,
            String name,
            Object target,
            Object newValue)
            throws Throwable {
        Executable executable = mExecutable;
        if (executable == null) {
            super.modify(type, name, target, newValue);
        } else {
            executable.receiveModify(type, name, target, newValue);
        }
    }
}