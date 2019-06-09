package org.kexie.android.hotfix.internal;

public class PatchImplTest extends Executable {


    public PatchImplTest(DynamicExecutionEngine mDynamicExecution) {
        super(mDynamicExecution);
    }

    @Override
    protected void onLoad(Metadatas metadata){metadata.addMethod(-864333218,"org.kexie.android.hotfix.sample.MainActivity","onCreate",new java.lang.String[]{"android.os.Bundle"});}

    @Override
    protected java.lang.Object invokeDynamicMethod(int id, java.lang.Object target, java.lang.Object[] prams)throws java.lang.Throwable {
        org.kexie.android.hotfix.internal.ExecutionEngine executionEngine = this.getExecutionEngine();
        switch (id) {
            case 1 : {
                return null;
            }
            default: {
                throw new java.lang.NoSuchMethodException();
            }
        }
    }

}
