package org.kexie.android.hotfix.plugins;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PatchPlugin implements Plugin<Project> {

    @SuppressWarnings("NullableProblems")
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin(HotfixPlugin.class)) {
            throw new IllegalStateException("请注释或移除\"apply plugin : 'org.kexie.hotfix'\"后重试");
        }
        if (project.getPlugins().hasPlugin(AppPlugin.class)) {
            AppExtension android = project.getExtensions().getByType(AppExtension.class);
            PatchTransform transform = new PatchTransform(project);
            android.registerTransform(transform);

        }
    }
}
