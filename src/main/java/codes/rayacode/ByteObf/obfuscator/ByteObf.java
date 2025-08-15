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
        try {
            final long startTime = System.currentTimeMillis();

            if (!this.config.getInput().exists()) throw new FileNotFoundException("Cannot find input");
            if (!this.config.getInput().isFile()) throw new IllegalArgumentException("Received input is not a file");

            String inputExtension = this.config.getInput().getName().substring(this.config.getInput().getName().lastIndexOf(".") + 1).toLowerCase();
            if ("jar".equals(inputExtension)) {
                log("Processing JAR input...");
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
                            log("Skipping duplicate resource/class entry: %s", zipEntry.getName());
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

            log("Transforming...");
            this.transformHandler = new TransformManager(this);
            transformHandler.transformAll();

            log("Writing...");
            final Set<String> writtenClassNames = ConcurrentHashMap.newKeySet();
            List<ClassNode> uniqueClasses = this.classes.stream()
                    .filter(cn -> !"module-info".equals(cn.name) && writtenClassNames.add(cn.name))
                    .collect(Collectors.toList());
            if (this.classes.size() != uniqueClasses.size()) {
                log("Removed %d duplicate class entries before writing.", this.classes.size() - uniqueClasses.size());
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
                                err("Cannot write resource: %s. Reason: %s", resourceWrapper.getZipEntry().getName(), e.getMessage());
                            }
                        });

                int total = uniqueClasses.size();
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        log("Obfuscation cancelled.");
                        break;
                    }
                    ClassNode classNode = uniqueClasses.get(i);
                    byte[] bytes;
                    try {
                        var classWriter = new CustomClassWriter(this, ClassWriter.COMPUTE_FRAMES, this.classLoader);
                        classNode.accept(classWriter);
                        bytes = classWriter.toByteArray();
                    } catch (Throwable t) {
                        try {
                            err("Could not process class %s with COMPUTE_FRAMES, falling back to COMPUTE_MAXS. Error: %s", classNode.name, t.getMessage());
                            var maxsWriter = new CustomClassWriter(this, ClassWriter.COMPUTE_MAXS, this.classLoader);
                            classNode.accept(maxsWriter);
                            bytes = maxsWriter.toByteArray();
                        } catch (Throwable t2) {
                            err("Failed to process class %s even with COMPUTE_MAXS. Skipping. Final error: %s", classNode.name, t2.getMessage());
                            t2.printStackTrace();
                            continue;
                        }
                    }

                    try {
                        out.putNextEntry(new JarEntry(classNode.name + ".class"));
                        out.write(bytes);
                    } catch (Throwable e) {
                        err("Cannot write class: %s. Reason: %s", classNode.name, e.getMessage());
                        e.printStackTrace();
                    }
                    updateProgress(i + 1, total);
                }

                transformHandler.getClassTransformers().stream()
                        .filter(ClassTransformer::isEnabled)
                        .forEach(classTransformer -> classTransformer.transformOutput(out));
            }

            log("Obfuscation process complete.");
            final String timeElapsed = new DecimalFormat("##.###").format(((double) System.currentTimeMillis() - startTime) / 1000D);
            log("Done. Took %ss", timeElapsed);
            final String oldSize = StringUtils.getConvertedSize(this.config.getInput().length());
            final String newSize = StringUtils.getConvertedSize(this.config.getOutput().toFile().length());
            log("File size changed from %s to %s", oldSize, newSize);
        } catch (Throwable e) {
            err("A critical error occurred during the obfuscation process: %s", e.getMessage());
            e.printStackTrace();
            updateMessage("Obfuscation failed. Please check the console for errors.");
            throw new Exception(e);
        }
        return null;
    }

    public boolean isExcluded(ClassTransformer classTransformer, final String str) {
        final String classNameWithDots = str.replace('/', '.');
        return this.getConfig().getExclude().lines().anyMatch(line -> {
            String cleanLine = line.trim();
            if (cleanLine.isEmpty()) {
                return false;
            }

            String targetTransformer = null;
            if (cleanLine.contains(":")) {
                targetTransformer = cleanLine.split(":")[0];
                cleanLine = cleanLine.substring((targetTransformer + ":").length());
            }

            if (targetTransformer != null && (classTransformer == null || !classTransformer.getName().equals(targetTransformer))) {
                return false;
            }

            if (cleanLine.endsWith(".**")) {
                String basePackage = cleanLine.substring(0, cleanLine.length() - 2);
                return classNameWithDots.startsWith(basePackage);
            } else {
                return classNameWithDots.equals(cleanLine);
            }
        });
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

    public void log(String format, Object... args) {
        String message = "[ByteObf] " + String.format(format, args);
        updateMessage(message);
        logConsumer.accept(message);
    }

    public void err(String format, Object... args) {
        String message = "[ByteObf] [ERROR] " + String.format(format, args);
        updateMessage(message);
        errConsumer.accept(message);
        try (FileWriter fw = new FileWriter("D:\\projects\\java\\javafx\\ByteObf\\log.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}