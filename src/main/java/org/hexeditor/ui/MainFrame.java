package org.hexeditor.ui;

import org.hexeditor.io.ByteSource;
import org.hexeditor.io.FileByteSource;
import org.hexeditor.model.HexViewport;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final JButton openButton = new JButton("Открыть");
    private final JButton startButton = new JButton("|<");
    private final JButton lineUpButton = new JButton("<");
    private final JButton pageUpButton = new JButton("<<");
    private final JButton pageDownButton = new JButton(">>");
    private final JButton lineDownButton = new JButton(">");
    private final JButton endButton = new JButton(">|");
    private final JLabel offsetInfoLabel = new JLabel("Offset: 00000000");
    private final JToolBar toolBar = new JToolBar();

    private final JTable table = new JTable();
    private final JTable offsetTable = new JTable();
    private final JScrollPane scrollPane = new JScrollPane(table);

    private final JTextField bytesPerRowField = new JTextField("16", 4);
    private final JTextField visibleRowsField = new JTextField("16", 4);

    private final HexViewport hexViewport = new HexViewport();

    private ByteSource currentByteSource;
    private HexTableModel currentHexModel;
    private OffsetTableModel currentOffsetModel;

    public MainFrame() {
        initFrame();
        initToolBar();
        initTables();
        initLayout();
        initAction();
        syncTableAppearance();
        updateOffsetLabel();
    }

    private void initFrame(){
        setTitle("Hex Editor");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
    }

    private void initLayout(){
        add(scrollPane, BorderLayout.CENTER);
        add(toolBar, BorderLayout.NORTH);
        add(offsetInfoLabel, BorderLayout.SOUTH);
    }

    private void initAction(){
        openButton.addActionListener(e -> openFile());

        startButton.addActionListener(e -> moveToStart());
        lineUpButton.addActionListener(e -> moveLineUp());
        pageUpButton.addActionListener(e -> movePageUp());
        pageDownButton.addActionListener(e -> movePageDown());
        lineDownButton.addActionListener(e -> moveLineDown());
        endButton.addActionListener(e -> moveToEnd());

        bytesPerRowField.addActionListener(e -> applyViewportSettings());
        visibleRowsField.addActionListener(e -> applyViewportSettings());

    }

    private void initToolBar(){
        toolBar.setFloatable(false);

        toolBar.add(openButton);
        toolBar.addSeparator();

        toolBar.add(startButton);
        toolBar.add(lineDownButton);
        toolBar.add(lineUpButton);
        toolBar.add(pageDownButton);
        toolBar.add(pageUpButton);
        toolBar.add(endButton);

        toolBar.addSeparator();

        toolBar.add(new JLabel("Байт в строке:"));
        toolBar.add(bytesPerRowField);

        toolBar.addSeparator();

        toolBar.add(new JLabel("Строк:"));
        toolBar.add(visibleRowsField);
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

    private void closeCurrentSource() {
        if (currentByteSource == null) {
            return;
        }

        try {
            currentByteSource.close();
        } catch (IOException e) {
            showErrorMessage("Ошибка при закрытии файла: " + e.getMessage());
        } finally {
            currentByteSource = null;
        }
    }

    private void loadFile(File file) {
        try {
            closeCurrentSource();

            currentByteSource = new FileByteSource(file);

            hexViewport.setTableOffset(0);
            currentHexModel = new HexTableModel(currentByteSource, hexViewport);
            currentOffsetModel = new OffsetTableModel(hexViewport);

            table.setModel(currentHexModel);
            offsetTable.setModel(currentOffsetModel);

            syncTableAppearance();

            table.clearSelection();
            offsetTable.clearSelection();

            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getRowHeader().setViewPosition(new Point(0, 0));

            updateOffsetLabel();

        } catch (IOException e) {
            showErrorMessage("Ошибка при открытии файла: " + e.getMessage());
        }
    }

    private void openFile(){
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if(result != JFileChooser.APPROVE_OPTION) {
           return;
        }
        File file = chooser.getSelectedFile();
        loadFile(file);
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

            setViewportOffsetClamped(hexViewport.getTableOffset());
            syncTableAppearance();

            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getRowHeader().setViewPosition(new Point(0, 0));

        } catch (NumberFormatException e) {
            showWarningMessage("Введите целые числа для количества байтов в строке и количества строк.");
        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        }
    }

    private void syncTableAppearance() {
        table.setRowHeight(22);
        offsetTable.setRowHeight(22);

        int offsetWidth = 90;

        if (offsetTable.getColumnModel().getColumnCount() > 0) {
            offsetTable.getColumnModel().getColumn(0).setPreferredWidth(offsetWidth);
            offsetTable.getColumnModel().getColumn(0).setMinWidth(offsetWidth);
            offsetTable.getColumnModel().getColumn(0).setMaxWidth(offsetWidth);
        }

        offsetTable.setPreferredScrollableViewportSize(new Dimension(offsetWidth, 0));
        offsetTable.setPreferredSize(new Dimension(offsetWidth, offsetTable.getPreferredSize().height));
        scrollPane.getRowHeader().setPreferredSize((new Dimension(offsetWidth, 0)));

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(42);
            table.getColumnModel().getColumn(i).setMinWidth(42);
        }

        table.revalidate();
        offsetTable.revalidate();
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    private void updateOffsetLabel() {
        offsetInfoLabel.setText(String.format("Offset: %08X", hexViewport.getTableOffset()));
    }

    private void refreshDataView() {
        if (currentHexModel != null) {
            currentHexModel.fireTableDataChanged();
        }

        if (currentOffsetModel != null) {
            currentOffsetModel.fireTableDataChanged();
        }

        updateOffsetLabel();
    }

    private long getMaxViewportOffset() {
        if (currentByteSource == null) {
            return 0;
        }

        try {
            long fileLength = currentByteSource.length();
            long pageSize = hexViewport.getPageBytesSize();

            if (fileLength <= pageSize) {
                return 0;
            }

            long maxOffset = fileLength - pageSize;
            return hexViewport.alignOffsetToRowStart(maxOffset);
        } catch (IOException e) {
            showErrorMessage("Ошибка при получении размера файла: " + e.getMessage());
            return 0;
        }
    }

    private void setViewportOffsetClamped(long requestedOffset) {
        long maxOffset = getMaxViewportOffset();

        long clampedOffset = requestedOffset;

        if (clampedOffset < 0) {
            clampedOffset = 0;
        }

        if (clampedOffset > maxOffset) {
            clampedOffset = maxOffset;
        }

        clampedOffset = hexViewport.alignOffsetToRowStart(clampedOffset);
        hexViewport.setTableOffset(clampedOffset);

        refreshDataView();
    }
    private void moveToStart() {
        setViewportOffsetClamped(0);
    }

    private void moveLineUp() {
        long newOffset = hexViewport.getTableOffset() - hexViewport.getBytesPerRow();
        setViewportOffsetClamped(newOffset);
    }

    private void moveLineDown() {
        long newOffset = hexViewport.getTableOffset() + hexViewport.getBytesPerRow();
        setViewportOffsetClamped(newOffset);
    }

    private void movePageUp() {
        long newOffset = hexViewport.getTableOffset() - hexViewport.getPageBytesSize();
        setViewportOffsetClamped(newOffset);
    }

    private void movePageDown() {
        long newOffset = hexViewport.getTableOffset() + hexViewport.getPageBytesSize();
        setViewportOffsetClamped(newOffset);
    }

    private void moveToEnd() {
        setViewportOffsetClamped(getMaxViewportOffset());
    }

    @Override
    public void dispose() {
        closeCurrentSource();
        super.dispose();
    }

    private void showErrorMessage(String message){
        JOptionPane.showMessageDialog(
                this,
                message,
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void showWarningMessage(String  message){
        JOptionPane.showMessageDialog(
                this,
                message,
                "Предупреждение",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
