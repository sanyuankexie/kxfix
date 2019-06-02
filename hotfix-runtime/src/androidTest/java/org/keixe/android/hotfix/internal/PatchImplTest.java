package org.keixe.android.hotfix.internal;

public class PatchImplTest extends Executable {


    public PatchImplTest(HotfixExecutionEngine mDynamicExecution) {
        super(mDynamicExecution);
    }

    @Override
    Metadata onLoaded() {
        Metadata metadata = new Metadata();
        return metadata;
    }

    @Override
    Object invokeDynamicMethod(String signature, Object target, Object[] prams) throws Throwable {
        //必定生成
        ExecutionEngine executionEngine = getExecutionEngine();
        switch (signature) {
            case "java.lang.Object#toString()": {
                int a = (int) executionEngine.newInstance(Integer.class, new Class[]{Integer.TYPE}, new Object[]{1});
                int b = (int) executionEngine.newInstance(Integer.class, new Class[]{Integer.TYPE}, new Object[]{1});
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
