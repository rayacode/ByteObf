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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExclusionManager {

    private final List<Pattern> excludePatterns;

    public ExclusionManager(String excludeConfig) {
        this.excludePatterns = excludeConfig.lines()
                .filter(line -> !line.trim().isEmpty())
                .map(this::createPattern)
                .collect(Collectors.toList());
    }

    private Pattern createPattern(String exclusionRule) {
        
        if (exclusionRule.startsWith("regex:")) {
            return Pattern.compile(exclusionRule.substring("regex:".length()));
        } else if (exclusionRule.startsWith("glob:")) {
            String glob = exclusionRule.substring("glob:".length());
            
            
            String regex = glob.replace(".", "\\.")
                    .replace("**", ".*")
                    .replace("*", "[^/]*"); 
            return Pattern.compile(regex);
        } else if (exclusionRule.endsWith(".**")) {
            
            String regex = exclusionRule.substring(0, exclusionRule.length() - 3).replace(".", "\\.") + ".*";
            return Pattern.compile(regex);
        } else {
            
            return Pattern.compile(Pattern.quote(exclusionRule));
        }
    }

    public boolean isExcluded(String name) {
        
        String normalizedName = name.replace('/', '.');
        return excludePatterns.stream().anyMatch(pattern -> pattern.matcher(normalizedName).matches());
    }
}