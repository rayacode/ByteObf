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
import codes.rayacode.ByteObf.obfuscator.transformer.ControlFlowTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class HeavyControlFlowTransformer extends ControlFlowTransformer {


    private static final int METHOD_SIZE_THRESHOLD = 15000;
    private static final double INJECTION_RATE = 0.15;
    private static final String FLOW_FIELD_NAME = String.valueOf((char) 5097);

    public HeavyControlFlowTransformer(ByteObf byteObf) {
        super(byteObf, "Control Flow obfuscation", ByteObfCategory.ADVANCED);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        if (!ASMUtils.isClassEligibleToModify(classNode)) return;

        classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, FLOW_FIELD_NAME, "J", null, 0L));
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if (!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;


        if (ASMUtils.getCodeSize(methodNode) > METHOD_SIZE_THRESHOLD) return;


        if (Arrays.stream(methodNode.instructions.toArray()).noneMatch(ASMUtils::isIf)) {
            injectDummyLoop(methodNode);
        }


        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : instructions) {

            if (!(ASMUtils.isInvokeMethod(insn, true) || insn.getOpcode() == NEW || ASMUtils.isFieldInsn(insn))) {
                continue;
            }


            if (ThreadLocalRandom.current().nextDouble() > INJECTION_RATE) continue;

            int type = ThreadLocalRandom.current().nextInt(2);
            if (type == 0) {
                injectFakeJump(classNode, methodNode, insn);
            } else {
                injectSwitchFlow(classNode, methodNode, insn);
            }
        }
    }

    private void injectDummyLoop(MethodNode methodNode) {
        InsnList il = new InsnList();
        LabelNode l1 = new LabelNode();
        LabelNode l0 = new LabelNode();
        il.add(new InsnNode(ICONST_1));
        il.add(new JumpInsnNode(GOTO, l1));
        il.add(l0);
        il.add(new InsnNode(ICONST_5));
        il.add(l1);
        il.add(new InsnNode(ICONST_M1));
        il.add(new JumpInsnNode(IF_ICMPLE, l0));
        methodNode.instructions.insert(il);
    }

    private void injectFakeJump(ClassNode cn, MethodNode mn, AbstractInsnNode target) {
        InsnList before = new InsnList();
        InsnList after = new InsnList();

        LabelNode lTrue = new LabelNode();
        LabelNode lFalse = new LabelNode();
        LabelNode lEnd = new LabelNode();
        LabelNode lJunk = new LabelNode();


        before.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
        before.add(new InsnNode(POP));
        before.add(new FieldInsnNode(GETSTATIC, cn.name, FLOW_FIELD_NAME, "J"));
        long rand = ThreadLocalRandom.current().nextLong();
        if (rand == 0) rand = 1;
        before.add(ASMUtils.pushLong(rand));
        before.add(new InsnNode(LCMP));


        before.add(new JumpInsnNode(IFEQ, lTrue));
        before.add(new JumpInsnNode(GOTO, lJunk));

        after.add(new JumpInsnNode(GOTO, lEnd));
        after.add(lTrue);
        after.add(new InsnNode(POP));
        after.add(lJunk);
        after.add(new InsnNode(ICONST_0));
        after.add(new JumpInsnNode(GOTO, lTrue));
        after.add(lEnd);

        mn.instructions.insertBefore(target, before);
        mn.instructions.insert(target, after);
    }

    private void injectSwitchFlow(ClassNode cn, MethodNode mn, AbstractInsnNode target) {
        InsnList before = new InsnList();
        before.add(new FieldInsnNode(GETSTATIC, cn.name, FLOW_FIELD_NAME, "J"));
        before.add(new InsnNode(L2I));


        LabelNode def = new LabelNode();
        LabelNode targetL = new LabelNode();
        LabelNode junkL = new LabelNode();

        before.add(new LookupSwitchInsnNode(def, new int[]{0, 1}, new LabelNode[]{targetL, junkL}));

        before.add(junkL);
        before.add(new InsnNode(ACONST_NULL));
        before.add(new InsnNode(ATHROW));

        before.add(def);
        before.add(targetL);

        mn.instructions.insertBefore(target, before);
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(), ByteObfConfig.ByteObfOptions.ControlFlowObfuscationOption.HEAVY);
    }
}