package org.keixe.android.hotfix.plugins


import com.android.build.gradle.AppPlugin
import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

final class HotfixPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(PatchPlugin)) {
            throw new IllegalStateException("请注释或移除\"apply plugin : 'org.kexie.patch'\"后重试")
        }
        if (project.plugins.hasPlugin(AppPlugin)) {
            if (!project.plugins.hasPlugin(AJXPlugin)) {
                project.plugins.apply(AJXPlugin)
            }
        }
    }
}
