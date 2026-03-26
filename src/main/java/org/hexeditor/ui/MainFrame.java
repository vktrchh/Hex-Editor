package org.hexeditor.ui;

import org.hexeditor.io.FileByteSource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {
    private JButton opnButton = new JButton("Открыть");
    private JToolBar toolBar = new JToolBar();
    private JTable table = new JTable();

    public MainFrame() {
        setTitle("Hex Editor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        add(new JScrollPane(table), BorderLayout.CENTER);

        toolBar.add(opnButton);
        add(toolBar, BorderLayout.NORTH);

        opnButton.addActionListener(e -> openFile());
    }

    private void openFile(){
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if(result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try {
                FileByteSource byteSource = new FileByteSource(file);
                table.setModel(new HexTableModel(byteSource));
            } catch (IOException e){
                JOptionPane.showMessageDialog(
                        this,
                        "Ошибка при открытии файла",
                        "ошибка",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
