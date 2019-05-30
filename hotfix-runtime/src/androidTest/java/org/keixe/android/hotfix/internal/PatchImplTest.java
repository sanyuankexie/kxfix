package org.keixe.android.hotfix.internal;

public class PatchImplTest extends Patch {


    public PatchImplTest(PatchExecution mPatchExecution) {
        super(mPatchExecution);
    }

    @Override
    Object invokeDynamicMethod(String signature, Object target, Object[] prams) throws Throwable {
        Intrinsics intrinsics = getIntrinsics();
        switch (signature) {
            case "String java.lang.Object.toString()": {
                return "123";
            }
            default: {
                //必定插入
                throw new NoSuchMethodException();
            }
        }
    }
}
