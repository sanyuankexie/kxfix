package org.keixe.android.hotfix.internal;

public class PatchImplTest extends Patch {


    @Override
    protected Object apply(int methodId, Object target, Object[] pram) throws Throwable {
        switch (methodId) {
            case 0: {
                int a = (int) Intrinsics.access(Object.class, target, "a");
                int b = (int) Intrinsics.access(Object.class, target, "b");
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
