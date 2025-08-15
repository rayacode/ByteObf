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
import java.util.function.Function;
import java.util.stream.Collectors;

public class MethodRenamerTransformer extends RenamerTransformer {

    private record ClassMethodWrapper(ClassNode classNode, MethodNode methodNode) {
        @Override
        public String toString() {
            return this.classNode.name + "." + this.methodNode.name + methodNode.desc;
        }
    }

    private final List<String> whitelistedMethods = new ArrayList<>();
    private final Map<String, List<ClassNode>> childrenMap = new ConcurrentHashMap<>();
    private boolean hierarchyBuilt = false;

    public MethodRenamerTransformer(ByteObf byteObf) {
        super(byteObf, "Rename", ByteObfCategory.STABLE);
        whitelistedMethods.addAll(List.of(
                "main([Ljava/lang/String;)V",
                "premain(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V",
                "agentmain(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V",

                
                "toString()Ljava/lang/String;",
                "clone()Ljava/lang/Object;",
                "equals(Ljava/lang/Object;)Z",
                "hashCode()I"

                
        ));
    }

    /**
     * **PERFORMANCE FIX:**
     * Build a map of the entire class hierarchy *once*. This prevents the transformer
     * from repeatedly scanning the entire class list for every single method.
     */
    private void buildHierarchy() {
        if (hierarchyBuilt) return;

        
        
        if (classMap.isEmpty()) {
            this.getByteObf().log("Building class map for fast renamer lookups...");
            classMap.putAll(this.getByteObf().getClasses().stream()
                    .collect(Collectors.toConcurrentMap(cn -> cn.name, Function.identity(), (a, b) -> a)));
            this.getByteObf().log("Class map built.");
        }

        
        this.getByteObf().log("Building class hierarchy for renamer...");
        for (ClassNode classNode : this.getByteObf().getClasses()) {
            if (classNode.superName != null) {
                childrenMap.computeIfAbsent(classNode.superName, k -> new ArrayList<>()).add(classNode);
            }
            for (String interfaceName : classNode.interfaces) {
                childrenMap.computeIfAbsent(interfaceName, k -> new ArrayList<>()).add(classNode);
            }
        }
        this.getByteObf().log("Class hierarchy built.");
        hierarchyBuilt = true;
    }

    @Override
    public void pre() {
        buildHierarchy();
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        
        if ((classNode.access & ACC_ANNOTATION) != 0) return;
        if (methodNode.name.contains("<")) return;
        if (whitelistedMethods.contains(methodNode.name + methodNode.desc)) return;

        final String mapName = ASMUtils.getName(classNode, methodNode);

        if ((methodNode.access & ACC_STATIC) != 0 || (methodNode.access & ACC_PRIVATE) != 0) {
            
            this.registerMap(mapName);
        } else {
            final Set<ClassMethodWrapper> sameMethods = new HashSet<>();
            ClassNode superClass = classNode;

            
            if (!this.canAccessAllInterfaces(classNode)) return;
            var superInterfaces = new ArrayList<ClassNode>();

            
            while (true) {
                
                boolean isSuperPresent = this.isSuperPresent(superClass);

                if ((superClass = this.getSuper(superClass)) == null) {
                    if (isSuperPresent) return;
                    break;
                }

                
                MethodNode overriddenMethod = findOverriddenMethod(superClass, methodNode);
                if (overriddenMethod != null) {
                    getSuperHierarchy(classNode, superClass).forEach(c -> sameMethods.add(new ClassMethodWrapper(c, overriddenMethod)));
                }

                
                superInterfaces.addAll(this.getInterfaces(superClass));
                if (!this.canAccessAllInterfaces(superClass)) return;
            }

            
            superInterfaces.forEach(cn -> cn.methods.stream()
                    .filter(method -> (methodNode.access & ACC_STATIC) == 0 && (methodNode.access & ACC_PRIVATE) == 0)
                    .filter(method -> method.name.equals(methodNode.name))
                    .filter(method -> method.desc.equals(methodNode.desc))
                    .findFirst()
                    .ifPresentOrElse(method -> this.getInterfaceHierarchyFromSuper(classNode, cn).forEach(c -> sameMethods.add(new ClassMethodWrapper(c, method))), () -> {
                    })
            );

            boolean methodOverrideFound = sameMethods.size() > 0;
            if (methodOverrideFound) {
                
                final String targetMap = sameMethods.stream()
                        .filter(cmw -> this.isMapRegistered(cmw.toString()))
                        .findFirst()
                        .map(cmw -> this.map.get(cmw.toString()))
                        .orElse(this.registerMap(mapName));

                
                sameMethods.stream()
                        .map(ClassMethodWrapper::toString)
                        .forEach(s -> this.registerMap(s, targetMap));
            } else {
                var map = this.isMapRegistered(mapName) ? this.map.get(mapName) : this.registerMap(mapName);
                this.getUpperSuperHierarchy(classNode).forEach(cn -> this.registerMap(ASMUtils.getName(cn, methodNode), map));

                
                this.getUpperInterfaceHierarchy(classNode).forEach(cn -> {
                    this.registerMap(ASMUtils.getName(cn, methodNode), map);
                    this.getUpperSuperHierarchy(cn).forEach(cn2 -> this.registerMap(ASMUtils.getName(cn2, methodNode), map));
                });
            }
        }
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getRename() != this.getEnableType().type(), ByteObfConfig.ByteObfOptions.RenameOption.OFF);
    }

    /**
     * @return Looks for non-static & non-private methods in classNode and returns the first method with the same method description and name
     */
    private static MethodNode findOverriddenMethod(ClassNode classNode, MethodNode targetMethod) {
        return classNode.methods.stream()
                .filter(methodNode -> (methodNode.access & ACC_STATIC) == 0 && (methodNode.access & ACC_PRIVATE) == 0)
                .filter(methodNode -> methodNode.name.equals(targetMethod.name))
                .filter(methodNode -> methodNode.desc.equals(targetMethod.desc))
                .findFirst()
                .orElse(null);
    }

    

    /**
     * @return all available interfaces and sub interfaces in the classNode
     */
    private List<ClassNode> getInterfaces(ClassNode classNode) {
        var interfaces = this.findClasses(classNode.interfaces);
        var tmpArr = interfaces.stream()
                .map(this::getInterfaces)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        interfaces.addAll(tmpArr);
        return interfaces;
    }

    /**
     * **PERFORMANCE FIX:**
     * Uses the pre-computed children map for fast lookups instead of scanning all classes.
     * @return all available class nodes that extends given class node
     */
    private List<ClassNode> getUpperSuperHierarchy(ClassNode classNode) {
        List<ClassNode> upperClasses = new ArrayList<>();
        List<ClassNode> children = childrenMap.getOrDefault(classNode.name, Collections.emptyList());
        upperClasses.addAll(children);
        children.stream()
                .map(this::getUpperSuperHierarchy)
                .flatMap(Collection::stream)
                .forEach(upperClasses::add);
        return upperClasses;
    }


    /**
     * **PERFORMANCE FIX:**
     * Uses the pre-computed children map for fast lookups instead of scanning all classes.
     * @return all available class nodes that implements given class node
     */
    private List<ClassNode> getUpperInterfaceHierarchy(ClassNode classNode) {
        return getUpperSuperHierarchy(classNode); 
    }

    /**
     * Used to check if there is an inaccessible library exists
     *
     * @return the state of whether all classNode interfaces are loaded as ClassNode objects
     */
    private boolean canAccessAllInterfaces(ClassNode classNode) {
        var interfaces = this.findClasses(classNode.interfaces);
        boolean b = interfaces.size() == classNode.interfaces.size();
        if (!b) return false;
        return interfaces.size() == 0 || interfaces.stream().allMatch(this::canAccessAllInterfaces);
    }

    /**
     * @return all available interfaces and sub interfaces in the classNode and its super classes
     */
    private List<ClassNode> getInterfaceHierarchyFromSuper(ClassNode base, ClassNode target) {
        var list = new ArrayList<ClassNode>();
        do {
            var l = this.getInterfaces(base);
            if (!l.isEmpty()) {
                List<ClassNode> tmp = new ArrayList<>();
                for (ClassNode iface : l) {
                    tmp.add(iface);
                    if (iface.equals(target)) {
                        list.addAll(tmp);
                        return list;
                    }
                }
            }

            list.add(base);
            base = this.getSuper(base);
        } while (base != null);
        return new ArrayList<>();
    }
}