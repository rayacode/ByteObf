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
import codes.rayacode.ByteObf.obfuscator.utils.StringUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class RenamerTransformer extends ClassTransformer {

    protected final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
    protected final AtomicInteger index = new AtomicInteger(0);

    public RenamerTransformer(ByteObf byteObf, String text, ByteObfCategory category) {
        super(byteObf, text, category);
    }

    protected String registerMap(String key) {
        return map.computeIfAbsent(key, k -> {
            int currentIndex = index.getAndIncrement();
            return switch (this.getByteObf().getConfig().getOptions().getRename()) {
                case ALPHABET -> StringUtils.getAlphabetCombinations().get(currentIndex);
                case INVISIBLE -> String.valueOf((char) (currentIndex + '\u3050'));
                case IlIlIlIlIl -> getRandomUniqueIl(400);
                default -> throw new IllegalStateException("transformClass called while rename is disabled, this shouldn't happen");
            };
        });
    }

    private final Set<String> IlList = ConcurrentHashMap.newKeySet();
    private String getRandomUniqueIl(int length) {
        String s;
        do {
            s = IntStream.range(0, length)
                    .mapToObj(i -> (ThreadLocalRandom.current().nextBoolean()) ? "I" : "l")
                    .collect(Collectors.joining());
        } while (!IlList.add(s));
        return s;
    }

    protected boolean isMapRegistered(String key) {
        return map.containsKey(key);
    }

    protected void registerMap(String key, String value) {
        map.put(key, value);
    }

    public ConcurrentHashMap<String, String> getMap() {
        return map;
    }
}