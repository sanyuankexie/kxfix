package org.kexie.gradle.hotfix.workflow;

import java.io.File;

/**
 * 需要操作文件的任务
 */
abstract class FileWork<I,O> extends Work<I,O> {
    protected String getOutput(Context context) {
        return context.getProject().getBuildDir()
                .getAbsolutePath() + File.separator +
                "outputs" + File.separator +
                "hotfix" + File.separator;
    }
}
