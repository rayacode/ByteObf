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

package codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class DummyClassTransformer extends ClassTransformer {

    public DummyClassTransformer(ByteObf byteObf) {
        super(byteObf, "Dummy class", ByteObfCategory.WATERMARK);
    }

    @Override
    public void transformOutput(JarOutputStream jarOutputStream) {
        ClassNode dummy = new ClassNode();
        
        String sanitizedClassName = this.getByteObf().getConfig().getOptions().getWatermarkOptions().getDummyClassText()
                .replaceAll("[^a-zA-Z0-9/_-]", "_");

        dummy.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, sanitizedClassName, null, "java/lang/Object", null);
        dummy.visitMethod(random.nextInt(100), "\u0001", "(\u0001/)L\u0001/;", null, null);
        try {
            jarOutputStream.putNextEntry(new JarEntry(dummy.name + ".class"));
            jarOutputStream.write(ASMUtils.toByteArrayDefault(dummy));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getWatermarkOptions().isDummyClass(), ".OBFUSCATED WITH ByteObf");
    }
}