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

package codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.transformer.RenamerTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import codes.rayacode.ByteObf.obfuscator.utils.model.ResourceWrapper;
import org.objectweb.asm.tree.ClassNode;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassRenamerTransformer extends RenamerTransformer {


    private static final Pattern CLASS_REF_PATTERN = Pattern.compile("([a-zA-Z0-9_$]+[./])+[a-zA-Z0-9_$]+");

    public ClassRenamerTransformer(ByteObf byteObf) {
        super(byteObf, "Rename", ByteObfCategory.STABLE);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        this.registerMap(classNode.name);
    }

    @Override
    public void transformResource(ResourceWrapper resource) {
        if (resource.getZipEntry().isDirectory()) return;


        String name = resource.getZipEntry().getName().toLowerCase();
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".gif") ||
                name.endsWith(".ico") || name.endsWith(".pdf") || name.endsWith(".dll") || name.endsWith(".so")) {
            return;
        }

        try {
            String content = new String(resource.getBytes(), StandardCharsets.UTF_8);
            if (content.isEmpty()) return;


            if (content.indexOf('.') == -1 && content.indexOf('/') == -1) return;

            Matcher matcher = CLASS_REF_PATTERN.matcher(content);
            StringBuilder sb = new StringBuilder(content.length());
            boolean modified = false;

            while (matcher.find()) {
                String match = matcher.group();

                String internalFormat = match.replace('.', '/');

                String replacement = map.get(internalFormat);
                if (replacement != null) {

                    String finalReplacement = (match.contains(".")) ? replacement.replace('/', '.') : replacement;
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(finalReplacement));
                    modified = true;
                }
            }

            if (modified) {
                matcher.appendTail(sb);
                resource.setBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {

        }
    }

    @Override
    public ByteObfConfig.EnableType getEnableType() {
        return new ByteObfConfig.EnableType(() -> this.getByteObf().getConfig().getOptions().getRename() != this.getEnableType().type(), ByteObfConfig.ByteObfOptions.RenameOption.OFF);
    }
}