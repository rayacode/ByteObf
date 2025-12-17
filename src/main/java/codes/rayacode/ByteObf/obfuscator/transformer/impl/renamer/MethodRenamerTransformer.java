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

package codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.RenamerTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MethodRenamerTransformer extends RenamerTransformer {

    private final Set<String> whitelistedSignatures = ConcurrentHashMap.newKeySet();
    private final Map<String, List<ClassNode>> childrenMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> interfaceAccessCache = new ConcurrentHashMap<>();


    public MethodRenamerTransformer(ByteObf byteObf) {
        super(byteObf, "Rename", ByteObfCategory.STABLE);
        whitelistedSignatures.addAll(List.of(
                "main([Ljava/lang/String;)V",
                "premain(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V",
                "agentmain(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V",
                "toString()Ljava/lang/String;",
                "clone()Ljava/lang/Object;",
                "equals(Ljava/lang/Object;)Z",
                "hashCode()I"
        ));
    }

    @Override
    public void pre() {
        this.getByteObf().log(ByteObf.LogLevel.INFO, "Building class hierarchy for Method Renaming...");

        this.getByteObf().getClasses().parallelStream().forEach(classNode -> {
            if (classNode.superName != null) {
                childrenMap.computeIfAbsent(classNode.superName, k -> Collections.synchronizedList(new ArrayList<>())).add(classNode);
            }
            for (String interfaceName : classNode.interfaces) {
                childrenMap.computeIfAbsent(interfaceName, k -> Collections.synchronizedList(new ArrayList<>())).add(classNode);
            }
        });
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if ((classNode.access & ACC_ANNOTATION) != 0) return;
        if (methodNode.name.startsWith("<")) return;


        if (whitelistedSignatures.contains(methodNode.name + methodNode.desc)) return;

        final String mapName = ASMUtils.getName(classNode, methodNode);


        if ((methodNode.access & ACC_STATIC) != 0 || (methodNode.access & ACC_PRIVATE) != 0) {
            this.registerMap(mapName);
        } else {

            if (!this.canAccessAllInterfaces(classNode)) return;


            if (isLibraryOverride(classNode, methodNode)) return;


            String newName = this.isMapRegistered(mapName) ? this.map.get(mapName) : this.registerMap(mapName);


            propagateRename(classNode, methodNode, newName);
        }
    }

    /**
     * Propagates a method rename to all children classes to ensure overrides remain valid.
     * Uses a breadth-first traversal.
     */
    private void propagateRename(ClassNode root, MethodNode method, String newName) {
        Queue<ClassNode> queue = new ArrayDeque<>();
        queue.add(root);

        Set<String> visited = new HashSet<>();
        visited.add(root.name);

        while (!queue.isEmpty()) {
            ClassNode current = queue.poll();
            String key = ASMUtils.getName(current, method);


            if (!this.isMapRegistered(key)) {
                this.registerMap(key, newName);
            }

            List<ClassNode> children = childrenMap.get(current.name);
            if (children != null) {

                synchronized (children) {
                    for (ClassNode child : children) {
                        if (visited.add(child.name)) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
    }

    private boolean isLibraryOverride(ClassNode classNode, MethodNode methodNode) {
        String current = classNode.superName;


        while (current != null) {
            ClassNode parent = findClass(current);
            if (parent == null) {


                return true;
            }
            for (MethodNode m : parent.methods) {
                if (m.name.equals(methodNode.name) && m.desc.equals(methodNode.desc)) return true;
            }
            current = parent.superName;
        }
        return false;
    }

    /**
     * FIXED: This method triggered the "Recursive update" exception.
     * Now uses get() -> compute -> put() pattern to avoid holding Map locks during recursion.
     */
    private boolean canAccessAllInterfaces(ClassNode classNode) {

        Boolean cached = interfaceAccessCache.get(classNode.name);
        if (cached != null) return cached;


        boolean result = true;
        var interfaces = this.findClasses(classNode.interfaces);


        if (interfaces.size() != classNode.interfaces.size()) {
            result = false;
        } else {

            result = interfaces.stream().allMatch(this::canAccessAllInterfaces);
        }


        interfaceAccessCache.put(classNode.name, result);
        return result;
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getRename() != this.getEnableType().type(), ByteObfConfig.ByteObfOptions.RenameOption.OFF);
    }

    private record ClassMethodWrapper(ClassNode classNode, MethodNode methodNode) {
        @Override
        public String toString() {
            return this.classNode.name + "." + this.methodNode.name + methodNode.desc;
        }
    }
}