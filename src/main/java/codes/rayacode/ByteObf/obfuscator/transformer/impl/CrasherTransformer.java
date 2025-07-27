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
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class CrasherTransformer extends ClassTransformer {

    public CrasherTransformer(ByteObf byteObf) {
        super(byteObf, "Decompiler crasher", ByteObfCategory.ADVANCED);
    }

    public static final String PACKAGE_NAME;
    public static final String REPEAT_BASE = "\u0001/";

    static {
        int caseNum;
        PACKAGE_NAME = switch (caseNum = ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> "com";
            case 1 -> "net";
            case 2 -> "io";
            case 3 -> "org";
            default -> throw new IllegalArgumentException("Invalid PACKAGE_NAME case " + caseNum);
        };
    }

    @Override
    public void transformOutput(JarOutputStream jarOutputStream) {
        ClassNode invalid = new ClassNode();
        invalid.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, PACKAGE_NAME + REPEAT_BASE.repeat((Character.MAX_VALUE / REPEAT_BASE.length()) - PACKAGE_NAME.length()), null, "java/lang/Object", null);
        try {
            jarOutputStream.putNextEntry(new JarEntry("\u0020".repeat(4) + ".class"));
            jarOutputStream.write(ASMUtils.toByteArrayDefault(invalid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().isCrasher(), boolean.class);
    }
}