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

import com.google.gson.annotations.SerializedName;

public enum ByteObfCategory {
    @SerializedName("Stable") STABLE("Stable obfuscation options. Most options are irreversible.\nA good way to protect & speed up your application."),
    @SerializedName("Advanced") ADVANCED("Advanced obfuscation options. Reversible.\nPowerful protection against newbies."),
    @SerializedName("Watermark") WATERMARK("Different ways to implement watermark to your application.");

    private final String description;

    ByteObfCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}