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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class RenamerTransformer extends ClassTransformer {

    protected final HashMap<String, String> map = new HashMap<>();
    protected int index = 0;

    public RenamerTransformer(ByteObf byteObf, String text, ByteObfCategory category) {
        super(byteObf, text, category);
    }

    protected String registerMap(String key) {
        var str = switch (this.getByteObf().getConfig().getOptions().getRename()) {
            case ALPHABET -> StringUtils.getAlphabetCombinations().get(index);
            case INVISIBLE -> String.valueOf((char)(index + '\u3050'));
            case IlIlIlIlIl -> getRandomUniqueIl(400);
            default -> throw new IllegalStateException("transformClass called while rename is disabled, this shouldn't happen");
        };
        map.put(key, str); index++;
        return str;
    }

    private final List<String> IlList = new ArrayList<>();
    private String getRandomUniqueIl(int length) {
        String s;
        do {
            s = IntStream.range(0, length)
                    .mapToObj(i -> (ThreadLocalRandom.current().nextBoolean()) ? "I" : "l")
                    .collect(Collectors.joining());
        } while (IlList.contains(s));
        IlList.add(s);
        return s;
    }

    protected boolean isMapRegistered(String key) {
        return map.get(key) != null;
    }

    protected void registerMap(String key, String value) {
        map.put(key, value);
    }

    public HashMap<String, String> getMap() {
        return map;
    }
}