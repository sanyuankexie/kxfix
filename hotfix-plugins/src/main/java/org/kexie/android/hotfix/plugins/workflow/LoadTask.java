package org.kexie.android.hotfix.plugins.workflow;

import com.android.SdkConstants;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformInput;
import com.android.build.gradle.AppExtension;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;

import io.reactivex.exceptions.Exceptions;
import javassist.CtClass;
import javassist.NotFoundException;

public class LoadTask implements Workflow<Collection<TransformInput>, List<CtClass>> {
    @Override
    public ContextWith<List<CtClass>>
    apply(ContextWith<Collection<TransformInput>> input) {
        Collection<TransformInput> inputs = input.getInput();
        String[] extension = {SdkConstants.EXT_CLASS};
        List<String> classNames = new LinkedList<>();
        try {
            for (File classpath : input.getContext()
                    .getProject()
                    .getExtensions()
                    .getByType(AppExtension.class)
                    .getBootClasspath()) {
                input.getContext().getClasses()
                        .appendClassPath(classpath.getAbsolutePath());
            }
            for (TransformInput input1 : inputs) {
                for (DirectoryInput directoryInput : input1.getDirectoryInputs()) {
                    String directory = directoryInput.getFile().getAbsolutePath();
                    input.getContext().getClasses().insertClassPath(directory);
                    for (File file : FileUtils.listFiles(directoryInput.getFile(),
                            extension, true)) {
                        String className = file.getAbsolutePath().substring(
                                directory.length() + 1,
                                file.getAbsolutePath().length() -
                                        SdkConstants.DOT_CLASS.length()
                        ).replaceAll(Matcher.quoteReplacement(File.separator),
                                ".");
                        classNames.add(className);
                    }
                }
                for (JarInput jarInput : input1.getJarInputs()) {
                    input.getContext().getClasses()
                            .insertClassPath(jarInput.getFile()
                                    .getAbsolutePath());
                    JarFile jarFile = new JarFile(jarInput.getFile());
                    Enumeration<JarEntry> enumeration = jarFile.entries();
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = enumeration.nextElement();
                        String className = jarEntry.getName();
                        if (className.endsWith(SdkConstants.DOT_CLASS)) {
                            className = className.substring(0,
                                    className.length() - SdkConstants.DOT_CLASS.length()
                            ).replaceAll(Matcher.quoteReplacement("/"),
                                    ".");
                            classNames.add(className);
                        }
                    }
                }
            }
        } catch (NotFoundException | IOException e) {
            throw Exceptions.propagate(e);
        }
        List<CtClass> classes = new LinkedList<>();
        for (String className : classNames) {
            CtClass ctClass = input.getContext()
                    .getClasses()
                    .getOrNull(className);
            if (ctClass != null) {
                classes.add(ctClass);
            }
        }
        System.out.println(classes.size());
        return input.getContext().with(classes);
    }
}