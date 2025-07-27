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

import com.google.gson.annotations.SerializedName;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfMessage;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class ByteObfUtils {

    private static final String RELEASES_URL = "https://github.com/rayacode/ByteObf/releases";
    private static final String VERSION_URL = "https://raw.githubusercontent.com/rayacode/ByteObf/master/latestrelease";

    public static String getLatestVersion() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VERSION_URL))
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static void openDownloadURL() {
        String err = Desktop.isDesktopSupported() ? (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ? null : "Browse") : "Desktop";
        if(err != null)
            ByteObfMessage.CANNOT_OPEN_URL.showError(err);
        else
            try {
                Desktop.getDesktop().browse(URI.create(RELEASES_URL));
            } catch (IOException e) {
                e.printStackTrace();
            }
        System.exit(0);
    }

    public static String getVersion() {
        try {
            final Properties properties = new Properties();
            properties.load(ByteObfUtils.class.getResourceAsStream("/byteobf.properties"));
            return properties.getProperty("byteobf.version");
        } catch (IOException e) {
            e.printStackTrace();
            return "Unknown version";
        }
    }

    public static String getSerializedName(Enum<?> en) {
        try {
            return en.getClass().getField(en.name()).getAnnotation(SerializedName.class).value();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}