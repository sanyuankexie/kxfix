package org.keixe.android.hotfix.internal;

public class PatchImplTest extends Patch {


    public PatchImplTest(PatchExecution mPatchExecution) {
        super(mPatchExecution);
    }

    @Override
    Object invokeDynamicMethod(String signature, Object target, Object[] prams) throws Throwable {
        //必定生成
        Intrinsics intrinsics = getIntrinsics();
        switch (signature) {
            case "String java.lang.Object.toString()": {
                int a = (int) intrinsics.newInstance(Integer.class, new Class[]{Integer.TYPE}, new Object[]{1});
                int b = (int) intrinsics.newInstance(Integer.class, new Class[]{Integer.TYPE}, new Object[]{1});
                int c = a + b;
                return Integer.toString(c);
            }
            default: {
                //必定生成
                throw new NoSuchMethodException();
            }
        }
    }
}
