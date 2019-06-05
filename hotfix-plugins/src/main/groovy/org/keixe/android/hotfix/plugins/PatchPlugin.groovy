package org.keixe.android.hotfix.plugins

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

final class PatchPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(ReleasePlugin.class)) {
            throw new IllegalStateException("请注释或移除'apply plugin : hotfix-release'后重试")
        }
        if (project.plugins.hasPlugin(AppPlugin.class)) {
            AppExtension android = project.extensions.getByType(AppExtension.class)
            PatchTransform transform = new PatchTransform(project)
            android.registerTransform(transform)
        }
    }
}