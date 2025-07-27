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

package codes.rayacode.ByteObf.ui;

import codes.rayacode.ByteObf.obfuscator.ByteObf;
import codes.rayacode.ByteObf.obfuscator.utils.ByteObfUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfMessage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.cli.*;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // FX GUI
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("/menu.fxml").openStream());
        Controller controller = fxmlLoader.getController();

        // Handle command lines
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(this.getOptions(), this.getParameters().getRaw().toArray(new String[0]));

            if(cmd.hasOption("config"))
                try {
                    controller.configManager.loadConfig(new File(cmd.getOptionValue("config")));
                } catch (IOException e) {
                    e.printStackTrace();
                    logException(e);
                    System.err.println("Cannot load config.");
                }
            if(cmd.hasOption("input"))
                controller.input.setText(cmd.getOptionValue("input"));
            if(cmd.hasOption("output"))
                controller.output.setText(cmd.getOptionValue("output"));

            // Update checker
            String latestVer = null;
            if(!cmd.hasOption("noupdate")) {
                try {
                    latestVer = ByteObfUtils.getLatestVersion();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    logException(e);
                }
            } else latestVer = ByteObfUtils.getVersion();

            // Console mode
            if(cmd.hasOption("console")) {
                if(!cmd.hasOption("noupdate")) {
                    if(latestVer == null)
                        System.out.println("[BYTEOBFGUI] " + ByteObfMessage.CANNOT_CHECK_UPDATE.toString());
                    else if(!ByteObfUtils.getVersion().equals(latestVer))
                        System.out.println("[BYTEOBFGUI] " + ByteObfMessage.NEW_UPDATE_AVAILABLE + latestVer);
                }

                ByteObfConfig config = controller.configManager.generateConfig();
                // **FIX**: Provide the correct consumers for console mode logging.
                ByteObf byteObf = new ByteObf(config, System.out::println, System.err::println);
                try {
                    byteObf.call(); // Use call() now that it's a Task
                } catch (Exception e) {
                    e.printStackTrace();
                    logException(e);
                }
                System.exit(0);
            }

            if(latestVer == null)
                JOptionPane.showMessageDialog(null, ByteObfMessage.CANNOT_CHECK_UPDATE.toString(), ByteObfMessage.VERSION_TEXT.toString(), JOptionPane.ERROR_MESSAGE);
            else if(!ByteObfUtils.getVersion().equals(latestVer)){
                var message = ByteObfMessage.NEW_UPDATE_AVAILABLE + latestVer + System.lineSeparator() + "Do you want to go to the site?";
                if(JOptionPane.showConfirmDialog(null, message, ByteObfMessage.VERSION_TEXT.toString(), JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == 0)
                    ByteObfUtils.openDownloadURL();
            }

            // GUI
            Scene scene = new Scene(root);
            stage.setTitle(ByteObfMessage.VERSION_TEXT.toString());
            stage.setScene(scene);
            stage.show();
        } catch (ParseException e) {
            e.printStackTrace();
            logException(e);
            System.err.println("Cannot parse command line.");
            System.exit(0);
        }
    }


    private void logException(Exception e) {
        try (FileWriter fw = new FileWriter("D:\\projects\\java\\javafx\\ByteObf\\loginfo.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            e.printStackTrace(pw);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private Options getOptions() {
        final Options options = new Options();
        options.addOption(new Option("input", true, "Input file."));
        options.addOption(new Option("output", true, "Output file."));
        options.addOption(new Option( "cfg", "config", true, "Config file."));
        options.addOption(new Option( "noupdate", "Disable update warnings"));
        options.addOption(new Option("c", "console", false, "Application will run without GUI and obfuscation task will be started immediately."));
        return options;
    }
}