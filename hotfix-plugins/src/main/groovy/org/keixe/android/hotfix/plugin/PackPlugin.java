package org.keixe.android.hotfix.plugin;

import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class PackPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin(PatchPlugin.class)) {
            throw new IllegalStateException("请注释或移除'apply plugin : hotfix-patch'后重试");
        }
        if (!project.getPlugins().hasPlugin(AJXPlugin.class)) {
            project.getPlugins().apply(AJXPlugin.class);
        }
    }
}
