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

package codes.rayacode.ByteObf.obfuscator.utils;

import java.io.*;

public class StreamUtils {

    
    public static byte[] readAll(InputStream in, int sizeEstimate) throws IOException {
        if (sizeEstimate > 0) {
            byte[] buffer = new byte[sizeEstimate];
            int offset = 0;
            int remaining = sizeEstimate;
            int read;
            while (remaining > 0 && (read = in.read(buffer, offset, remaining)) != -1) {
                offset += read;
                remaining -= read;
            }
            
            if (remaining == 0 && in.read() == -1) {
                return buffer;
            }
            
            
            ByteArrayOutputStream out = new ByteArrayOutputStream(sizeEstimate + 8192);
            out.write(buffer, 0, offset);
            copy(in, out);
            return out.toByteArray();
        } else {
            return readAll(in);
        }
    }

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[0x4000]; 
        int read;
        while((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
        return out.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        int read;
        byte[] buffer = new byte[0x10000]; 
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}