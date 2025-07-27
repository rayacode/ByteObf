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

package codes.rayacode.ByteObf.ui;

import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import com.google.gson.*;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.*;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.renamer.ClassRenamerTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.DummyClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.TextInsideClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.UnusedStringTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.impl.watermark.ZipCommentTransformer;
import codes.rayacode.ByteObf.obfuscator.utils.ByteObfUtils;
import codes.rayacode.ByteObf.obfuscator.utils.Reflection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final Controller controller;
    public ConfigManager(Controller controller) {
        this.controller = controller;
    }

    public void loadConfig(File file) throws IOException {
        String str = Files.readString(file.toPath());
        try {
            // Deserializer for input/output file
            JsonDeserializer<ByteObfConfig> deserializer = (jsonElement, type, jsonDeserializationContext) -> {
                try {
                    ByteObfConfig byteObfConfig = new Gson().fromJson(jsonElement, ByteObfConfig.class);
                    var reflect = new Reflection<>(byteObfConfig);
                    reflect.setDeclaredField("input", new File(((JsonObject)jsonElement).get("input").getAsString()));
                    reflect.setDeclaredField("output", Path.of(((JsonObject)jsonElement).get("output").getAsString()));
                    return byteObfConfig;
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    return null;
                }
            };

            // Load config
            ByteObfConfig byteObfConfig = new GsonBuilder()
                    .registerTypeAdapter(ByteObfConfig.class, deserializer)
                    .create()
                    .fromJson(str, ByteObfConfig.class);
            if(byteObfConfig != null)
                this.loadConfig(byteObfConfig);
            else throw new NullPointerException("byteObfConfig");
        } catch (JsonSyntaxException | NullPointerException e) {
            e.printStackTrace();
            this.controller.log("Cannot parse config: " + file.getName());
        }
    }

    public void loadConfig(ByteObfConfig byteObfConfig) {
        var c = this.controller;
        c.input.setText(byteObfConfig.getInput().getAbsolutePath());
        c.output.setText(byteObfConfig.getOutput().toFile().getAbsolutePath());
        c.exclude.setText(byteObfConfig.getExclude());
        c.libraries.getItems().addAll(byteObfConfig.getLibraries());

        // Obfuscation options
        c.getComboBox(LineNumberTransformer.class).getSelectionModel().select(ByteObfUtils.getSerializedName(byteObfConfig.getOptions().getLineNumbers()));
        c.getComboBox(LocalVariableTransformer.class).getSelectionModel().select(ByteObfUtils.getSerializedName(byteObfConfig.getOptions().getLocalVariables()));
        c.getComboBox(ClassRenamerTransformer.class).getSelectionModel().select(ByteObfUtils.getSerializedName(byteObfConfig.getOptions().getRename()));
        c.getCheckBox(SourceFileTransformer.class).setSelected(byteObfConfig.getOptions().isRemoveSourceFile());
        c.getCheckBox(ShuffleTransformer.class).setSelected(byteObfConfig.getOptions().isShuffle());
        c.getCheckBox(InnerClassTransformer.class).setSelected(byteObfConfig.getOptions().isRemoveInnerClasses());
        c.getComboBox(LightControlFlowTransformer.class).getSelectionModel().select(ByteObfUtils.getSerializedName(byteObfConfig.getOptions().getControlFlowObfuscation()));
        c.getCheckBox(CrasherTransformer.class).setSelected(byteObfConfig.getOptions().isCrasher());
        c.getComboBox(ConstantTransformer.class).getSelectionModel().select(ByteObfUtils.getSerializedName(byteObfConfig.getOptions().getConstantObfuscation()));

        // Watermark options
        c.getCheckBox(DummyClassTransformer.class).setSelected(byteObfConfig.getOptions().getWatermarkOptions().isDummyClass());
        c.getCheckBox(TextInsideClassTransformer.class).setSelected(byteObfConfig.getOptions().getWatermarkOptions().isTextInsideClass());
        c.getCheckBox(UnusedStringTransformer.class).setSelected(byteObfConfig.getOptions().getWatermarkOptions().isLdcPop());
        c.getCheckBox(ZipCommentTransformer.class).setSelected(byteObfConfig.getOptions().getWatermarkOptions().isZipComment());

        c.getTextInputControl(DummyClassTransformer.class).setText(byteObfConfig.getOptions().getWatermarkOptions().getDummyClassText());
        c.getTextInputControl(TextInsideClassTransformer.class).setText(byteObfConfig.getOptions().getWatermarkOptions().getTextInsideClassText());
        c.getTextInputControl(UnusedStringTransformer.class).setText(byteObfConfig.getOptions().getWatermarkOptions().getLdcPopText());
        c.getTextInputControl(ZipCommentTransformer.class).setText(byteObfConfig.getOptions().getWatermarkOptions().getZipCommentText());
    }

    public void saveConfig(ByteObfConfig byteObfConfig) throws IOException {
        try (FileWriter fw = new FileWriter("byteobfconfig.json")) {
            // Serializer for input/output file
            JsonSerializer<ByteObfConfig> serializer = (cfg, type, jsonSerializationContext) -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("input", new JsonPrimitive(cfg.getInput().getAbsolutePath()));
                jsonObject.add("output", new JsonPrimitive(cfg.getOutput().toFile().getAbsolutePath()));
                ((JsonObject) new Gson().toJsonTree(cfg))
                        .entrySet().forEach(stringJsonElementEntry -> jsonObject.add(stringJsonElementEntry.getKey(), stringJsonElementEntry.getValue()));
                return jsonObject;
            };

            // Write config
            fw.write(new GsonBuilder()
                    .registerTypeAdapter(ByteObfConfig.class, serializer)
                    .setPrettyPrinting()
                    .create()
                    .toJson(byteObfConfig)
            );
            fw.flush();
        }
    }

    public ByteObfConfig generateConfig() {
        var c = this.controller;

        ByteObfConfig.ByteObfOptions.WatermarkOptions watermarkOptions = new ByteObfConfig.ByteObfOptions.WatermarkOptions(
                c.getCheckBox(DummyClassTransformer.class).isSelected(),
                c.getCheckBox(TextInsideClassTransformer.class).isSelected(),
                c.getCheckBox(UnusedStringTransformer.class).isSelected(),
                c.getCheckBox(ZipCommentTransformer.class).isSelected(),
                c.getTextInputControl(DummyClassTransformer.class).getText(),
                c.getTextInputControl(TextInsideClassTransformer.class).getText(),
                c.getTextInputControl(UnusedStringTransformer.class).getText(),
                c.getTextInputControl(ZipCommentTransformer.class).getText()
        );
        ByteObfConfig.ByteObfOptions byteObfOptions = new ByteObfConfig.ByteObfOptions(
                (ByteObfConfig.ByteObfOptions.RenameOption) c.getEnum(ClassRenamerTransformer.class),
                (ByteObfConfig.ByteObfOptions.LineNumberOption) c.getEnum(LineNumberTransformer.class),
                (ByteObfConfig.ByteObfOptions.LocalVariableOption) c.getEnum(LocalVariableTransformer.class),
                c.getCheckBox(SourceFileTransformer.class).isSelected(),
                c.getCheckBox(ShuffleTransformer.class).isSelected(),
                c.getCheckBox(InnerClassTransformer.class).isSelected(),
                (ByteObfConfig.ByteObfOptions.ControlFlowObfuscationOption) c.getEnum(LightControlFlowTransformer.class),
                c.getCheckBox(CrasherTransformer.class).isSelected(),
                (ByteObfConfig.ByteObfOptions.ConstantObfuscationOption) c.getEnum(ConstantTransformer.class),
                watermarkOptions
        );
        ByteObfConfig byteObfConfig = new ByteObfConfig(c.input.getText(), c.output.getText(), c.exclude.getText(), this.controller.libraries.getItems(), byteObfOptions);

        try {
            this.saveConfig(byteObfConfig);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot save config.");
        }
        return byteObfConfig;
    }

    public void loadDefaultConfig() throws IOException {
        File f = new File("byteobfconfig.json");
        if(f.exists() && f.isFile())
            this.loadConfig(f);
    }
}