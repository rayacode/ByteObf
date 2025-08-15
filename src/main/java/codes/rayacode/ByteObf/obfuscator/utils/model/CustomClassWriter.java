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
    public CustomClassWriter(ByteObf byteObf, int flags, ClassLoader classLoader) {
        super(flags);
        this.byteObf = byteObf;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable e) { 
            String missingType = null;
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof TypeNotPresentException) {
                    missingType = ((TypeNotPresentException) cause).typeName();
                    break;
                } else if (cause instanceof ClassNotFoundException) {
                    missingType = cause.getMessage();
                    break;
                } else if (cause instanceof NoClassDefFoundError) {
                    missingType = cause.getMessage();
                    break;
                }
                cause = cause.getCause(); 
            }

            if (missingType != null) {
                byteObf.err(ByteObf.LogLevel.WARN, "Dependency missing during common super class computation for types %s and %s: %s. " +
                        "Falling back to java/lang/Object. This often means a required library is not added. " +
                        "Please ensure all runtime dependencies of your input JAR are included in the 'Libraries' list.", type1, type2, missingType);
                byteObf.log(ByteObf.LogLevel.DEBUG, "Stack trace for missing dependency:"); 
                byteObf.logStackTrace(e);
            } else {
                
                byteObf.err(ByteObf.LogLevel.WARN, "Unexpected error during common super class computation for types %s and %s: %s. " +
                        "Falling back to java/lang/Object.", type1, type2, e.getMessage());
                byteObf.log(ByteObf.LogLevel.DEBUG, "Stack trace for unexpected error:"); 
                byteObf.logStackTrace(e);
            }

            
            
            
            return "java/lang/Object";
        }
    }

    private String findTypeOrDefault(String type) {
        if (byteObf.getTransformHandler() == null) {
            return type;
        }
        var crt = byteObf.getTransformHandler().getClassTransformer(ClassRenamerTransformer.class);
        return crt.getMap().entrySet().stream()
                .filter(entry -> entry.getValue().equals(type))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(type);
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }
}