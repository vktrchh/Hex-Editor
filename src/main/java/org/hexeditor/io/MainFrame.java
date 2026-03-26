package org.hexeditor.io;

import org.hexeditor.Main;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JButton opnButton = new JButton("Открыть");
    private JToolBar toolBar = new JToolBar();


    public MainFrame() {
        setTitle("MainFrame");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        JTable table = new JTable(16, 16);
        add(new JScrollPane(table), BorderLayout.CENTER);

        toolBar.add(opnButton);
        add(toolBar, BorderLayout.NORTH);

    }
}
