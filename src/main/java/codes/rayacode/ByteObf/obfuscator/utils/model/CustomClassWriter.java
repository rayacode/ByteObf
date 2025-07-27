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

package codes.rayacode.ByteObf.obfuscator.utils.model;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer.ClassRenamerTransformer;
import org.objectweb.asm.ClassWriter;

import java.util.Map;

public class CustomClassWriter extends ClassWriter {

    private final ByteObf byteObf;
    private final ClassLoader classLoader;
    public CustomClassWriter(ByteObf byteObf, int flags, ClassLoader classLoader) {
        super(flags);
        this.byteObf = byteObf;
        this.classLoader = classLoader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (TypeNotPresentException e) {
            return super.getCommonSuperClass(this.findTypeOrDefault(type1), this.findTypeOrDefault(type2));
        }
    }

    private String findTypeOrDefault(String type) {
        var crt = this.byteObf.getTransformHandler().getClassTransformer(ClassRenamerTransformer.class);
        return crt.getMap().entrySet().stream()
                .filter(entry -> entry.getValue().equals(type))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(type);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }
}