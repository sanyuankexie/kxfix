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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import io.reactivex.exceptions.Exceptions;
import javassist.CtClass;
import javassist.NotFoundException;

public class LoadTask extends Workflow<Collection<TransformInput>, List<CtClass>> {
    @Override
    ContextWith<List<CtClass>> doWork(ContextWith<Collection<TransformInput>> context) {
        Collection<TransformInput> inputs = context.getData();
        String[] extension = {SdkConstants.EXT_CLASS};
        List<String> classNames = new LinkedList<>();
        try {
            for (File classpath : context
                    .getProject()
                    .getExtensions()
                    .getByType(AppExtension.class)
                    .getBootClasspath()) {
                context.getClasses()
                        .appendClassPath(classpath.getAbsolutePath());
            }
            for (TransformInput input1 : inputs) {
                for (DirectoryInput directoryInput : input1.getDirectoryInputs()) {
                    String directory = directoryInput.getFile().getAbsolutePath();
                    context.getClasses().insertClassPath(directory);
                    FileUtils.listFiles(directoryInput.getFile(), extension, true)
                            .stream()
                            .map(file -> file.getAbsolutePath().substring(
                                    directory.length() + 1,
                                    file.getAbsolutePath().length() -
                                            SdkConstants.DOT_CLASS.length()
                            ).replaceAll(Matcher.quoteReplacement(File.separator),
                                    "."))
                            .forEach(classNames::add);
                }
                for (JarInput jarInput : input1.getJarInputs()) {
                    context.getClasses().insertClassPath(jarInput.getFile()
                            .getAbsolutePath());
                    JarFile jarFile = new JarFile(jarInput.getFile());
                    jarFile.stream().map(JarEntry::getName)
                            .filter(className -> className.endsWith(SdkConstants.DOT_CLASS))
                            .map(className -> className.substring(0,
                                    className.length() - SdkConstants.DOT_CLASS.length()
                            ).replaceAll(Matcher.quoteReplacement("/"),
                                    "."))
                            .forEach(classNames::add);
                }
            }
        } catch (NotFoundException | IOException e) {
            throw Exceptions.propagate(e);
        }
        List<CtClass> classes = classNames
                .stream()
                .map(className -> context
                .getClasses()
                .getOrNull(className))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
        return context.with(classes);
    }
}