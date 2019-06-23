package org.kexie.android.hotfix.plugins;

import org.junit.Test;

import java.lang.reflect.Method;

import io.reactivex.functions.Function;

public class JavaTest implements Function<Object,Object> {


    static void test2(int[][] x, Object[][] x2) {

    }

    @Override
    public Object apply(Object object) {
        return null;
    }


    @Test
    public void test() throws Throwable {

    }

    private static String makeMethodSignature(Method method) {
        StringBuilder builder = new StringBuilder("(");
        Class[] pramTypes = method.getParameterTypes();
        Class returnType = method.getReturnType();
        for (Class pramType : pramTypes) {
            if (pramType.isPrimitive()) {
                makePrimitiveSignature(builder, pramType);
            } else {
                makeReferenceSignature(builder, pramType);
            }
        }
        builder.append(')');
        makeReferenceSignature(builder, returnType);
        return builder.toString();
    }

    private static void makePrimitiveSignature(StringBuilder builder, Class pramType) {
        char c;
        if (pramType == Integer.TYPE) {
            c = 'I';
        } else if (pramType == Void.TYPE) {
            c = 'V';
        } else if (pramType == Boolean.TYPE) {
            c = 'Z';
        } else if (pramType == Byte.TYPE) {
            c = 'B';
        } else if (pramType == Character.TYPE) {
            c = 'C';
        } else if (pramType == Short.TYPE) {
            c = 'S';
        } else if (pramType == Double.TYPE) {
            c = 'D';
        } else if (pramType == Float.TYPE) {
            c = 'F';
        } else /* if (d == Long.TYPE) */ {
            c = 'J';
        }
        builder.append(c);
    }

    private static void makeReferenceSignature(StringBuilder builder, Class pramType) {
        while (pramType.isArray()) {
            builder.append('[');
            Class next = pramType.getComponentType();
            if (next == null) {
                break;
            }
            pramType = next;
        }
        if (pramType.isPrimitive()) {
            makePrimitiveSignature(builder, pramType);
        } else {
            builder.append("L").append(pramType.getName().replace('.', '/'))
                    .append(';');
        }
    }
}
