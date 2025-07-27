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

import codes.rayacode.ByteObf.obfuscator.utils.ByteObfUtils;

import javax.swing.*;

public enum ByteObfMessage {

    TITLE("ByteObf Java Bytecode Obfuscator"),
    VERSION_TEXT(TITLE + " v" + ByteObfUtils.getVersion()),

    // Update checker messages
    NEW_UPDATE_AVAILABLE("New update is available: v"),
    CANNOT_CHECK_UPDATE("Cannot check the latest version." + System.lineSeparator() + "Connection failed."),
    CANNOT_OPEN_URL("Cannot open URL. %s, is not supported in your platform.");

    private final String message;
    ByteObfMessage(String message) {
        this.message = message;
    }

    public void showError(Object... args) {
        JOptionPane.showMessageDialog(null, String.format(this.message, args), ByteObfMessage.VERSION_TEXT.toString(), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public String toString() {
        return this.message;
    }
}