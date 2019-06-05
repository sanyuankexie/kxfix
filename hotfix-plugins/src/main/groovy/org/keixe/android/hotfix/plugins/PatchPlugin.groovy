package org.keixe.android.hotfix.plugins

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

final class PatchPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(HotfixPlugin)) {
            throw new IllegalStateException("请注释或移除\"apply plugin : 'org.kexie.hotfix'\"后重试")
        }
        if (project.plugins.hasPlugin(AppPlugin)) {
            AppExtension android = project.extensions.getByType(AppExtension)
            PatchTransform transform = new PatchTransform(project)
            android.registerTransform(transform)
        }
    }
}