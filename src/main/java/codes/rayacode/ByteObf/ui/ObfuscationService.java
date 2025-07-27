/*  ByteObf: A Java Bytecode Obfuscator
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

package codes.rayacode.ByteObf.ui;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;

import java.util.function.Consumer;

public class ObfuscationService extends Service<Void> {

    private final ByteObfConfig config;
    private final ListView<String> console;

    public ObfuscationService(ByteObfConfig config, ListView<String> console) {
        this.config = config;
        this.console = console;
    }

    @Override
    protected Task<Void> createTask() {
        Consumer<String> logConsumer = msg -> Platform.runLater(() -> {
            console.getItems().add(msg);
            console.scrollTo(console.getItems().size() - 1);
        });
        Consumer<String> errConsumer = msg -> Platform.runLater(() -> {
            console.getItems().add(msg);
            console.scrollTo(console.getItems().size() - 1);
        });

        return new ByteObf(config, logConsumer, errConsumer);
    }
}