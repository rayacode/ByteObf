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
import codes.rayacode.ByteObf.obfuscator.transformer.ControlFlowTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.InsnBuilder;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class LightControlFlowTransformer extends ControlFlowTransformer {

    public LightControlFlowTransformer(ByteObf byteObf) {
        super(byteObf, "Control Flow obfuscation", ByteObfCategory.ADVANCED);
    }

    private static final String FLOW_FIELD_NAME = String.valueOf((char)5096);
    private static final int[] accessArr = new int[] { 0, ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED };

    private long flowFieldValue = 0;
    @Override
    public void transformClass(ClassNode classNode) {
        // Skip interfaces because we cannot declare mutable fields in that
        if(!ASMUtils.isClassEligibleToModify(classNode)) return;

        this.flowFieldValue = ThreadLocalRandom.current().nextLong();
        classNode.fields.add(new FieldNode(accessArr[ThreadLocalRandom.current().nextInt(accessArr.length)] | ACC_STATIC, FLOW_FIELD_NAME, "J", null, this.flowFieldValue));
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if(!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;

        // Main obfuscation
        Arrays.stream(methodNode.instructions.toArray())
                .filter(insn -> ASMUtils.isInvokeMethod(insn, true) || insn.getOpcode() == NEW || ASMUtils.isFieldInsn(insn))
                .forEach(insn -> {
                    final LabelNode label0 = new LabelNode();
                    final LabelNode label1 = new LabelNode();
                    final LabelNode label2 = new LabelNode();
                    final LabelNode label3 = new LabelNode();
                    final LabelNode label4 = new LabelNode();
                    final LabelNode label5 = new LabelNode();
                    final LabelNode label6 = new LabelNode();

                    final InsnList before = new InsnList();
                    final InsnList after = new InsnList();

                    switch (ThreadLocalRandom.current().nextInt(2)) {
                        case 0 -> {
                            before.add(new JumpInsnNode(GOTO, label3));
                            before.add(label2);
                            before.add(new InsnNode(POP));
                            before.add(label3);
                            before.add(new FieldInsnNode(GETSTATIC, classNode.name, FLOW_FIELD_NAME, "J"));
                            long l;
                            do {
                                l = ThreadLocalRandom.current().nextLong();
                            } while (l == this.flowFieldValue);
                            before.add(ASMUtils.pushLong(l));
                            before.add(new InsnNode(LCMP));
                            before.add(new InsnNode(DUP));
                            before.add(new JumpInsnNode(IFEQ, label2));

                            before.add(ASMUtils.pushInt((this.flowFieldValue > l) ? 1 : -1));
                            before.add(new JumpInsnNode(IF_ICMPNE, label5));

                            after.add(new JumpInsnNode(GOTO, label6));
                            after.add(label5);
                            after.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                            after.add(new JumpInsnNode(GOTO, label2));
                            after.add(label6);
                        }
                        case 1 -> {
                            before.add(new FieldInsnNode(GETSTATIC, classNode.name, FLOW_FIELD_NAME, "J"));
                            before.add(new JumpInsnNode(GOTO, label1));
                            before.add(label0);
                            before.add(ASMUtils.pushLong(ThreadLocalRandom.current().nextLong()));
                            before.add(new InsnNode(LDIV));
                            before.add(label1);
                            before.add(new InsnNode(L2I));
                            before.add(getRandomLookupSwitch(2 + ThreadLocalRandom.current().nextInt(3),
                                    (int)this.flowFieldValue,
                                    new SwitchBlock(InsnBuilder.createEmpty().insn(new JumpInsnNode(GOTO, label4)).getInsnList()),
                                    () -> new SwitchBlock(InsnBuilder.createEmpty().insn(ASMUtils.pushLong(ThreadLocalRandom.current().nextLong()), new JumpInsnNode(GOTO, label0)).getInsnList()),
                                    InsnBuilder.createEmpty().getInsnList()));
                            before.add(label4);
                        }
                    }

                    methodNode.instructions.insertBefore(insn, before);
                    methodNode.instructions.insert(insn, after);
                });
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(), ByteObfConfig.ByteObfOptions.ControlFlowObfuscationOption.LIGHT);
    }
}