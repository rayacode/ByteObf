package io.github.vimasig.bozar.ui;

/**
 * A standard Java main class to launch the JavaFX application.
 * This is necessary for non-modular applications packaged as a "fat JAR".
 */
public class Launcher {
    public static void main(String[] args) {
        App.launch(App.class, args);
    }
}