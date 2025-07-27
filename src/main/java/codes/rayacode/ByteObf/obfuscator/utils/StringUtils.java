/*  ByteObf: A Java Bytecode Obfuscator
 *  Copyright (C) 2021 vimasig
 *  Copyright (C) [2025] Mohammad Ali Solhjoo mohammadalisolhjoo@live.com
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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringUtils {

    private static final StringBuilder sb = new StringBuilder();
    private static List<String> strings = new ArrayList<>();
    private static void generateCombinations(String dictionary, int maxIndex, int index)
    {
        if(index == maxIndex)
            strings.add(sb.toString());
        else dictionary.chars().forEach(value -> {
            sb.append((char)value);
            generateCombinations(dictionary, maxIndex, index + 1);
            sb.deleteCharAt(sb.length() - 1);
        });
    }

    private static List<String> alphabetCombinations = null;
    public static List<String> getAlphabetCombinations() {
        if(alphabetCombinations == null) {
            strings = new ArrayList<>();
            for (int i = 2; i <= 3; i++)
                generateCombinations(getAlphabet(), i, 0);
            alphabetCombinations = Collections.unmodifiableList(strings);
        } return alphabetCombinations;
    }

    public static String getAlphabet() {
        return IntStream.rangeClosed('A', 'z')
                .mapToObj(operand -> (char) operand)
                .filter(Character::isLetter)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    public static String getConvertedSize(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }
}