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
import org.objectweb.asm.tree.FieldNode;

public class FieldRenamerTransformer extends RenamerTransformer {

    public FieldRenamerTransformer(ByteObf byteObf) {
        super(byteObf, "Rename", ByteObfCategory.STABLE);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        // Map all fields in this class node and its super classes (if not mapped)
        getSuperHierarchy(classNode)
                .forEach(cn -> cn.fields.stream()
                        .filter(fieldNode -> !this.getByteObf().isExcluded(this, ASMUtils.getName(cn, fieldNode)))
                        .filter(fieldNode -> !this.isMapRegistered(getFieldMapFormat(cn, fieldNode)))
                        .forEach(fieldNode -> this.registerMap(getFieldMapFormat(cn, fieldNode)))
                );

        // Apply map to upper classes if a mapping is applied to a subclass
        // So our mapper can rename references that access subfields from upper classes
        getSuperHierarchy(classNode)
                .forEach(cn -> cn.fields.stream()
                        .filter(fieldNode -> this.isMapRegistered(getFieldMapFormat(cn, fieldNode)))
                        .filter(fieldNode -> !this.isMapRegistered(getFieldMapFormat(classNode, fieldNode)))
                        .forEach(fieldNode -> this.registerMap(getFieldMapFormat(classNode, fieldNode), this.map.get(getFieldMapFormat(cn, fieldNode))))
                );
    }

    private static String getFieldMapFormat(ClassNode classNode, FieldNode fieldNode) {
        return classNode.name + "." + fieldNode.name;
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getRename() != this.getEnableType().type(), ByteObfConfig.ByteObfOptions.RenameOption.OFF);
    }
}