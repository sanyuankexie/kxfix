package org.keixe.android.hotfix.plugins

import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin

import org.gradle.api.Plugin
import org.gradle.api.Project

final class ReleasePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(PatchPlugin.class)) {
            throw new IllegalStateException("请注释或移除'apply plugin : hotfix-patch'后重试")
        }
        if (!project.plugins.hasPlugin(AJXPlugin.class)) {
            project.plugins.apply(AJXPlugin.class)
        }
    }
}
