package org.kexie.gradle.hotfix.plugins;

import com.android.build.gradle.AppPlugin;
import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BenchmarkPlugin implements Plugin<Project> {

    @SuppressWarnings("NullableProblems")
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin(PatchPlugin.class)) {
            throw new IllegalStateException("请注释或移除\"apply plugin " +
                    ": 'org.kexie.gradle.hotfix.patch'\"后重试");
        }
        if (project.getPlugins().hasPlugin(AppPlugin.class)
                && !project.getPlugins().hasPlugin(AJXPlugin.class)) {
            project.getPlugins().apply(AJXPlugin.class);
        }
    }
}
