package org.kexie.android.hotfix.plugins;

import org.junit.Test;

import io.reactivex.functions.Function;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class JavaTest implements Function<Object,Object> {

    public static void data(int xczxc, int as) {
        test2(xczxc, xczxc);
    }

    static void test2(int x,int x2)
    {

    }

    @Override
    public Object apply(Object object) {
        return null;
    }


    @Test
    public void test() throws Throwable {
        ClassPool classPool = ClassPool.getDefault();
        CtClass clazz = classPool.get(JavaTest.class.getName());
        clazz.defrost();
        for (CtMethod method : clazz.getDeclaredMethods()) {
            method.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("test2")) {
                        m.replace("System.out.print(\"\"+$class);$_ = null;");
                    }
                }
            });
        }
        clazz.writeFile("C:\\Users\\Luke\\Desktop\\OpenSource\\kxfix\\test");
    }

}
