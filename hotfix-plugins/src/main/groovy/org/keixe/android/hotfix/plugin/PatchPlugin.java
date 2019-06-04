package org.keixe.android.hotfix.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

final class PatchPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin(PackPlugin.class)) {
            throw new IllegalStateException("请注释或移除'apply plugin : hotfix-pack'后重试");
        }
        if (project.getPlugins().hasPlugin(AppPlugin.class)) {
            AppExtension android = project.getExtensions().getByType(AppExtension.class);
            PatchTransform transform = new PatchTransform(project);
            android.registerTransform(transform);
        }
    }
}