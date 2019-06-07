package org.kexie.android.hotfix.plugins.workflow;

import com.android.SdkConstants;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;

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

import javassist.CtClass;
import javassist.NotFoundException;

public class LoadingTask implements Task<Collection<TransformInput>, List<CtClass>> {
    @Override
    public List<CtClass> apply(Context context, Collection<TransformInput> inputs)
            throws IOException, TransformException {
        String[] extension = {SdkConstants.EXT_CLASS};
        List<String> classNames = new LinkedList<>();
        try {
            for (File classpath : context.mAndroid.getBootClasspath()) {
                context.mClassPool.appendClassPath(classpath.getAbsolutePath());
            }
            for (TransformInput input : inputs) {
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    String directory = directoryInput.getFile().getAbsolutePath();
                    context.mClassPool.insertClassPath(directory);
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
                for (JarInput jarInput : input.getJarInputs()) {
                    context.mClassPool.insertClassPath(jarInput.getFile().getAbsolutePath());
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
        } catch (NotFoundException e) {
            throw new TransformException(e);
        }
        List<CtClass> classes = new LinkedList<>();
        for (String className : classNames) {
            CtClass ctClass = context.mClassPool.getOrNull(className);
            if (ctClass != null) {
                classes.add(ctClass);
            }
        }
        return classes;
    }
}
