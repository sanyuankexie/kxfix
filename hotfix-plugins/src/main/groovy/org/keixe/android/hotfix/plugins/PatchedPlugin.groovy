package org.keixe.android.hotfix.plugins

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * groovy和java的语法很大程度上是相似的
 * 这里为了执行效率考虑不使用groovy风格的写法
 *
 * 代码里的提示信息不会使用英文，原因有三:
 * 1.我英文贼差,我知道英文差的人看英文文档时的痛苦
 * 2.热更新一般是中国人用才会用的,没必要脱裤子放屁显得很高端
 * 3.我很傲慢,不考虑国际化
 */
final class PatchedPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(AppPlugin.class)) {
            if (project.plugins.hasPlugin("android-aspectjx")) {
                throw new IllegalStateException("请注释或移除'apply plugin : android-aspectjx'后重试")
            }
            AppExtension android = project.extensions.getByType(AppExtension.class)
            PatchedTransform transform = new PatchedTransform(project.logger)
            android.registerTransform(transform)
        }
    }
}