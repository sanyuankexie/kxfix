package org.kexie.android.hotfix.plugins;

import org.junit.Test;

import io.reactivex.functions.Function;
import javassist.ClassPool;
import javassist.CtMethod;

public class JavaTest implements Function<Object,Object> {

    public static void data(int xczxc, int as) {
        test2(xczxc, xczxc);
    }

    static void test2(int x, int x2) {

    }

    @Override
    public Object apply(Object object) {
        return null;
    }


    @Test
    public void test() throws Throwable {
        for (CtMethod method : ClassPool.getDefault().get(JavaTest.class.getName()).getDeclaredMethods()) {
            System.out.println(method.getSignature());
        }
    }
}
