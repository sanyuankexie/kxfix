package org.kexie.android.hotfix.plugins.workflow;

import java.io.File;

abstract class TempWorkflow<I,O> extends FileWorkflow<I,O> {

    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "tmp" + File.separator;
    }
}
