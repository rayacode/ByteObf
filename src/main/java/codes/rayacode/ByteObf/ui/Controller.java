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

import codes.rayacode.ByteObf.obfuscator.transformer.ClassTransformer;
import codes.rayacode.ByteObf.obfuscator.transformer.TransformManager;
import codes.rayacode.ByteObf.obfuscator.utils.ByteObfUtils;
import codes.rayacode.ByteObf.obfuscator.utils.FileUtils;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfCategory;
import codes.rayacode.ByteObf.obfuscator.utils.model.ByteObfConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Controller {

    public final ConfigManager configManager = new ConfigManager(this);

    @FXML private Button browseInput;
    @FXML private Button browseOutput;
    @FXML private ListView<String> console;
    @FXML private Button buttonObf;
    @FXML private Button buttonAddJAR;
    @FXML private Button buttonAddDir;
    @FXML private Button buttonRemoveLib;
    @FXML private TabPane optionsTab;
    @FXML private ProgressBar progressBar;

    public TextField input;
    public TextField output;
    public TextArea exclude;
    public ListView<String> libraries;

    private ObfuscationService obfuscationService;

    @FXML
    private void handleObfuscateAction() {
        console.getItems().clear();
        log("Generating config...");
        ByteObfConfig config = this.configManager.generateConfig();

        obfuscationService = new ObfuscationService(config, console);

        progressBar.progressProperty().bind(obfuscationService.progressProperty());
        buttonObf.disableProperty().bind(obfuscationService.runningProperty());

        obfuscationService.setOnSucceeded(e -> {
            log("UI: Obfuscation service succeeded.");
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
        });

        obfuscationService.setOnFailed(e -> {
            log("UI: Obfuscation service failed.");
            Throwable ex = e.getSource().getException();
            if (ex != null) {
                ex.printStackTrace(System.err);
            }
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        });

        obfuscationService.setOnCancelled(e -> {
            log("UI: Obfuscation service cancelled.");
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        });

        obfuscationService.start();
    }

    // The RedirectedPrintStream class is no longer necessary as we are handling logging directly.

    @FXML
    public void initialize() {
        // We will log directly to the console ListView now.
        log("Initializing controller...");

        for (final ByteObfCategory category : ByteObfCategory.values()) {
            final VBox vBox = new VBox();
            vBox.setSpacing(15);
            vBox.setPadding(new Insets(20));
            this.optionsTab.getTabs().add(new Tab(ByteObfUtils.getSerializedName(category), vBox));

            TransformManager.getTransformers().stream()
                    .map(TransformManager::createTransformerInstance)
                    .filter(Objects::nonNull)
                    .filter(ct -> !isPresent(vBox, ct))
                    .filter(ct -> ct.getCategory() == category)
                    .forEach(ct -> {
                        try {
                            ByteObfConfig.EnableType enableType = ct.getEnableType();
                            Object type = enableType.type();

                            if(type.getClass().isEnum())
                                type = new ArrayList<>(List.of((Enum<?>)type));

                            if(List.class.isAssignableFrom(type.getClass())) {
                                HBox hBox = getHBox(vBox);
                                hBox.getChildren().add(new Label(ct.getText()));

                                var comboBox = new ComboBox<>(FXCollections.observableList(new ArrayList<String>()));
                                mapComboBox(comboBox, ((Enum<?>) ((List<?>) type).get(0)).getDeclaringClass());
                                comboBox.setPrefWidth(150);
                                hBox.getChildren().add(comboBox);
                            } else if(type.getClass() == String.class) {
                                HBox hBox = getHBox(vBox);
                                var checkBox = new CheckBox(ct.getText());
                                hBox.getChildren().add(checkBox);

                                TextInputControl tic;
                                if(((String)type).contains("\n")) {
                                    tic = new TextArea();
                                    tic.setPrefWidth(200);
                                    tic.setPrefHeight(200);
                                    VBox.setVgrow(hBox, Priority.ALWAYS);
                                } else tic = new TextField();

                                HBox.setHgrow(tic, Priority.ALWAYS);
                                tic.setText((String)enableType.type());
                                hBox.getChildren().add(tic);
                            } else if(type == boolean.class) {
                                var checkBox = new CheckBox(ct.getText());
                                vBox.getChildren().add(checkBox);
                            } else throw new IllegalArgumentException();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            Region region = new Region();
            VBox.setVgrow(region, Priority.ALWAYS);
            vBox.getChildren().add(region);

            Label label = new Label(category.getDescription());
            label.setFont(Font.font(11D));
            vBox.getChildren().add(label);
        }

        exclude.setPromptText("com.example.myapp.MyClass\ncom.example.mypackage.**");

        final Function<ActionEvent, Window> getWindowFunc = actionEvent -> ((Button)actionEvent.getSource()).getScene().getWindow();
        final var jarFilter = new FileChooser.ExtensionFilter("JAR files (*.jar)", "*.jar");
        browseInput.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(jarFilter);
            File file = fileChooser.showOpenDialog(getWindowFunc.apply(actionEvent));
            if (file != null && file.exists() && file.isFile()) {
                input.setText(file.getAbsolutePath());
            }
        });
        browseOutput.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(jarFilter);
            File file = fileChooser.showSaveDialog(getWindowFunc.apply(actionEvent));
            if (file != null) {
                output.setText(file.getAbsolutePath());
            }
        });

        buttonObf.setOnAction(e -> handleObfuscateAction());

        buttonAddJAR.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(jarFilter);
            List<File> files = fileChooser.showOpenMultipleDialog(getWindowFunc.apply(actionEvent));
            if(files == null) return;
            files.forEach(file -> {
                if (file != null && file.exists() && file.isFile()) {
                    libraries.getItems().add(file.getAbsolutePath());
                }
            });
        });
        buttonAddDir.setOnAction(actionEvent -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            File file = dirChooser.showDialog(getWindowFunc.apply(actionEvent));
            if (file != null && file.exists() && file.isDirectory()) {
                libraries.getItems().addAll(FileUtils.getAllFiles(file).stream()
                        .filter(f -> f.getName().endsWith(".jar"))
                        .map(f -> {
                            try {
                                return f.getCanonicalPath();
                            } catch (IOException e) {
                                throw new RuntimeException(String.format("Cannot get canonical path of file %s", f.getName()), e);
                            }
                        }).collect(Collectors.toList()));
            }
        });
        buttonRemoveLib.setOnAction(actionEvent -> {
            int index = libraries.getSelectionModel().getSelectedIndex();
            if(index != -1) {
                libraries.getItems().remove(index);
            }
        });

        try {
            this.configManager.loadDefaultConfig();
        } catch (IOException e) {
            e.printStackTrace();
            this.log("Cannot load default config");
        }

        log("Loaded.");
    }

    private static boolean isPresent(VBox vBox, ClassTransformer ct) {
        return vBox.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox)node)
                .anyMatch(hBox -> hBox.getChildren().stream()
                        .filter(node -> node instanceof Label || node instanceof CheckBox)
                        .map(node -> {
                            if(node instanceof Label) return ((Label)node).getText();
                            else return ((CheckBox)node).getText();
                        })
                        .findFirst()
                        .orElseThrow(NullPointerException::new)
                        .equals(ct.getText())
                );
    }

    private static HBox getHBox(VBox vBox) {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setSpacing(20);
        vBox.getChildren().add(hBox);
        return hBox;
    }

    @SuppressWarnings("unchecked")
    TextInputControl getTextInputControl(Class<? extends ClassTransformer> transformerClass) {
        final var transformer = Objects.requireNonNull(TransformManager.createTransformerInstance(transformerClass));
        return this.getTabFromCategory(transformer.getCategory()).getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox)node)
                .filter(hBox -> hBox.getChildren().stream().filter(node -> node instanceof Label || node instanceof CheckBox).map(node -> {
                    if(node instanceof Label) return ((Label)node).getText();
                    else return ((CheckBox)node).getText();
                }).anyMatch(s -> s.equals(transformer.getText())))
                .map(hBox -> hBox.getChildren().stream()
                        .filter(node -> node instanceof TextInputControl)
                        .map(node -> ((TextInputControl)node))
                        .findFirst()
                        .orElseThrow(NullPointerException::new)
                )
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    CheckBox getCheckBox(Class<? extends ClassTransformer> transformerClass) {
        final var transformer = Objects.requireNonNull(TransformManager.createTransformerInstance(transformerClass));
        return this.getTabFromCategory(transformer.getCategory()).getChildren().stream()
                .filter(node -> node instanceof CheckBox || (node instanceof HBox && ((HBox)node).getChildren().stream().anyMatch(n -> n instanceof CheckBox)))
                .map(node -> {
                    if(node instanceof CheckBox) return (CheckBox) node;
                    else return (CheckBox) ((HBox)node).getChildren().stream()
                            .filter(n -> n instanceof CheckBox)
                            .findFirst()
                            .orElseThrow(NullPointerException::new);
                })
                .filter(checkBox -> checkBox.getText().equals(transformer.getText()))
                .findFirst()
                .orElseThrow(NullPointerException::new);
    }

    @SuppressWarnings("unchecked")
    ComboBox<String> getComboBox(Class<? extends ClassTransformer> transformerClass) {
        ClassTransformer transformer = Objects.requireNonNull(TransformManager.createTransformerInstance(transformerClass));
        return this.getTabFromCategory(transformer.getCategory()).getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox)node)
                .filter(hBox -> hBox.getChildren().stream().filter(node -> node instanceof Label).map(node -> ((Label)node).getText()).anyMatch(s -> s.equals(transformer.getText())))
                .map(hBox -> hBox.getChildren().stream()
                        .filter(node -> node instanceof ComboBox)
                        .map(node -> ((ComboBox<String>)node))
                        .findFirst()
                        .orElseThrow(NullPointerException::new)
                )
                .findFirst()
                .orElse(null);
    }

    Enum<?> getEnum(Class<? extends ClassTransformer> transformerClass) {
        ClassTransformer transformer = Objects.requireNonNull(TransformManager.createTransformerInstance(transformerClass));
        return getEnum(transformer.getName(), getComboBox(transformerClass).getSelectionModel().getSelectedItem());
    }

    private static Enum<?> getEnum(String transformerName, String enumName) {
        return TransformManager.getTransformers().stream()
                .map(TransformManager::createTransformerInstance)
                .filter(Objects::nonNull)
                .filter(ct -> ct.getName().equals(transformerName))
                .map(ct -> {
                            Object obj = ct.getEnableType().type();
                            if(List.class.isAssignableFrom(obj.getClass())) obj = ((List<?>)obj).get(0);
                            return EnumSet.allOf(((Enum<?>)obj).getDeclaringClass()).stream()
                                    .filter(anEnum -> Objects.requireNonNull(ByteObfUtils.getSerializedName(anEnum)).equals(enumName))
                                    .findFirst()
                                    .orElse(null);
                        }
                )
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(NullPointerException::new);
    }

    private VBox getTabFromCategory(ByteObfCategory category) {
        return this.optionsTab.getTabs().stream()
                .filter(tab -> tab.getText().equals(ByteObfUtils.getSerializedName(category)))
                .map(tab -> (VBox)tab.getContent())
                .findFirst()
                .orElseThrow(NullPointerException::new);
    }

    private static void mapComboBox(ComboBox<String> comboBox, Class<? extends Enum<?>> enumClass) {
        comboBox.getItems().addAll(Arrays.stream(enumClass.getEnumConstants())
                .map(ByteObfUtils::getSerializedName)
                .toList());
        comboBox.getSelectionModel().select(0);
    }

    public void log(String s) {
        Platform.runLater(() -> {
            console.getItems().add("[BYTEOBFGUI] " + s);
            console.scrollTo(console.getItems().size() - 1);
        });
    }
}