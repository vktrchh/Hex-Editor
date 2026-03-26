package org.hexeditor;

import org.hexeditor.io.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->{
            new MainFrame().setVisible(true);
        });
    }
}