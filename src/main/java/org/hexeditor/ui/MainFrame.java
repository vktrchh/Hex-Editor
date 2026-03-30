package org.hexeditor.ui;

import org.hexeditor.io.FileByteSource;
import org.hexeditor.model.HexViewport;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {
    private JButton openButton = new JButton("Открыть");
    private JToolBar toolBar = new JToolBar();

    private JTable table = new JTable();
    private JTable offsetTable = new JTable();
    private JScrollPane scrollPane = new JScrollPane(table);

    private final JTextField bytesPerRowField = new JTextField("16", 4);
    private final JTextField visibleRowsField = new JTextField("16", 4);

    private final HexViewport hexViewport = new HexViewport();

    private FileByteSource currentByteSource;
    private HexTableModel currentHexModel;
    private OffsetTableModel currentOffsetModel;

    public MainFrame() {
        setTitle("Hex Editor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        initToolBar();
        initTables();

        add(scrollPane, BorderLayout.CENTER);
        add(toolBar, BorderLayout.NORTH);

        openButton.addActionListener(e -> openFile());
    }

    private void initToolBar(){
        toolBar.setFloatable(false);

        toolBar.add(openButton);
        toolBar.addSeparator();

        toolBar.add(new JLabel("Байт/строка:"));
        toolBar.add(bytesPerRowField);

        toolBar.addSeparator();

        toolBar.add(new JLabel("Строк:"));
        toolBar.add(visibleRowsField);

        //openButton.addActionListener(e -> openFile());
        bytesPerRowField.addActionListener(e -> applyViewportSettings());
        visibleRowsField.addActionListener(e -> applyViewportSettings());
    }

    private void initTables() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setFillsViewportHeight(true);

        offsetTable.setEnabled(false);
        offsetTable.setFocusable(false);
        offsetTable.setRowSelectionAllowed(false);
        offsetTable.setCellSelectionEnabled(false);
        offsetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        offsetTable.setFillsViewportHeight(true);

        scrollPane.setRowHeaderView(offsetTable);
    }

    private void openFile(){
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if(result != JFileChooser.APPROVE_OPTION) {
           return;
        }
        File file = chooser.getSelectedFile();

        try {
            if (currentByteSource != null) {
                currentByteSource.close();
            }

            currentByteSource = new FileByteSource(file);

            hexViewport.setTableOffset(0);
            currentHexModel = new HexTableModel(currentByteSource, hexViewport);
            currentOffsetModel = new OffsetTableModel(hexViewport);

            table.setModel(currentHexModel);
            offsetTable.setModel(currentOffsetModel);
            syncTableAppearance();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ошибка при открытии файла: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void applyViewportSettings(){
        try {
            int bytesPerRow = Integer.parseInt(bytesPerRowField.getText().trim());
            int visibleRows = Integer.parseInt(visibleRowsField.getText().trim());

            hexViewport.setBytesPerRow(bytesPerRow);
            hexViewport.setVisibleRows(visibleRows);

            if (currentHexModel != null) {
                currentHexModel.fireTableStructureChanged();
            }

            if (currentOffsetModel != null) {
                currentOffsetModel.fireTableStructureChanged();
            }

            syncTableAppearance();

            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getRowHeader().setViewPosition(new Point(0, 0));

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Введите целые числа для количества байтов в строке и количества строк.",
                    "Ошибка ввода",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Некорректные параметры",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void syncTableAppearance() {
        table.setRowHeight(22);
        offsetTable.setRowHeight(22);

        if (offsetTable.getColumnModel().getColumnCount() > 0) {
            offsetTable.getColumnModel().getColumn(0).setPreferredWidth(90);
            offsetTable.getColumnModel().getColumn(0).setMinWidth(90);
            offsetTable.getColumnModel().getColumn(0).setMaxWidth(90);
        }

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(42);
            table.getColumnModel().getColumn(i).setMinWidth(42);
        }

        table.revalidate();
        offsetTable.revalidate();

        scrollPane.revalidate();
        scrollPane.repaint();
    }
}
