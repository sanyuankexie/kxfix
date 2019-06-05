package org.kexie.android.hotfix.internal;

public class PatchImplTest extends Executable {


    public PatchImplTest(HotfixExecutionEngine mDynamicExecution) {
        super(mDynamicExecution);
    }

    @Override
    protected Metadata onLoaded() {
        Metadata metadata = new Metadata();
        return metadata;
    }

    @Override
    protected Object invokeDynamicMethod(int id, Object target, Object[] prams) throws Throwable {
        //必定生成
        ExecutionEngine executionEngine = getExecutionEngine();
        switch (id) {
            case 1: {
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
