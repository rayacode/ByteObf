/*  ByteObf: A Java Bytecode Obfuscator
 *  Copyright (C) 2021 vimasig
 *  Copyright (C) 2025 Mohammad Ali Solhjoo mohammadalisolhjoo@live.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package codes.rayacode.ByteObf.obfuscator;

import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.TransformManager;
import codes.rayacode.ByteObf.obfuscator.utils.StreamUtils;
import codes.rayacode.ByteObf.obfuscator.utils.StringUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import codes.rayacode.ByteObf.obfuscator.utils.model.CustomClassWriter;
import codes.rayacode.ByteObf.obfuscator.utils.model.ResourceWrapper;
import codes.rayacode.ByteObf.obfuscator.utils.ExclusionManager;
import codes.rayacode.ByteObf.obfuscator.utils.AsyncFileLogger;
import javafx.concurrent.Task;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ByteObf extends Task<Void> {

    private final ByteObfConfig config;
    private final Consumer<String> logConsumer;
    private final Consumer<String> errConsumer;
    private ExclusionManager exclusionManager;

    
    private static final LogLevel MIN_LOG_LEVEL_FOR_UI = LogLevel.INFO; 

    public ByteObf(ByteObfConfig config, Consumer<String> logConsumer, Consumer<String> errConsumer) {
        this.config = config;
        this.logConsumer = logConsumer;
        this.errConsumer = errConsumer;
    }

    private final List<ClassNode> classes = Collections.synchronizedList(new ArrayList<>());
    private final List<ResourceWrapper> resources = Collections.synchronizedList(new ArrayList<>());
    private ClassLoader classLoader;
    private TransformManager transformHandler;

    @Override
    public Void call() throws Exception {
        AsyncFileLogger.start();

        try {
            final long startTime = System.currentTimeMillis();

            this.exclusionManager = new ExclusionManager(this.config.getExclude());

            if (!this.config.getInput().exists()) throw new FileNotFoundException("Cannot find input");
            if (!this.config.getInput().isFile()) throw new IllegalArgumentException("Received input is not a file");

            String inputExtension = this.config.getInput().getName().substring(this.config.getInput().getName().lastIndexOf(".") + 1).toLowerCase();
            if ("jar".equals(inputExtension)) {
                log(LogLevel.INFO, "Processing JAR input...");
                final Set<String> entryNames = new HashSet<>();
                try (var jarInputStream = new ZipInputStream(Files.newInputStream(this.config.getInput().toPath()))) {
                    ZipEntry zipEntry;
                    while ((zipEntry = jarInputStream.getNextEntry()) != null) {
                        if (entryNames.add(zipEntry.getName())) {
                            if (zipEntry.getName().endsWith(".class")) {
                                if (classes.size() == Integer.MAX_VALUE) throw new IllegalArgumentException("Maximum class count exceeded");
                                ClassReader reader = new ClassReader(jarInputStream);
                                ClassNode classNode = new ClassNode();
                                reader.accept(classNode, 0);
                                classes.add(classNode);
                            } else {
                                if (resources.size() == Integer.MAX_VALUE) throw new IllegalArgumentException("Maximum resource count exceeded");
                                resources.add(new ResourceWrapper(zipEntry, StreamUtils.readAll(jarInputStream)));
                            }
                        } else {
                            log(LogLevel.WARN, "Skipping duplicate resource/class entry: %s", zipEntry.getName());
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Unsupported file extension: " + inputExtension);
            }

            if (classes.isEmpty()) throw new IllegalArgumentException("Received input does not look like a proper JAR file");

            final var libs = this.getConfig().getLibraries();
            URL[] urls = new URL[libs.size() + 1];
            urls[libs.size()] = this.config.getInput().toURI().toURL();
            for (int i = 0; i < libs.size(); i++) {
                urls[i] = new File(libs.get(i)).toURI().toURL();
            }
            this.classLoader = new URLClassLoader(urls);

            log(LogLevel.INFO, "Transforming...");
            this.transformHandler = new TransformManager(this);
            transformHandler.transformAll();

            log(LogLevel.INFO, "Writing...");
            final Set<String> writtenClassNames = ConcurrentHashMap.newKeySet();
            List<ClassNode> uniqueClasses = this.classes.stream()
                    .filter(cn -> !"module-info".equals(cn.name) && writtenClassNames.add(cn.name))
                    .collect(Collectors.toList());
            if (this.classes.size() != uniqueClasses.size()) {
                log(LogLevel.INFO, "Removed %d duplicate class entries before writing.", this.classes.size() - uniqueClasses.size());
            }

            try (var out = new JarOutputStream(Files.newOutputStream(this.config.getOutput()))) {
                final Set<String> writtenResourceNames = new HashSet<>();
                resources.stream()
                        .filter(resourceWrapper -> !resourceWrapper.getZipEntry().isDirectory())
                        .filter(resourceWrapper -> resourceWrapper.getBytes() != null)
                        .filter(resourceWrapper -> writtenResourceNames.add(resourceWrapper.getZipEntry().getName()))
                        .forEach(resourceWrapper -> {
                            try {
                                out.putNextEntry(new JarEntry(resourceWrapper.getZipEntry().getName()));
                                StreamUtils.copy(new ByteArrayInputStream(resourceWrapper.getBytes()), out);
                            } catch (Throwable e) {
                                err(LogLevel.ERROR, "Cannot write resource: %s. Reason: %s", resourceWrapper.getZipEntry().getName(), e.getMessage());
                                logStackTrace(e);
                            }
                        });

                int total = uniqueClasses.size();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        log(LogLevel.INFO, "Obfuscation cancelled.");
                        break;
                    }
                    ClassNode classNode = uniqueClasses.get(i);
                    byte[] bytes;
                    try {
                        var classWriter = new CustomClassWriter(this, ClassWriter.COMPUTE_FRAMES, this.classLoader);
                        classNode.accept(classWriter);
                        bytes = classWriter.toByteArray();
                    } catch (Throwable t) {
                        err(LogLevel.WARN, "Could not process class %s with COMPUTE_FRAMES, falling back to COMPUTE_MAXS. Error: %s", classNode.name, t.getMessage());
                        logStackTrace(t);
                        try {
                            var maxsWriter = new CustomClassWriter(this, ClassWriter.COMPUTE_MAXS, this.classLoader);
                            classNode.accept(maxsWriter);
                            bytes = maxsWriter.toByteArray();
                        } catch (Throwable t2) {
                            err(LogLevel.ERROR, "Failed to process class %s even with COMPUTE_MAXS. Skipping. Final error: %s", classNode.name, t2.getMessage());
                            logStackTrace(t2);
                            continue;
                        }
                    }

                    try {
                        out.putNextEntry(new JarEntry(classNode.name + ".class"));
                        out.write(bytes);
                    } catch (Throwable e) {
                        err(LogLevel.ERROR, "Cannot write class: %s. Reason: %s", classNode.name, e.getMessage());
                        logStackTrace(e);
                    }
                    updateProgress(i + 1, total);
                }

                transformHandler.getClassTransformers().stream()
                        .filter(ClassTransformer::isEnabled)
                        .forEach(classTransformer -> classTransformer.transformOutput(out));
            }

            log(LogLevel.INFO, "Obfuscation process complete.");
            final String timeElapsed = new DecimalFormat("##.###").format(((double) System.currentTimeMillis() - startTime) / 1000D);
            log(LogLevel.INFO, "Done. Took %ss", timeElapsed);
            final String oldSize = StringUtils.getConvertedSize(this.config.getInput().length());
            final String newSize = StringUtils.getConvertedSize(this.config.getOutput().toFile().length());
            log(LogLevel.INFO, "File size changed from %s to %s", oldSize, newSize);
        } catch (Throwable e) {
            err(LogLevel.ERROR, "A critical error occurred during the obfuscation process: %s", e.getMessage());
            logStackTrace(e);
            updateMessage("Obfuscation failed. Please check the console for errors.");
            throw new Exception(e);
        } finally {
            AsyncFileLogger.stop();
        }
        return null;
    }

    public boolean isExcluded(ClassTransformer classTransformer, final String str) {
        String transformerSpecificRule = classTransformer.getName() + ":" + str;
        if (exclusionManager.isExcluded(transformerSpecificRule)) {
            return true;
        }
        return exclusionManager.isExcluded(str);
    }

    public TransformManager getTransformHandler() {
        return transformHandler;
    }
    public List<ClassNode> getClasses() {
        return classes;
    }
    public List<ResourceWrapper> getResources() {
        return resources;
    }
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    public ByteObfConfig getConfig() {
        return config;
    }

    public void log(LogLevel level, String format, Object... args) {
        String message = String.format("[%s] %s", level.name(), String.format(format, args));
        
        if (level.ordinal() >= MIN_LOG_LEVEL_FOR_UI.ordinal()) {
            updateMessage(message); 
            logConsumer.accept(message); 
        }
        AsyncFileLogger.log(message); 
    }

    public void err(LogLevel level, String format, Object... args) {
        String message = String.format("[%s] [ERROR] %s", level.name(), String.format(format, args));
        
        updateMessage(message);
        errConsumer.accept(message);
        AsyncFileLogger.log(message); 
    }

    public void logStackTrace(Throwable t) {
        AsyncFileLogger.logStackTrace(t);
    }

    public enum LogLevel {
        DEBUG, 
        INFO,
        WARN,
        ERROR
    }
}