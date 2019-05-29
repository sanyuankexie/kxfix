package org.keixe.android.hotfix.internal;

public class PatchImplTest extends Patch {


    @Override
    protected Object invokeWithId(int methodId, Object target, Object[] pram) throws Throwable {
        switch (methodId) {
            case 0: {
                invokeWithId(1, null, null);
                Class type = Class.forName("java.lang.Object");
                Object o = Intrinsics.newInstance(type, null, null);
                int a = (int) Intrinsics.access(Object.class, target, "a");
                int b = (int) Intrinsics.access(Object.class, target, "b");
                int c = a + b;
                Intrinsics.modify(Object.class, "result", target, c);
                return Intrinsics.invoke(Object.class, "add", new Class[]{
                        Integer.TYPE, Integer.TYPE
                }, true, target, new Object[]{a, b});
            }
            case 1: {
                return new Object();
            }
        }
        return null;
    }
}
