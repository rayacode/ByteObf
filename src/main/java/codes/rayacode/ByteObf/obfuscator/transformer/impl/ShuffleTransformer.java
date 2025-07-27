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
import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collections;
import java.util.List;

public class ShuffleTransformer extends ClassTransformer {

    public ShuffleTransformer(ByteObf byteObf) {
        super(byteObf, "Randomize obfuscation", ByteObfCategory.STABLE);
    }

    @Override
    public void pre() {
        this.shuffle();
    }

    @Override
    public void post() {
        this.shuffle();
    }

    private void shuffle() {
        var classes = this.getByteObf().getClasses();
        Collections.shuffle(classes);
        classes.forEach(ShuffleTransformer::shuffle);
    }

    private static void shuffle(ClassNode classNode) {
        shuffleIfNonnull(classNode.fields);
        shuffleIfNonnull(classNode.methods);
        shuffleIfNonnull(classNode.innerClasses);
        shuffleIfNonnull(classNode.interfaces);
        shuffleIfNonnull(classNode.attrs);
        shuffleIfNonnull(classNode.invisibleAnnotations);
        shuffleIfNonnull(classNode.visibleAnnotations);
        shuffleIfNonnull(classNode.invisibleTypeAnnotations);
        shuffleIfNonnull(classNode.visibleTypeAnnotations);
        classNode.fields.forEach(f -> {
            shuffleIfNonnull(f.attrs);
            shuffleIfNonnull(f.invisibleAnnotations);
            shuffleIfNonnull(f.visibleAnnotations);
            shuffleIfNonnull(f.invisibleTypeAnnotations);
            shuffleIfNonnull(f.visibleTypeAnnotations);
        });
        classNode.methods.forEach(m -> {
            shuffleIfNonnull(m.attrs);
            shuffleIfNonnull(m.invisibleAnnotations);
            shuffleIfNonnull(m.visibleAnnotations);
            shuffleIfNonnull(m.invisibleTypeAnnotations);
            shuffleIfNonnull(m.visibleTypeAnnotations);
            shuffleIfNonnull(m.exceptions);
            shuffleIfNonnull(m.invisibleLocalVariableAnnotations);
            shuffleIfNonnull(m.visibleLocalVariableAnnotations);
            shuffleIfNonnull(m.localVariables);
            shuffleIfNonnull(m.parameters);
        });
    }

    private static void shuffleIfNonnull(List<?> list) {
        if(list != null) Collections.shuffle(list);
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().isShuffle(), boolean.class);
    }
}