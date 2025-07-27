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

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StreamUtils {


    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[0x1000];
        int read;
        while((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
        return out.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        int read;
        byte[] buffer = new byte[0x1000];
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}