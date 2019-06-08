package org.kexie.android.hotfix.plugins.workflow;

import java.io.File;

abstract class FileWorkflow<I,O> extends Workflow<I,O> {
    protected String getOutput(Context context) {
        return context.getProject().getBuildDir()
                .getAbsolutePath() + File.separator +
                "outputs" + File.separator +
                "hotfix" + File.separator;
    }
}
