package org.kexie.gradle.hotfix.workflow;

import java.io.File;

/**
 * 中间过程任务
 */
abstract class TempWork<I,O> extends FileWork<I,O> {
    @Override
    protected String getOutput(Context context) {
        return super.getOutput(context) + "tmp" + File.separator;
    }
}
