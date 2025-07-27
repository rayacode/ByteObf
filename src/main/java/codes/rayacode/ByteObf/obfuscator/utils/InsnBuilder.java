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

package codes.rayacode.ByteObf.obfuscator.utils;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.Arrays;

public class InsnBuilder {

    private final InsnList insnList;

    private InsnBuilder() {
        this.insnList = new InsnList();
    }

    private InsnBuilder(InsnList insnList) {
        this.insnList = insnList;
    }

    public InsnBuilder insn(AbstractInsnNode... insnNodes) {
        Arrays.stream(insnNodes).forEach(this.insnList::add);
        return this;
    }

    public InsnBuilder insnList(InsnList... insnLists) {
        Arrays.stream(insnLists).forEach(this.insnList::add);
        return this;
    }

    public InsnList getInsnList() {
        return insnList;
    }

    public static InsnBuilder create(InsnList insnList) {
        return new InsnBuilder(insnList);
    }

    public static InsnBuilder createEmpty() {
        return new InsnBuilder();
    }
}