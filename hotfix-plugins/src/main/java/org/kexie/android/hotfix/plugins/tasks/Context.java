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
    final String mWorkDir;
    private final String mClassOutputDir;
    final AppExtension mAndroid;

    public Context(Project project) {
        mProject = project;
        mLogger = project.getLogger();
        mAndroid = project.getExtensions().getByType(AppExtension.class);
        mClassPool = new ClassPool();
        mWorkDir = mProject.getBuildDir()
                .getAbsolutePath() + File.separator +
                "output" + File.separator +
                "hotfix" + File.separator;
        mClassOutputDir = mWorkDir + "classes" + File.separator;
    }
}
