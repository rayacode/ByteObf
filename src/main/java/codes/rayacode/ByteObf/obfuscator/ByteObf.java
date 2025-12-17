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
 *  along with this program.  If not, see <https:
 */

package codes.rayacode.ByteObf.obfuscator;

import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.TransformManager;
import codes.rayacode.ByteObf.obfuscator.utils.AsyncFileLogger;
import codes.rayacode.ByteObf.obfuscator.utils.ExclusionManager;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ByteObf extends Task<Void> {

    private final ByteObfConfig config;
    private final Consumer<String> logConsumer;
    private final Consumer<String> errConsumer;
    private ExclusionManager exclusionManager;

    private static final LogLevel MIN_LOG_LEVEL_FOR_UI = LogLevel.INFO;
    
    private static final int IO_BUFFER_SIZE = 64 * 1024 * 1024;

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
            printHardwareInfo();
            final long startTime = System.nanoTime();

            this.exclusionManager = new ExclusionManager(this.config.getExclude());

            if (!this.config.getInput().exists()) throw new FileNotFoundException("Cannot find input");
            if (!this.config.getInput().isFile()) throw new IllegalArgumentException("Received input is not a file");

            String inputExtension = this.config.getInput().getName().substring(this.config.getInput().getName().lastIndexOf(".") + 1).toLowerCase();

            
            if ("jar".equals(inputExtension)) {
                log(LogLevel.INFO, "Processing JAR input with Parallel I/O...");
                final Set<String> entryNames = ConcurrentHashMap.newKeySet();

                
                try (ZipFile zipFile = new ZipFile(this.config.getInput())) {
                    List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
                    AtomicInteger processedCount = new AtomicInteger(0);
                    int totalEntries = entries.size();

                    log(LogLevel.INFO, "Reading and parsing %d entries in parallel...", totalEntries);

                    entries.parallelStream().forEach(zipEntry -> {
                        try {
                            if (entryNames.add(zipEntry.getName())) {
                                try (InputStream is = zipFile.getInputStream(zipEntry)) {
                                    byte[] data = StreamUtils.readAll(is, (int) zipEntry.getSize());

                                    if (zipEntry.getName().endsWith(".class")) {
                                        ClassReader reader = new ClassReader(data);
                                        ClassNode classNode = new ClassNode();
                                        reader.accept(classNode, 0);
                                        classes.add(classNode);
                                    } else {
                                        resources.add(new ResourceWrapper(zipEntry, data));
                                    }
                                }
                            } else {
                                log(LogLevel.WARN, "Skipping duplicate resource/class entry: %s", zipEntry.getName());
                            }
                            
                            if (processedCount.incrementAndGet() % 500 == 0) {
                                updateMessage("Reading: " + (processedCount.get() * 100 / totalEntries) + "%");
                            }
                        } catch (IOException e) {
                            err(LogLevel.ERROR, "Failed to read entry %s: %s", zipEntry.getName(), e.getMessage());
                        }
                    });
                }
            } else {
                throw new IllegalArgumentException("Unsupported file extension: " + inputExtension);
            }
            

            if (classes.isEmpty()) throw new IllegalArgumentException("Received input does not look like a proper JAR file");

            log(LogLevel.INFO, "Loaded %d classes and %d resources.", classes.size(), resources.size());

            
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

            
            log(LogLevel.INFO, "Preparing output...");

            final Set<String> writtenClassNames = ConcurrentHashMap.newKeySet();
            List<ClassNode> uniqueClasses = this.classes.stream()
                    .filter(cn -> !"module-info".equals(cn.name) && writtenClassNames.add(cn.name))
                    .collect(Collectors.toList());

            
            
            log(LogLevel.INFO, "Compiling %d classes in parallel (High CPU Usage)...", uniqueClasses.size());

            AtomicInteger compiledCount = new AtomicInteger(0);
            List<CompiledEntry> compiledClasses = uniqueClasses.parallelStream()
                    .map(classNode -> {
                        try {
                            byte[] bytes;
                            try {
                                var classWriter = new CustomClassWriter(this, ClassWriter.COMPUTE_FRAMES, this.classLoader);
                                classNode.accept(classWriter);
                                bytes = classWriter.toByteArray();
                            } catch (Throwable t) {
                                
                                var maxsWriter = new CustomClassWriter(this, ClassWriter.COMPUTE_MAXS, this.classLoader);
                                classNode.accept(maxsWriter);
                                bytes = maxsWriter.toByteArray();
                            }
                            if (compiledCount.incrementAndGet() % 200 == 0) {
                                updateProgress(compiledCount.get(), uniqueClasses.size());
                            }
                            return new CompiledEntry(classNode.name + ".class", bytes);
                        } catch (Throwable e) {
                            err(LogLevel.ERROR, "Failed to compile class %s: %s", classNode.name, e.getMessage());
                            logStackTrace(e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            
            log(LogLevel.INFO, "Writing to disk with %dMB buffer...", IO_BUFFER_SIZE / 1024 / 1024);

            try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(this.config.getOutput()), IO_BUFFER_SIZE);
                 JarOutputStream out = new JarOutputStream(bos)) {

                
                final Set<String> writtenResourceNames = new HashSet<>();
                for (ResourceWrapper resource : resources) {
                    if (!resource.getZipEntry().isDirectory() && resource.getBytes() != null
                            && writtenResourceNames.add(resource.getZipEntry().getName())) {
                        try {
                            out.putNextEntry(new JarEntry(resource.getZipEntry().getName()));
                            out.write(resource.getBytes());
                            out.closeEntry();
                        } catch (IOException e) {
                            err(LogLevel.ERROR, "Cannot write resource: %s. Reason: %s", resource.getZipEntry().getName(), e.getMessage());
                        }
                    }
                }

                
                for (CompiledEntry entry : compiledClasses) {
                    try {
                        out.putNextEntry(new JarEntry(entry.name));
                        out.write(entry.data);
                        out.closeEntry();
                    } catch (IOException e) {
                        err(LogLevel.ERROR, "Cannot write class: %s", entry.name);
                    }
                }

                
                transformHandler.getClassTransformers().stream()
                        .filter(ClassTransformer::isEnabled)
                        .forEach(classTransformer -> classTransformer.transformOutput(out));
            }
            

            log(LogLevel.INFO, "Obfuscation process complete.");
            long endTime = System.nanoTime();
            final String timeElapsed = new DecimalFormat("##.###").format((endTime - startTime) / 1_000_000_000.0);
            log(LogLevel.INFO, "Done. Took %ss", timeElapsed);

            final String oldSize = StringUtils.getConvertedSize(this.config.getInput().length());
            final String newSize = StringUtils.getConvertedSize(this.config.getOutput().toFile().length());
            log(LogLevel.INFO, "File size changed from %s to %s", oldSize, newSize);

        } catch (Throwable e) {
            err(LogLevel.ERROR, "A critical error occurred: %s", e.getMessage());
            logStackTrace(e);
            updateMessage("Obfuscation failed.");
            throw new Exception(e);
        } finally {
            AsyncFileLogger.stop();
        }
        return null;
    }

    
    private record CompiledEntry(String name, byte[] data) {}

    private void printHardwareInfo() {
        String cpuName = getCpuName();
        int cores = Runtime.getRuntime().availableProcessors();
        long memory = Runtime.getRuntime().maxMemory();

        log(LogLevel.INFO, "------------------------------------------------");
        log(LogLevel.INFO, "System Hardware:");
        log(LogLevel.INFO, "CPU Model: " + cpuName);
        log(LogLevel.INFO, "Cores:     " + cores + " Logical Processors");
        log(LogLevel.INFO, "Max Mem:   " + StringUtils.getConvertedSize(memory));
        log(LogLevel.INFO, "------------------------------------------------");
    }

    private static String getCpuName() {
        String os = System.getProperty("os.name").toLowerCase();
        String command = "";
        if (os.contains("win")) {
            command = "wmic cpu get name";
        } else if (os.contains("mac")) {
            command = "sysctl -n machdep.cpu.brand_string";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            String[] cmd = {"/bin/sh", "-c", "grep -m 1 'model name' /proc/cpuinfo | awk -F: '{print $2}'"};
            return runCommand(cmd).trim();
        } else {
            return "Unknown CPU (OS: " + os + ")";
        }
        return runCommand(command.split(" ")).trim();
    }

    private static String runCommand(String[] command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equalsIgnoreCase("Name")) {
                        output.append(line);
                        break;
                    }
                }
                return output.toString();
            }
        } catch (Exception e) {
            return "Unknown CPU";
        }
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