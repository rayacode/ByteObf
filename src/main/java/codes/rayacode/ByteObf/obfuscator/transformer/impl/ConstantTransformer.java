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
package codes.rayacode.ByteObf.obfuscator.transformer.impl;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.tree.*;

import java.util.concurrent.ThreadLocalRandom;

public class ConstantTransformer extends ClassTransformer {

    private static final int METHOD_SIZE_THRESHOLD = 30000;
    private static final int MAX_STRING_LENGTH_TO_OBFUSCATE = 128;

    private static final double INJECTION_RATE = 0.25;

    public ConstantTransformer(ByteObf byteObf) {
        super(byteObf, "Constant obfuscation", ByteObfCategory.ADVANCED);
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if (ASMUtils.getCodeSize(methodNode) > METHOD_SIZE_THRESHOLD) return;


        AbstractInsnNode[] insns = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : insns) {
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                if (s.length() > MAX_STRING_LENGTH_TO_OBFUSCATE || s.isEmpty()) continue;
                if (ThreadLocalRandom.current().nextDouble() > INJECTION_RATE) continue;


                methodNode.instructions.insertBefore(ldc, convertStringFast(s));
                methodNode.instructions.remove(ldc);
            }
        }


        obfuscateNumbers(classNode, methodNode);
    }

    private void obfuscateNumbers(ClassNode classNode, MethodNode methodNode) {
        boolean isFlow = this.getByteObf().getConfig().getOptions().getConstantObfuscation() == ByteObfConfig.ByteObfOptions.ConstantObfuscationOption.FLOW;

        AbstractInsnNode[] insns = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : insns) {
            if (!ASMUtils.isPushInt(insn) && !ASMUtils.isPushLong(insn)) continue;
            if (ThreadLocalRandom.current().nextDouble() > INJECTION_RATE) continue;

            ValueType valueType = getValueType(insn);
            long value = (valueType == ValueType.INTEGER) ? ASMUtils.getPushedInt(insn) : ASMUtils.getPushedLong(insn);

            InsnList replacement = new InsnList();


            long key = ThreadLocalRandom.current().nextInt(Short.MAX_VALUE);
            long enc = value ^ key;

            if (valueType == ValueType.INTEGER) {
                replacement.add(ASMUtils.pushInt((int) key));
                replacement.add(ASMUtils.pushInt((int) enc));
                replacement.add(new InsnNode(IXOR));
            } else {
                replacement.add(ASMUtils.pushLong(key));
                replacement.add(ASMUtils.pushLong(enc));
                replacement.add(new InsnNode(LXOR));
            }


            if (isFlow) {
                InsnList flow = new InsnList();
                LabelNode lTrue = new LabelNode();
                LabelNode lFalse = new LabelNode();


                flow.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                flow.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                flow.add(new JumpInsnNode(IF_ICMPEQ, lTrue));


                flow.add(replacement);
                flow.add(new JumpInsnNode(GOTO, lFalse));


                flow.add(lTrue);
                flow.add(new InsnNode(POP2));
                flow.add((valueType == ValueType.INTEGER) ? ASMUtils.pushInt(0) : ASMUtils.pushLong(0));

                flow.add(lFalse);

                methodNode.instructions.insertBefore(insn, flow);
                methodNode.instructions.remove(insn);
            } else {
                methodNode.instructions.insertBefore(insn, replacement);
                methodNode.instructions.remove(insn);
            }
        }
    }


    private InsnList convertStringFast(String str) {
        InsnList list = new InsnList();
        list.add(ASMUtils.pushInt(str.length()));
        list.add(new IntInsnNode(NEWARRAY, T_BYTE));

        byte[] bytes = str.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            list.add(new InsnNode(DUP));
            list.add(ASMUtils.pushInt(i));
            list.add(ASMUtils.pushInt(bytes[i]));
            list.add(new InsnNode(BASTORE));
        }

        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP_X1));
        list.add(new InsnNode(SWAP));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false));
        return list;
    }


    @Override
    public void transformField(ClassNode classNode, FieldNode fieldNode) {
        if (fieldNode.value instanceof String && (fieldNode.access & ACC_STATIC) != 0) {

            MethodNode clinit = ASMUtils.findOrCreateClinit(classNode);
            InsnList il = new InsnList();
            il.add(new LdcInsnNode(fieldNode.value));
            il.add(new FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc));
            clinit.instructions.insert(il);
            fieldNode.value = null;
        }
    }

    private ValueType getValueType(AbstractInsnNode insn) {
        if (ASMUtils.isPushInt(insn)) return ValueType.INTEGER;
        if (ASMUtils.isPushLong(insn)) return ValueType.LONG;
        throw new IllegalArgumentException("Insn is not a push int/long instruction");
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> ((java.util.List<?>) this.getEnableType().type()).contains(this.getByteObf().getConfig().getOptions().getConstantObfuscation()),
                java.util.List.of(ByteObfConfig.ByteObfOptions.ConstantObfuscationOption.LIGHT, ByteObfConfig.ByteObfOptions.ConstantObfuscationOption.FLOW));
    }

    private enum ValueType {INTEGER, LONG}
}