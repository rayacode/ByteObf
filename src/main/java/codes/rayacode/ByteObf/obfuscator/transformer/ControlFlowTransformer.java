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

package codes.rayacode.ByteObf.obfuscator.transformer;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.utils.ASMUtils;
import codes.rayacode.ByteObf.obfuscator.utils.InsnBuilder;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ControlFlowTransformer extends ClassTransformer {

    public ControlFlowTransformer(ByteObf byteObf, String text, ByteObfCategory category) {
        super(byteObf, text, category);
    }

    protected static final record SwitchBlock(LabelNode labelNode, InsnList insnList) {
        public SwitchBlock() {
            this(new LabelNode(), new InsnList());
            this.insnList.add(getRandomLongDiv());
        }

        public SwitchBlock(InsnList insnList) {
            this(new LabelNode(), insnList);
        }
    }

    protected static InsnList getRandomLookupSwitch(final int switchSize, final int targetKey, final SwitchBlock targetBlock, final InsnList defInstructions) {
        return getRandomLookupSwitch(switchSize, targetKey, targetBlock, SwitchBlock::new, defInstructions);
    }

    protected static InsnList getRandomLookupSwitch(final int switchSize, final int targetKey, final SwitchBlock targetBlock, final Supplier<SwitchBlock> dummyBlock, final InsnList defInstructions) {
        final InsnList il = new InsnList();
        var switchDefaultLabel = new LabelNode();
        var switchEndLabel = new LabelNode();
        var switchBlocks = IntStream.range(0, switchSize).mapToObj(v -> dummyBlock.get()).collect(Collectors.toList());
        var keyList = getUniqueRandomIntArray(switchSize - 1);

        {
            keyList.add(targetKey);
            Collections.sort(keyList);
            switchBlocks.set(keyList.indexOf(targetKey), targetBlock);
        }

        il.add(new LookupSwitchInsnNode(switchDefaultLabel, keyList.stream().mapToInt(j -> j).toArray(), switchBlocks.stream().map(SwitchBlock::labelNode).toArray(LabelNode[]::new)));
        switchBlocks.forEach(switchBlock -> {
            il.add(switchBlock.labelNode());
            il.add(switchBlock.insnList());
            il.add(new JumpInsnNode(GOTO, switchEndLabel));
        });
        il.add(switchDefaultLabel);
        il.add(defInstructions);
        il.add(switchEndLabel);
        return il;
    }

    protected static List<Integer> getUniqueRandomIntArray(int size) {
        var baseList = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            int j;
            do {
                j = ThreadLocalRandom.current().nextInt();
            } while (baseList.contains(j));
            baseList.add(j);
        } return baseList;
    }

    protected static InsnList getRandomLongDiv() {
        return InsnBuilder.createEmpty().insn(ASMUtils.pushLong(new Random().nextLong()), new InsnNode(LDIV)).getInsnList();
    }
}