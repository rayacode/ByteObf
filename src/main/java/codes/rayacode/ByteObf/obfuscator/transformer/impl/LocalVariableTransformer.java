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

package codes.rayacode.ByteObf.obfuscator.transformer.impl;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class LocalVariableTransformer extends ClassTransformer {

    public LocalVariableTransformer(ByteObf byteObf) {
        super(byteObf, "Local variables", ByteObfCategory.STABLE);
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        switch (this.getByteObf().getConfig().getOptions().getLocalVariables()) {
            case DELETE -> {
                methodNode.localVariables = null;
                methodNode.parameters = null;
            }
            case OBFUSCATE -> {
                final String s = "\u6000";
                if(methodNode.localVariables != null) methodNode.localVariables.forEach(localVar -> {
                    localVar.name = s;
                    localVar.signature = null;
                    localVar.desc = String.format("L%s;", s);
                });
                if(methodNode.parameters != null) methodNode.parameters.forEach(parameterNode -> parameterNode.name = s);
            }
        }
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> ((List<?>)this.getEnableType().type()).contains(this.getByteObf().getConfig().getOptions().getLocalVariables()),
                List.of(ByteObfConfig.ByteObfOptions.LocalVariableOption.DELETE, ByteObfConfig.ByteObfOptions.LocalVariableOption.OBFUSCATE));
    }
}