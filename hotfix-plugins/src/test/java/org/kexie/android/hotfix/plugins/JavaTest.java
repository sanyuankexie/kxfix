package org.kexie.android.hotfix.plugins;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import javassist.expr.Instanceof;
import javassist.expr.MethodCall;
import javassist.expr.NewArray;
import javassist.expr.NewExpr;

public class JavaTest {

    public static Collection<Integer> data(int xczxc,int as) {
        return Arrays.asList(xczxc, xczxc, xczxc, xczxc);
    }

    @Test
    public void test() throws Throwable {
        ClassPool classPool = ClassPool.getDefault();
        classPool.get(getClass().getName()).getDeclaredMethods()[0].instrument(new ExprEditor(){
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    System.out.println(m.getMethod());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void edit(Cast c) throws CannotCompileException {
                super.edit(c);
            }

            @Override
            public void edit(Handler h) throws CannotCompileException {
                try {
                    System.out.println(h.getType());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void edit(NewExpr e) throws CannotCompileException {
                super.edit(e);
            }

            @Override
            public void edit(NewArray a) throws CannotCompileException {
                super.edit(a);
            }

            @Override
            public void edit(Instanceof i) throws CannotCompileException {
                super.edit(i);
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    System.out.println(f.getField());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                super.edit(c);
            }
        });
    }
}
