package org.kexie.gradle.hotfix.plugins;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.Project;
import org.kexie.gradle.hotfix.workflow.Context;
import org.kexie.gradle.hotfix.workflow.Workflow;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Android Studio Plugin 可以完全使用Java来进行开发
 * 而Java是强类型非动态的
 * 特别适合用来构建复杂臃肿但又可靠的的系统
 * 所以这里没有使用groovy
 */
public class PatchTransform extends Transform {

    private final Context context;

    PatchTransform(Project project) {
        context = Context.make(project);
    }

    @Override
    public String getName() {
        return "patch";
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws IOException {
        transformInvocation.getOutputProvider().deleteAll();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        Workflow.doWorks(context, inputs);
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

}