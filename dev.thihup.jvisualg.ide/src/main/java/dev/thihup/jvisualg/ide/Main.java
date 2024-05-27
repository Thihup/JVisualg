package dev.thihup.jvisualg.ide;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) throws AWTException {
        // Native image by default does not set the java.home
        if (System.getProperty("java.home") == null) {
            System.setProperty("java.home", ".");
        }
        // Native image does not support the new implementation based on FFM
        System.setProperty("sun.font.layout.ffm", "false");

        SwingUtilities.invokeLater(() -> new SwingIDE().setVisible(true));
    }

}
