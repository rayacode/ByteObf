/*  ByteObf: A Java Bytecode Obfuscator
 *  Copyright (C) 2021 vimasig
 *  Copyright (C) [2025] Mohammad Ali Solhjoo mohammadalisolhjoo@live.com
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

package codes.rayacode.ByteObf.obfuscator.transformer;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.*;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer.ClassRenamerTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer.FieldRenamerTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer.MethodRenamerTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.DummyClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.TextInsideClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.UnusedStringTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.ZipCommentTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TransformManager {

    private final ByteObf byteObf;
    private final List<ClassTransformer> classTransformers = new ArrayList<>();

    public TransformManager(ByteObf byteObf) {
        this.byteObf = byteObf;
        this.classTransformers.addAll(getTransformers().stream()
            .map(clazz -> {
                try {
                    return clazz.getConstructor(ByteObf.class).newInstance(this.byteObf);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
    }

    public static List<Class<? extends ClassTransformer>> getTransformers() {
        final var transformers = new ArrayList<Class<? extends ClassTransformer>>();

        transformers.add(ClassRenamerTransformer.class);
        transformers.add(FieldRenamerTransformer.class);
        transformers.add(MethodRenamerTransformer.class);

        // TODO: AntiDebugTransformer
        transformers.add(LightControlFlowTransformer.class);
        transformers.add(HeavyControlFlowTransformer.class);
        transformers.add(ConstantTransformer.class);
        transformers.add(LocalVariableTransformer.class);
        transformers.add(LineNumberTransformer.class);
        transformers.add(SourceFileTransformer.class);
        transformers.add(DummyClassTransformer.class);
        transformers.add(TextInsideClassTransformer.class);
        transformers.add(UnusedStringTransformer.class);
        transformers.add(ZipCommentTransformer.class);
        transformers.add(CrasherTransformer.class);
        transformers.add(ShuffleTransformer.class);
        transformers.add(InnerClassTransformer.class);

        return transformers;
    }

    public static ClassTransformer createTransformerInstance(Class<? extends ClassTransformer> transformerClass) {
        try {
            return transformerClass.getConstructor(ByteObf.class).newInstance((Object)null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void transformAll() {
        // Apply renamer transformers
        var map = new HashMap<String, String>();
        this.classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .filter(ct -> ct instanceof RenamerTransformer)
                .map(ct -> (RenamerTransformer)ct)
                .forEach(crt -> {
                    this.byteObf.log("Applying renamer %s", crt.getName());
                    this.byteObf.getClasses().forEach(classNode -> this.transform(classNode, crt.getClass()));
                    this.byteObf.getResources().forEach(crt::transformResource);
                    map.putAll(crt.map);
                });

        // Remap classes
        if(this.byteObf.getConfig().getOptions().getRename() != ByteObfConfig.ByteObfOptions.RenameOption.OFF) {
            this.byteObf.log("Applying renamer...");
            var reMapper = new SimpleRemapper(map);
            for (int i = 0; i < this.byteObf.getClasses().size(); i++) {
                ClassNode classNode = this.byteObf.getClasses().get(i);
                ClassNode remappedClassNode = new ClassNode();
                ClassRemapper adapter = new ClassRemapper(remappedClassNode, reMapper);
                classNode.accept(adapter);
                this.byteObf.getClasses().set(i, remappedClassNode);
            }
        }

        // Pre
        this.classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .forEach(ClassTransformer::pre);

        // Transform all classes
        this.classTransformers.stream()
            .filter(ClassTransformer::isEnabled)
            .filter(ct -> !(ct instanceof RenamerTransformer))
            .forEach(ct -> {
                this.byteObf.log("Applying %s", ct.getName());
                this.byteObf.getClasses().forEach(classNode -> this.transform(classNode, ct.getClass()));
                this.byteObf.getResources().forEach(ct::transformResource);
        });

        // Post
        this.classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .forEach(ClassTransformer::post);
    }

    public void transform(ClassNode classNode, Class<? extends ClassTransformer> transformerClass) {
        ClassTransformer classTransformer = this.getClassTransformer(transformerClass);
        if(this.byteObf.isExcluded(classTransformer, ASMUtils.getName(classNode))) return;

        classTransformer.transformClass(classNode);
        classNode.fields.stream()
                .filter(fieldNode -> !this.byteObf.isExcluded(classTransformer, ASMUtils.getName(classNode, fieldNode)))
                .forEach(fieldNode -> classTransformer.transformField(classNode, fieldNode));
        classNode.methods.stream()
                .filter(methodNode -> !this.byteObf.isExcluded(classTransformer, ASMUtils.getName(classNode) + "." + methodNode.name + "()"))
                .forEach(methodNode -> {
            AbstractInsnNode[] insns = methodNode.instructions.toArray().clone();
            classTransformer.transformMethod(classNode, methodNode);

            // Revert changes if method size is invalid
            if (!ASMUtils.isMethodSizeValid(methodNode)) {
                this.byteObf.log("Cannot apply \"%s\" on \"%s\" due to low method capacity", classTransformer.getName(), classNode.name + "." + methodNode.name + methodNode.desc);
                methodNode.instructions = ASMUtils.arrayToList(insns);
            }
        });
    }

    @SuppressWarnings("unchecked") // Checked using stream
    public <T extends ClassTransformer> T getClassTransformer(Class<T> transformerClass) {
        if(transformerClass == null)
            throw new NullPointerException("transformerClass cannot be null");
        return (T) this.classTransformers.stream()
                .filter(ct -> ct.getClass().equals(transformerClass))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Cannot find transformerClass: " + transformerClass.getName()));
    }

    public List<ClassTransformer> getClassTransformers() {
        return classTransformers;
    }
}