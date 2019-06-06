package org.kexie.android.hotfix.plugins.tasks;

import com.android.build.api.transform.TransformException;

import java.io.IOException;

/**
 * 明确职责边界
 * 一个Task对应一种任务
 * 有输入和输出
 */
interface Task<I,O> {
    O apply(Context context, I input) throws IOException, TransformException;
}
