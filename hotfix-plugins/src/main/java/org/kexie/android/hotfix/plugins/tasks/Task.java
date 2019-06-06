package org.kexie.android.hotfix.plugins.tasks;

import com.android.build.api.transform.TransformException;

import java.io.IOException;

interface Task<I,O> {
    O apply(Context context, I input) throws IOException, TransformException;
}
