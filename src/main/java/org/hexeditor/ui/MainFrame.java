package org.hexeditor.ui;

import org.hexeditor.document.FileHexDocument;
import org.hexeditor.document.HexDocument;
import org.hexeditor.model.HexViewport;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final JButton openButton = new JButton("Открыть");
    private final JButton findButton = new JButton("Найти");
    //private final JButton findByMask = new JButton("Поиск по маске");
    private final JButton startButton = new JButton("|<");
    private final JButton lineUpButton = new JButton("<");
    private final JButton pageUpButton = new JButton("<<");
    private final JButton pageDownButton = new JButton(">>");
    private final JButton lineDownButton = new JButton(">");
    private final JButton endButton = new JButton(">|");

    private final ByteInfoPanel byteInfoPanel = new ByteInfoPanel();

    private final JToolBar toolBar = new JToolBar();

    private final JTable table = new JTable();
    private final JTable offsetTable = new JTable();
    private final JScrollPane scrollPane = new JScrollPane(table);

    private final JTextField bytesPerRowField = new JTextField("16", 4);
    private final JTextField visibleRowsField = new JTextField("16", 4);

    private final HexViewport hexViewport = new HexViewport();

    private HexDocument currentDocument;
    private HexTableModel currentHexModel;
    private OffsetTableModel currentOffsetModel;

    private long selectedByteOffset = -1;
    private boolean selectionUpdating = false;

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
        add(byteInfoPanel, BorderLayout.SOUTH);
    }

    private void initAction(){
        openButton.addActionListener(e -> openFile());
        findButton.addActionListener(e-> showFindDialog());

        startButton.addActionListener(e -> moveToStart());
        lineUpButton.addActionListener(e -> moveLineUp());
        pageUpButton.addActionListener(e -> movePageUp());
        pageDownButton.addActionListener(e -> movePageDown());
        lineDownButton.addActionListener(e -> moveLineDown());
        endButton.addActionListener(e -> moveToEnd());

        bytesPerRowField.addActionListener(e -> applyViewportSettings());
        visibleRowsField.addActionListener(e -> applyViewportSettings());

        table.getSelectionModel().addListSelectionListener(e ->{
            if(!e.getValueIsAdjusting()) {
                updateSelectedByteFromTable();
            }
        });

        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                updateSelectedByteFromTable();
            }
        });
    }

    private void initToolBar(){
        toolBar.setFloatable(false);

        toolBar.add(openButton);
        toolBar.add(findButton);
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


    private void closeCurrentDocument() {
        if (currentDocument == null) {
            return;
        }

        try {
            currentDocument.close();
        } catch (IOException e) {
            showErrorMessage("Ошибка при закрытии файла: " + e.getMessage());
        } finally {
            currentDocument = null;
        }
    }

    private void loadFile(File file) {
        try {
            closeCurrentDocument();

            currentDocument = new FileHexDocument(file);

            hexViewport.setTableOffset(0);
            currentHexModel = new HexTableModel(currentDocument, hexViewport);
            currentOffsetModel = new OffsetTableModel(hexViewport);

            table.setModel(currentHexModel);
            offsetTable.setModel(currentOffsetModel);

            syncTableAppearance();

            table.clearSelection();
            offsetTable.clearSelection();

            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getRowHeader().setViewPosition(new Point(0, 0));

            updateOffsetLabel();

            if(isOffsetInsideFile(0)) {
                selectedByteOffset = 0;
                restoreSelectionIfVisible();
            } else {
                clearSelectedByteInfo();
            }

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
        byteInfoPanel.setViewOffset(hexViewport.getTableOffset());
    }

    private void refreshDataView() {
        if (currentHexModel != null) {
            currentHexModel.fireTableDataChanged();
        }

        if (currentOffsetModel != null) {
            currentOffsetModel.fireTableDataChanged();
        }

        updateOffsetLabel();
        restoreSelectionIfVisible();
    }

    private long getMaxViewportOffset() {
        if (currentDocument == null) {
            return 0;
        }

        try {
            long fileLength = currentDocument.length();
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
        closeCurrentDocument();
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

    private void clearSelectedByteInfo() {
        selectedByteOffset = -1;
        byteInfoPanel.clearSelectionInfo();
    }

    private boolean isOffsetInsideFile(long offset) {
        if (currentDocument == null || offset < 0) {
            return false;
        }

        try {
            return offset < currentDocument.length();
        } catch (IOException e) {
            showErrorMessage("Ошибка при получении размера файла: " + e.getMessage());
            return false;
        }
    }

    private void updateSelectedByteInfo() {
        if (currentDocument == null || selectedByteOffset < 0) {
            clearSelectedByteInfo();
            return;
        }

        try {
            if (selectedByteOffset >= currentDocument.length()) {
                clearSelectedByteInfo();
                return;
            }

            byte value = currentDocument.readByte(selectedByteOffset);
            int unsignedValue = value & 0xFF;


            byteInfoPanel.showSingleByte(selectedByteOffset, unsignedValue, value);
            updateMultiByteInfo();

        } catch (IOException e) {
            showErrorMessage("Ошибка при чтении выбранного байта: " + e.getMessage());
            clearSelectedByteInfo();
        }
    }

    private void updateSelectedByteFromTable() {
        if (selectionUpdating || currentDocument == null) {
            return;
        }

        int selectedRow = table.getSelectedRow();
        int selectedColumn = table.getSelectedColumn();

        if (selectedRow < 0 || selectedColumn < 0) {
            clearSelectedByteInfo();
            return;
        }

        long offset = hexViewport.getByteOffset(selectedRow, selectedColumn);

        if (!isOffsetInsideFile(offset)) {
            clearSelectedByteInfo();
            return;
        }

        selectedByteOffset = offset;
        updateSelectedByteInfo();
    }

    private boolean isSelectedByteVisibleInViewport() {
        if (selectedByteOffset < 0) {
            return false;
        }

        long pageStart = hexViewport.getTableOffset();
        long pageEnd = pageStart + hexViewport.getPageBytesSize();

        return selectedByteOffset >= pageStart && selectedByteOffset < pageEnd;
    }

    private void restoreSelectionIfVisible() {
        if (selectedByteOffset < 0) {
            return;
        }

        if (!isSelectedByteVisibleInViewport()) {
            selectionUpdating = true;
            try {
                table.clearSelection();
            } finally {
                selectionUpdating = false;
            }
            clearSelectedByteInfo();
            return;
        }

        long relativeOffset = selectedByteOffset - hexViewport.getTableOffset();
        int row = (int) (relativeOffset / hexViewport.getBytesPerRow());
        int column = (int) (relativeOffset % hexViewport.getBytesPerRow());

        selectionUpdating = true;
        try {
            table.changeSelection(row, column, false, false);
        } finally {
            selectionUpdating = false;
        }

        updateSelectedByteInfo();
    }

    private byte[] readByteBlock(long startOffset, int size) throws IOException {
        if (currentDocument == null || startOffset < 0) {
            return null;
        }

        long fileLength = currentDocument.length();
        if (startOffset + size > fileLength) {
            return null;
        }

        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = currentDocument.readByte(startOffset + i);
        }

        return data;
    }

    private void updateMultiByteInfo() {
        if (currentDocument == null || selectedByteOffset < 0) {
            byteInfoPanel.clearInt16Info();
            byteInfoPanel.clearInt32Info();
            byteInfoPanel.clearInt64Info();
            return;
        }

        try {
            byte[] block2 = readByteBlock(selectedByteOffset, 2);
            if (block2 != null) {
                java.nio.ByteBuffer bb2 = java.nio.ByteBuffer.wrap(block2)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);

                short int16Value = bb2.getShort(0);
                int uint16Value = int16Value & 0xFFFF;

                byteInfoPanel.showInt16(int16Value, uint16Value);
            } else {
                byteInfoPanel.clearInt16Info();
            }

            byte[] block4 = readByteBlock(selectedByteOffset, 4);
            if (block4 != null) {
                java.nio.ByteBuffer bb4 = java.nio.ByteBuffer.wrap(block4)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);

                int int32Value = bb4.getInt(0);
                long uint32Value = int32Value & 0xFFFFFFFFL;
                float floatValue = bb4.getFloat(0);

                byteInfoPanel.showInt32(int32Value, uint32Value, floatValue);
            } else {
                byteInfoPanel.clearInt32Info();
            }

            byte[] block8 = readByteBlock(selectedByteOffset, 8);
            if (block8 != null) {
                java.nio.ByteBuffer bb8 = java.nio.ByteBuffer.wrap(block8)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);

                long int64Value = bb8.getLong(0);
                String uint64Value = Long.toUnsignedString(int64Value);
                double doubleValue = bb8.getDouble(0);

                byteInfoPanel.showInt64(int64Value, uint64Value, doubleValue);
            } else {
                byteInfoPanel.clearInt64Info();
            }

        } catch (IOException e) {
            showErrorMessage("Ошибка при чтении блока байтов: " + e.getMessage());
            byteInfoPanel.clearInt16Info();
            byteInfoPanel.clearInt32Info();
            byteInfoPanel.clearInt64Info();
        }
    }

    private void showFindDialog() {
        String input = JOptionPane.showInputDialog(
                this,
                "Введите байты в hex",
                "Точный поиск",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) {
            return;
        }

        input = input.trim();
        if (input.isEmpty()) {
            showWarningMessage("Введите последовательность байтов для поиска.");
            return;
        }

        try {
            byte[] pattern = parseHexPattern(input);
            long foundOffset = findExactPattern(pattern);

            if (foundOffset < 0) {
                showWarningMessage("Совпадение не найдено.");
                return;
            }

            navigateToFoundOffset(foundOffset);

        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        } catch (IOException e) {
            showErrorMessage("Ошибка при поиске: " + e.getMessage());
        }
    }

    private byte[] parseHexPattern(String input) {
        String normalized = input.trim().replaceAll("\\s+", " ");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Строка поиска пуста.");
        }

        String[] parts = normalized.split(" ");

        byte[] result = new byte[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (part.length() != 2) {
                throw new IllegalArgumentException(
                        "Каждый байт должен быть записан двумя hex-символами."
                );
            }

            try {
                int value = Integer.parseInt(part, 16);
                result[i] = (byte) value;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Некорректное hex-значение: " + part
                );
            }
        }

        return result;
    }

    private long findExactPattern(byte[] pattern) throws IOException {
        if (currentDocument == null) {
            throw new IllegalStateException("Файл не открыт.");
        }

        if (pattern == null || pattern.length == 0) {
            return -1;
        }

        long fileLength = currentDocument.length();
        long maxStart = fileLength - pattern.length;

        for (long start = 0; start <= maxStart; start++) {
            boolean match = true;

            for (int i = 0; i < pattern.length; i++) {
                byte fileByte = currentDocument.readByte(start + i);
                if (fileByte != pattern[i]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return start;
            }
        }

        return -1;
    }

    private void navigateToFoundOffset(long foundOffset) {
        long rowStartOffset = hexViewport.alignOffsetToRowStart(foundOffset);
        setViewportOffsetClamped(rowStartOffset);

        if (isOffsetInsideFile(foundOffset)) {
            selectedByteOffset = foundOffset;
            restoreSelectionIfVisible();
        } else {
            clearSelectedByteInfo();
        }
    }

}
