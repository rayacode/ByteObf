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
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import codes.rayacode.ByteObf.obfuscator.utils.model.ResourceWrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public abstract class ClassTransformer implements Opcodes {

    private final ByteObf byteObf;
    private final String text;
    private final ByteObfCategory category;
    protected final Random random = new Random();

    public ClassTransformer(ByteObf byteObf, String text, ByteObfCategory category) {
        this.byteObf = byteObf;
        this.text = text;
        this.category = category;
    }

    public void pre() {}
    public void post() {}
    public void transformClass(ClassNode classNode) {}
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {}
    public void transformField(ClassNode classNode, FieldNode fieldNode) {}
    public void transformResource(ResourceWrapper resource) {}
    public boolean transformOutput(ClassNode classNode) { return true; }
    public void transformOutput(JarOutputStream jarOutputStream) {}
    public void transformClassWriter(ClassWriter classWriter) {}

    public final ByteObf getBozar() {
        return byteObf;
    }

    public abstract ByteObfConfig.EnableType getEnableType();

    public boolean isEnabled() {
        return this.getEnableType().isEnabled().get();
    }

    public String getText() {
        return this.text;
    }

    public final String getName() {
        return this.getClass().getSimpleName();
    }

    public ByteObfCategory getCategory() {
        return category;
    }

    protected boolean isSuperPresent(ClassNode classNode) {
        return classNode.superName != null && !classNode.superName.equals("java/lang/Object");
    }

    protected ClassNode getSuper(ClassNode classNode) {
        return this.findClass(classNode.superName);
    }

    protected ClassNode findClass(String className) {
        return this.getBozar().getClasses().stream().filter(cn -> cn.name.equals(className)).findFirst().orElse(null);
    }

    protected List<ClassNode> findClasses(List<String> classNames) {
        return this.getBozar().getClasses().stream()
                .filter(cn -> classNames.contains(cn.name))
                .collect(Collectors.toList());
    }

    protected List<ClassNode> getSuperHierarchy(ClassNode base) {
        return getSuperHierarchy(base, null);
    }

    protected List<ClassNode> getSuperHierarchy(ClassNode base, ClassNode to) {
        var superList = new ArrayList<ClassNode>();
        while (base != null) {
            superList.add(base);
            if(base.equals(to)) break;
            base = this.getSuper(base);
        } return superList;
    }
}