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
package codes.rayacode.ByteObf.obfuscator.transformer;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RenamerTransformer extends ClassTransformer {


    protected final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>(65536);
    protected final AtomicInteger index = new AtomicInteger(0);

    public RenamerTransformer(ByteObf byteObf, String text, ByteObfCategory category) {
        super(byteObf, text, category);
    }

    protected String registerMap(String key) {

        return map.computeIfAbsent(key, k -> generateName(index.getAndIncrement()));
    }

    protected String registerMap(String key, String forceValue) {
        map.put(key, forceValue);
        return forceValue;
    }

    protected boolean isMapRegistered(String key) {
        return map.containsKey(key);
    }

    public ConcurrentHashMap<String, String> getMap() {
        return map;
    }

    /**
     * Generates a unique name based on a numeric ID using Base-N encoding.
     * Complexity: O(log_N(ID)) -> effectively O(1)
     */
    private String generateName(int id) {
        ByteObfConfig.ByteObfOptions.RenameOption option = this.getByteObf().getConfig().getOptions().getRename();
        switch (option) {
            case ALPHABET:
                return toBaseString(id, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            case INVISIBLE:
                return generateInvisible(id);
            case IlIlIlIlIl:
                return generateIlIl(id);
            default:
                throw new IllegalStateException("Rename disabled or unknown");
        }
    }

    private String generateIlIl(int id) {

        String bin = Integer.toBinaryString(id + 1);
        return bin.replace('0', 'I').replace('1', 'l');
    }

    private String generateInvisible(int id) {

        char[] chars = {'\u200B', '\u200C', '\u200D'};
        StringBuilder sb = new StringBuilder();
        int n = id + 1;
        while (n > 0) {
            n--;
            sb.append(chars[n % 3]);
            n /= 3;
        }
        return sb.toString();
    }

    private String toBaseString(int n, String alphabet) {
        int base = alphabet.length();
        if (n == 0) return String.valueOf(alphabet.charAt(0));
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;
            sb.append(alphabet.charAt(n % base));
            n /= base;
        }

        return sb.reverse().toString();
    }
}