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

public class HeavyControlFlowTransformer extends ControlFlowTransformer {

    
    private static final int METHOD_SIZE_THRESHOLD = 30000;
    
    private static final double INJECTION_RATE = 0.20;

    public HeavyControlFlowTransformer(ByteObf byteObf) {
        super(byteObf, "Control Flow obfuscation", ByteObfCategory.ADVANCED);
    }

    private static final String FLOW_FIELD_NAME = String.valueOf((char)5097);
    private static final int[] accessArr = new int[] { 0, ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED };

    @Override
    public void transformClass(ClassNode classNode) {
        if(!ASMUtils.isClassEligibleToModify(classNode)) return;
        classNode.fields.add(new FieldNode(accessArr[ThreadLocalRandom.current().nextInt(accessArr.length)] | ACC_STATIC, FLOW_FIELD_NAME, "J", null, 0L));
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if(!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;

        
        if (ASMUtils.getCodeSize(methodNode) > METHOD_SIZE_THRESHOLD) {
            this.getByteObf().log("Skipping heavy control flow for already large method: %s.%s", classNode.name, methodNode.name);
            return;
        }

        if(Arrays.stream(methodNode.instructions.toArray()).noneMatch(ASMUtils::isIf)) {
            final InsnList il = new InsnList();
            final LabelNode label0 = new LabelNode();
            final LabelNode label1 = new LabelNode();
            il.add(new InsnNode(ICONST_1));
            il.add(new JumpInsnNode(GOTO, label1));
            il.add(label0);
            il.add(new InsnNode(ICONST_5));
            il.add(label1);
            il.add(new InsnNode(ICONST_M1));
            il.add(new JumpInsnNode(IF_ICMPLE, label0));
            methodNode.instructions.insert(il);
        }

        
        Arrays.stream(methodNode.instructions.toArray())
                .filter(insn -> ASMUtils.isInvokeMethod(insn, true) || insn.getOpcode() == NEW || ASMUtils.isFieldInsn(insn))
                .forEach(insn -> {
                    
                    if (random.nextDouble() > INJECTION_RATE) return;

                    final InsnList before = new InsnList();
                    final InsnList after = new InsnList();

                    switch (ThreadLocalRandom.current().nextInt(2)) {
                        case 0 -> {
                            final LabelNode label0 = new LabelNode();
                            final LabelNode label1 = new LabelNode();
                            final LabelNode label2 = new LabelNode();
                            final LabelNode label3 = new LabelNode();

                            before.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                            before.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                            before.add(label2);
                            before.add(new InsnNode(POP2));
                            before.add(new FieldInsnNode(GETSTATIC, classNode.name, FLOW_FIELD_NAME, "J"));
                            long l;
                            do {
                                l = ThreadLocalRandom.current().nextLong();
                            } while (l == 0);
                            before.add(ASMUtils.pushLong(l));
                            before.add(new InsnNode(LCMP));
                            before.add(new InsnNode(ICONST_0));
                            before.add(new InsnNode(SWAP));
                            before.add(new InsnNode(DUP));
                            before.add(new JumpInsnNode(IFEQ, label0));
                            before.add(new JumpInsnNode(IFEQ, label3));
                            before.add(new InsnNode(POP));

                            after.add(new JumpInsnNode(GOTO, label1));
                            after.add(label0);
                            after.add(new InsnNode(POP));
                            after.add(label3);
                            after.add(new InsnNode(ICONST_0));
                            after.add(new JumpInsnNode(GOTO, label2));
                            after.add(label1);
                        }
                        case 1 -> {
                            before.add(new FieldInsnNode(GETSTATIC, classNode.name, FLOW_FIELD_NAME, "J"));
                            before.add(new InsnNode(L2I));
                            before.add(getRandomLookupSwitch(2 + ThreadLocalRandom.current().nextInt(3),
                                    0,
                                    new SwitchBlock(InsnBuilder.createEmpty().getInsnList()),
                                    () -> new SwitchBlock(InsnBuilder.createEmpty().getInsnList()),
                                    InsnBuilder.createEmpty().insn(new InsnNode(ACONST_NULL), new InsnNode(ATHROW)).getInsnList()));
                        }
                    }

                    methodNode.instructions.insertBefore(insn, before);
                    methodNode.instructions.insert(insn, after);
                });
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(), ByteObfConfig.ByteObfOptions.ControlFlowObfuscationOption.HEAVY);
    }
}