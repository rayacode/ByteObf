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
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;

public class LineNumberTransformer extends ClassTransformer {

    public LineNumberTransformer(ByteObf byteObf) {
        super(byteObf, "Line numbers", ByteObfCategory.STABLE);
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        switch (this.getByteObf().getConfig().getOptions().getLineNumbers()) {
            case DELETE -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn instanceof LineNumberNode)
                    .map(insn -> (LineNumberNode)insn)
                    .forEach(lineNumberNode -> methodNode.instructions.remove(lineNumberNode));
            case RANDOMIZE -> Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn instanceof LineNumberNode)
                    .map(insn -> (LineNumberNode)insn)
                    // Character.MAX_VALUE is not a special requirement
                    .forEach(lineNumberNode -> lineNumberNode.line = this.random.nextInt(Character.MAX_VALUE));
        }
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> ((List<?>)this.getEnableType().type()).contains(this.getByteObf().getConfig().getOptions().getLineNumbers()),
                List.of(ByteObfConfig.ByteObfOptions.LineNumberOption.DELETE, ByteObfConfig.ByteObfOptions.LineNumberOption.RANDOMIZE));
    }
}