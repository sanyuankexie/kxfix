package org.kexie.android.hotfix.plugins.tasks;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.File;

import javassist.ClassPool;

public class Context {
    final ClassPool mClassPool;
    final Project mProject;
    final Logger mLogger;
    final String mBaseWorkDir;
    final AppExtension mAndroid;

    public Context(Project project) {
        mProject = project;
        mLogger = project.getLogger();
        mAndroid = project.getExtensions().getByType(AppExtension.class);
        mClassPool = new ClassPool();
        mBaseWorkDir = mProject.getBuildDir()
                .getAbsolutePath() + File.separator +
                "outputs" + File.separator +
                "hotfix" + File.separator;
    }
}
