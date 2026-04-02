package org.hexeditor.ui;

import org.hexeditor.document.DeleteOption;
import org.hexeditor.document.FileHexDocument;
import org.hexeditor.document.InsertOption;
import org.hexeditor.editing.HexClipboard;
import org.hexeditor.model.EditorState;
import org.hexeditor.search.HexSearchService;
import org.hexeditor.search.MaskPattern;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/*
    Главное окно hex-редактора.
    Отвечает за сборку интерфейса, открытие, сохранения файла,
    навигацию, выделение байтов и за операции редактирования.
 */

public class MainFrame extends JFrame {
    private final JLabel saveStatus = new JLabel("Сохранено");

    private final ByteInfoPanel byteInfoPanel = new ByteInfoPanel();
    private final HexSearchService searchService = new HexSearchService();

    private final JToolBar toolBar = new JToolBar();
    private final MainMenuBar mainMenuBar = new MainMenuBar();

    private final JTable table = new JTable();
    private final JTable offsetTable = new JTable();
    private final JScrollPane scrollPane = new JScrollPane(table);
    private final HexClipboard clipboard = new HexClipboard();

    private final JTextField bytesPerRowField = new JTextField("16", 4);
    private final JTextField visibleRowsField = new JTextField("16", 4);

    private final EditorState editorState = new EditorState();

    public MainFrame() {
        initFrame();
        setJMenuBar(mainMenuBar);
        initToolBar();
        initTables();
        initLayout();
        initAction();
        syncTableAppearance();
        byteInfoPanel.setViewOffset(editorState.getHexViewport().getTableOffset());
    }

    private void initFrame(){
        setTitle("Hex Editor");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
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
        mainMenuBar.getOpenItem().addActionListener(e -> openFile());
        mainMenuBar.getSaveItem().addActionListener(e -> saveCurrentFile());
        mainMenuBar.getSaveAsItem().addActionListener(e -> saveFileAs());
        mainMenuBar.getExitItem().addActionListener(e -> attemptClose());

        mainMenuBar.getCopyItem().addActionListener(e -> copySelectedRange());
        mainMenuBar.getCutItem().addActionListener(e -> cutSelectedRange());
        mainMenuBar.getPasteItem().addActionListener(e -> pasteClipboard(InsertOption.SHIFT_RIGHT));
        mainMenuBar.getPasteOverwriteItem().addActionListener(e -> pasteClipboard(InsertOption.OVERWRITE));
        mainMenuBar.getInsertHexItem().addActionListener(e -> showInsertHexDialog());
        mainMenuBar.getDeleteItem().addActionListener(e -> deleteSelectedRange(DeleteOption.SHIFT_LEFT));
        mainMenuBar.getMakeZeroItem().addActionListener(e -> deleteSelectedRange(DeleteOption.ZERO_FILL));

        mainMenuBar.getSearchItem().addActionListener(e -> showSearchDialog());

        mainMenuBar.getStartItem().addActionListener(e -> moveToStart());
        mainMenuBar.getLineUpItem().addActionListener(e -> moveLineUp());
        mainMenuBar.getPageUpItem().addActionListener(e -> movePageUp());
        mainMenuBar.getPageDownItem().addActionListener(e -> movePageDown());
        mainMenuBar.getLineDownItem().addActionListener(e -> moveLineDown());
        mainMenuBar.getEndItem().addActionListener(e -> moveToEnd());

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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptClose();
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (row < 0 || column < 0) {
                    return;
                }

                long offset = editorState.getHexViewport().getByteOffset(row, column);
                if (!isOffsetInsideFile(offset)) {
                    return;
                }

                if (e.isShiftDown()) {
                    editorState.getSelectionModel().extendSelectionTo(offset);
                    applyRangeSelectionToTable();
                    updateSelectedByteInfo();
                } else {
                    selectSingleByte(offset);
                }
            }
        });
    }

    private void initToolBar(){
        toolBar.setFloatable(false);
        toolBar.add(new JLabel("Байт в строке:"));
        toolBar.add(bytesPerRowField);

        toolBar.addSeparator();

        toolBar.add(new JLabel("Строк:"));
        toolBar.add(visibleRowsField);
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(saveStatus);
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
        if (editorState.getCurrentDocument() == null) {
            return;
        }

        try {
            editorState.getCurrentDocument().close();
        } catch (IOException e) {
            showErrorMessage("Ошибка при закрытии файла: " + e.getMessage());
        } finally {
            editorState.clearDocumentState();
        }
    }

    private void loadFile(File file) {
        try {
            closeCurrentDocument();

            editorState.setCurrentDocument(new FileHexDocument(file));
            editorState.setCurrentFile(file);

            editorState.getHexViewport().setTableOffset(0);
            editorState.setCurrentHexModel(
                    new HexTableModel(editorState.getCurrentDocument(), editorState.getHexViewport())
            );
            editorState.setCurrentOffsetModel(
                    new OffsetTableModel(editorState.getHexViewport())
            );

            table.setModel(editorState.getCurrentHexModel());
            offsetTable.setModel(editorState.getCurrentOffsetModel());
            editorState.getCurrentHexModel().addTableModelListener(e -> {
                if (editorState.getSelectionModel().getSelectedByteOffset() >= 0) {
                    updateSelectedByteInfo();
                }
                updateModifiedStatus();
            });

            syncTableAppearance();

            table.clearSelection();
            offsetTable.clearSelection();

            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getRowHeader().setViewPosition(new Point(0, 0));

            byteInfoPanel.setViewOffset(editorState.getHexViewport().getTableOffset());

            if(isOffsetInsideFile(0)) {
                selectSingleByte(0);
            } else {
                clearSelectedByteInfo();
            }
            updateModifiedStatus();

        } catch (IOException e) {
            showErrorMessage("Ошибка при открытии файла: " + e.getMessage());
        }
    }

    private void openFile(){
        if(confirmSaveIfNeeded()){
            return;
        }
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

            editorState.getHexViewport().setBytesPerRow(bytesPerRow);
            editorState.getHexViewport().setVisibleRows(visibleRows);

            if (editorState.getCurrentHexModel() != null) {
                editorState.getCurrentHexModel().fireTableStructureChanged();

            }

            if (editorState.getCurrentOffsetModel() != null) {
                editorState.getCurrentOffsetModel().fireTableStructureChanged();
            }

            setViewportOffsetClamped(editorState.getHexViewport().getTableOffset());
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

    private void refreshDataView() {
        if (editorState.getCurrentHexModel() != null) {
            editorState.getCurrentHexModel().fireTableDataChanged();
        }

        if (editorState.getCurrentOffsetModel() != null) {
            editorState.getCurrentOffsetModel().fireTableDataChanged();
        }

        byteInfoPanel.setViewOffset(editorState.getHexViewport().getTableOffset());
        restoreSelectionIfVisible();
    }

    private long getMaxViewportOffset() {
        if (editorState.getCurrentDocument() == null) {
            return 0;
        }

        try {
            long fileLength = editorState.getCurrentDocument().length();
            long pageSize = editorState.getHexViewport().getPageBytesSize();

            if (fileLength <= pageSize) {
                return 0;
            }

            long maxOffset = fileLength - pageSize;
            return editorState.getHexViewport().alignOffsetToRowStart(maxOffset);
        } catch (IOException e) {
            showErrorMessage("Ошибка при получении размера файла: " + e.getMessage());
            return 0;
        }
    }

    private void setViewportOffsetClamped(long requestedOffset) {
        long maxOffset = getMaxViewportOffset();
        long clampedOffset = Math.max(0, Math.min(requestedOffset, maxOffset));

        clampedOffset = editorState.getHexViewport().alignOffsetToRowStart(clampedOffset);
        editorState.getHexViewport().setTableOffset(clampedOffset);

        refreshDataView();
    }
    private void moveToStart() {
        setViewportOffsetClamped(0);
    }

    private void moveLineUp() {
        long newOffset = editorState.getHexViewport().getTableOffset() - editorState.getHexViewport().getBytesPerRow();
        setViewportOffsetClamped(newOffset);
    }

    private void moveLineDown() {
        long newOffset = editorState.getHexViewport().getTableOffset() + editorState.getHexViewport().getBytesPerRow();
        setViewportOffsetClamped(newOffset);
    }

    private void movePageUp() {
        long newOffset = editorState.getHexViewport().getTableOffset() - editorState.getHexViewport().getPageBytesSize();
        setViewportOffsetClamped(newOffset);
    }

    private void movePageDown() {
        long newOffset = editorState.getHexViewport().getTableOffset() + editorState.getHexViewport().getPageBytesSize();
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

    private void clearSelectedByteInfo() {
        editorState.getSelectionModel().clear();
        byteInfoPanel.clearSelectionInfo();
    }

    private boolean isOffsetInsideFile(long offset) {
        if (editorState.getCurrentDocument() == null || offset < 0) {
            return false;
        }

        try {
            return offset < editorState.getCurrentDocument().length();
        } catch (IOException e) {
            showErrorMessage("Ошибка при получении размера файла: " + e.getMessage());
            return false;
        }
    }

    private void updateSelectedByteInfo() {
        long selectedByteOffset = editorState.getSelectionModel().getSelectedByteOffset();
        if (editorState.getCurrentDocument() == null || selectedByteOffset < 0) {
            clearSelectedByteInfo();
            return;
        }

        try {
            if (selectedByteOffset >= editorState.getCurrentDocument().length()) {
                clearSelectedByteInfo();
                return;
            }

            byte value = editorState.getCurrentDocument().readByte(selectedByteOffset);
            int unsignedValue = value & 0xFF;


            byteInfoPanel.showSingleByte(selectedByteOffset, unsignedValue, value);
            long blockLength = editorState.getSelectionModel().getSelectedRangeLength();
            blockLength = blockLength <= 1 ? 1: blockLength;

            byteInfoPanel.showBlockLength(blockLength);
            updateMultiByteInfo();

        } catch (IOException e) {
            showErrorMessage("Ошибка при чтении выбранного байта: " + e.getMessage());
            clearSelectedByteInfo();
        }
    }

    private void updateSelectedByteFromTable() {
        if (editorState.isSelectionUpdating() || editorState.getCurrentDocument() == null) {
            return;
        }

        int minRow = table.getSelectionModel().getMinSelectionIndex();
        int maxRow = table.getSelectionModel().getMaxSelectionIndex();
        int minCol = table.getColumnModel().getSelectionModel().getMinSelectionIndex();
        int maxCol = table.getColumnModel().getSelectionModel().getMaxSelectionIndex();

        if (minRow < 0 || maxRow < 0 || minCol < 0 || maxCol < 0) {
            clearSelectedByteInfo();
            return;
        }

        long minOffset = Long.MAX_VALUE;
        long maxOffset = Long.MIN_VALUE;
        long activeOffset = -1;

        int selectedRow = table.getSelectedRow();
        int selectedColumn = table.getSelectedColumn();

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (!table.isCellSelected(row, col)) {
                    continue;
                }

                long offset = editorState.getHexViewport().getByteOffset(row, col);
                if (!isOffsetInsideFile(offset)) {
                    continue;
                }

                minOffset = Math.min(minOffset, offset);
                maxOffset = Math.max(maxOffset, offset);

                if (row == selectedRow && col == selectedColumn) {
                    activeOffset = offset;
                }
            }
        }

        if (minOffset == Long.MAX_VALUE) {
            clearSelectedByteInfo();
            return;
        }

        if (activeOffset < 0) {
            activeOffset = minOffset;
        }

        long anchorOffset = editorState.getSelectionModel().getSelectionAnchorOffset();

        if (anchorOffset < 0 || !isOffsetInsideFile(anchorOffset)) {
            anchorOffset = minOffset;
        }
        editorState.getSelectionModel().setRange(activeOffset, anchorOffset, minOffset, maxOffset);

        updateSelectedByteInfo();
    }

    private void restoreSelectionIfVisible() {
        long selectedByteOffset = editorState.getSelectionModel().getSelectedByteOffset();
        if (selectedByteOffset < 0) {
            return;
        }

        long pageStart = editorState.getHexViewport().getTableOffset();
        long pageEnd = pageStart + editorState.getHexViewport().getPageBytesSize();

        if (!(selectedByteOffset >= pageStart && selectedByteOffset < pageEnd)) {
            editorState.setSelectionUpdating(true);
            try {
                table.clearSelection();
            } finally {
                editorState.setSelectionUpdating(false);
            }
            clearSelectedByteInfo();
            return;
        }

        editorState.setSelectionUpdating(true);
        try {
            applyRangeSelectionToTable();
            updateSelectedByteInfo();
        } finally {
            editorState.setSelectionUpdating(false);
        }
    }

    private byte[] readByteBlock(long startOffset, int size) throws IOException {
        if (editorState.getCurrentDocument() == null || startOffset < 0) {
            return null;
        }

        long fileLength = editorState.getCurrentDocument().length();
        if (startOffset + size > fileLength) {
            return null;
        }

        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = editorState.getCurrentDocument().readByte(startOffset + i);
        }

        return data;
    }

    private void updateMultiByteInfo() {
        if (editorState.getCurrentDocument() == null || editorState.getSelectionModel().getSelectedByteOffset() < 0) {
            byteInfoPanel.clearInt16Info();
            byteInfoPanel.clearInt32Info();
            byteInfoPanel.clearInt64Info();
            return;
        }

        try {
            byte[] block2 = readByteBlock(editorState.getSelectionModel().getSelectedByteOffset(), 2);
            if (block2 != null) {
                java.nio.ByteBuffer bb2 = java.nio.ByteBuffer.wrap(block2)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);

                short int16Value = bb2.getShort(0);
                int uint16Value = int16Value & 0xFFFF;

                byteInfoPanel.showInt16(int16Value, uint16Value);
            } else {
                byteInfoPanel.clearInt16Info();
            }

            byte[] block4 = readByteBlock(editorState.getSelectionModel().getSelectedByteOffset(), 4);
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

            byte[] block8 = readByteBlock(editorState.getSelectionModel().getSelectedByteOffset(), 8);
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

    private void navigateToFoundOffset(long foundOffset) {
        long rowStartOffset = editorState.getHexViewport().alignOffsetToRowStart(foundOffset);
        setViewportOffsetClamped(rowStartOffset);

        if (isOffsetInsideFile(foundOffset)) {
            selectSingleByte(foundOffset);
        } else {
            clearSelectedByteInfo();
        }
    }

    private void updateModifiedStatus() {
        if (editorState.getCurrentDocument() != null && editorState.getCurrentDocument().isModified()) {
            saveStatus.setText("Есть несохраненные изменения");
        } else {
            saveStatus.setText("Сохранено");
        }
    }

    private void saveFileAs() {
        if (ensureDocumentOpened()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            saveDocumentToTarget(file);
            showInfoMessage();
        } catch (IOException e) {
            showErrorMessage("Ошибка при сохранении файла: " + e.getMessage());
        }
    }

    private void saveCurrentFile() {
        if (ensureDocumentOpened()) {
            return;
        }

        if (editorState.getCurrentFile() == null) {
            saveFileAs();
            return;
        }

        try {
            saveDocumentToTarget(editorState.getCurrentFile());
            showInfoMessage();
        } catch (IOException e) {
            showErrorMessage("Ошибка при сохранении файла: " + e.getMessage());
        }
    }

    private boolean confirmSaveIfNeeded() {
        if (editorState.getCurrentDocument() == null || !editorState.getCurrentDocument().isModified()) {
            return false;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Есть несохраненные изменения. Сохранить?",
                "Несохраненные изменения",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return true;
        }

        if (choice == JOptionPane.YES_OPTION) {
            if (editorState.getCurrentFile() == null) {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showSaveDialog(this);

                if (result != JFileChooser.APPROVE_OPTION) {
                    return true;
                }

                File file = chooser.getSelectedFile();

                try {
                    saveDocumentToTarget(file);
                    return false;
                } catch (IOException e) {
                    showErrorMessage("Ошибка при сохранении файла: " + e.getMessage());
                    return true;
                }
            } else {
                try {
                    saveDocumentToTarget(editorState.getCurrentFile());
                    return false;
                } catch (IOException e) {
                    showErrorMessage("Ошибка при сохранении файла: " + e.getMessage());
                    return true;
                }
            }
        }

        return false;
    }

    private void attemptClose() {
        if (confirmSaveIfNeeded()) {
            return;
        }

        closeCurrentDocument();
        super.dispose();
    }

    private boolean isSameFile(File a, File b) {
        try {
            return a.getCanonicalFile().equals(b.getCanonicalFile());
        } catch (IOException e) {
            return a.getAbsoluteFile().equals(b.getAbsoluteFile());
        }
    }

    private void saveDocumentToTarget(File targetFile) throws IOException {
        if (editorState.getCurrentDocument() == null) {
            throw new IOException("Документ не открыт.");
        }

        boolean overwriteCurrent =
                editorState.getCurrentFile() != null && isSameFile(editorState.getCurrentFile(), targetFile);

        if (!overwriteCurrent) {
            editorState.getCurrentDocument().saveTo(targetFile);
            loadFile(targetFile);
            return;
        }

        File parentDir = targetFile.getAbsoluteFile().getParentFile();
        if (parentDir == null) {
            parentDir = new File(".");
        }

        File tempFile = File.createTempFile("hexedit_", ".tmp", parentDir);

        editorState.getCurrentDocument().saveTo(tempFile);

        closeCurrentDocument();

        Path tempPath = tempFile.toPath();
        Path targetPath = targetFile.toPath();

        try {
            Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        loadFile(targetFile);
    }

    private void showSearchDialog() {
        if (ensureDocumentOpened()) {
            return;
        }
        String input = JOptionPane.showInputDialog(
                this,
                "Введите последовательность байтов или маску. ?? - любой байт",
                "Поиск",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) {
            return;
        }

        input = input.trim();
        if (input.isEmpty()) {
            showWarningMessage("Введите последовательность байтов или маску для поиска.");
            return;
        }

        try {
            MaskPattern maskPattern = searchService.parseMaskPattern(input);
            long foundOffset = searchService.findMaskedPattern(editorState.getCurrentDocument(), maskPattern);

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

    private void selectSingleByte(long offset) {
        editorState.getSelectionModel().selectSingleByte(offset);
        applyRangeSelectionToTable();
        updateSelectedByteInfo();
    }

    private void applyRangeSelectionToTable() {
        long min = editorState.getSelectionModel().getRangeMinOffset();
        long max = editorState.getSelectionModel().getRangeMaxOffset();

        editorState.setSelectionUpdating(true);
        try {
            table.clearSelection();

            if (min < 0 || max < 0) {
                return;
            }

            long pageStart = editorState.getHexViewport().getTableOffset();
            long pageEnd = pageStart + editorState.getHexViewport().getPageBytesSize() - 1;

            long visibleStart = Math.max(min, pageStart);
            long visibleEnd = Math.min(max, pageEnd);

            if (visibleStart > visibleEnd) {
                return;
            }

            for (long offset = visibleStart; offset <= visibleEnd; offset++) {
                long relativeOffset = offset - pageStart;
                int row = (int) (relativeOffset /editorState.getHexViewport().getBytesPerRow());
                int column = (int) (relativeOffset % editorState.getHexViewport().getBytesPerRow());

                table.addRowSelectionInterval(row, row);
                table.addColumnSelectionInterval(column, column);
            }

        } finally {
            editorState.setSelectionUpdating(false);
        }
    }
    private void deleteSelectedRange(DeleteOption option) {
        if (ensureDocumentOpened()) {
            return;
        }

        long start = editorState.getSelectionModel().getRangeMinOffset();
        long length = editorState.getSelectionModel().getSelectedRangeLength();

        if (start < 0 || length <= 0) {
            showWarningMessage("Сначала выделите байт или диапазон.");
            return;
        }

        try {
            deleteRangeFromDocument(start, length, option);
        } catch (IOException e) {
            showErrorMessage("Ошибка при удалении: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        }
    }

    private byte[] readSelectedRangeBytes() throws IOException {
        long start = editorState.getSelectionModel().getRangeMinOffset();
        long length = editorState.getSelectionModel().getSelectedRangeLength();

        if (start < 0 || length <= 0) {
            throw new IllegalArgumentException("Нет выделенного диапазона.");
        }

        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Слишком большой диапазон для копирования.");
        }

        byte[] data = new byte[(int) length];

        for (int i = 0; i < data.length; i++) {
            data[i] = editorState.getCurrentDocument().readByte(start + i);
        }

        return data;
    }

    private void copySelectedRange() {
        if (ensureDocumentOpened()) {
            return;
        }

        try {
            byte[] data = readSelectedRangeBytes();
            clipboard.setBytes(data);
        } catch (IOException e) {
            showErrorMessage("Ошибка при копировании: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        }
    }

    private void deleteRangeFromDocument(long start, long length, DeleteOption option) throws IOException {
        editorState.getCurrentDocument().delete(start, length, option);

        if(option == DeleteOption.ZERO_FILL){
            editorState.getSelectionModel().updateAfterZeroFill(start,length, editorState.getCurrentDocument().length());
        } else {
            editorState.getSelectionModel().updateAfterShiftDelete(start, editorState.getCurrentDocument().length());
        }
        setViewportOffsetClamped(editorState.getHexViewport().getTableOffset());
        updateModifiedStatus();
    }

    private void cutSelectedRange() {
        if (ensureDocumentOpened()) {
            return;
        }
        long start = editorState.getSelectionModel().getRangeMinOffset();
        long length = editorState.getSelectionModel().getSelectedRangeLength();

        if (start < 0 || length <= 0) {
            showWarningMessage("Сначала выделите байт или диапазон.");
            return;
        }

        try {
            byte[] data = readSelectedRangeBytes();
            clipboard.setBytes(data);

            deleteRangeFromDocument(start, length, DeleteOption.SHIFT_LEFT);

        } catch (IOException e) {
            showErrorMessage("Ошибка при вырезании: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        }
    }

    private void pasteClipboard(InsertOption option) {
        if (ensureDocumentOpened()) {
            return;
        }

        if (!clipboard.hasData()) {
            showWarningMessage("Буфер пуст.");
            return;
        }

        byte[] data = clipboard.getBytes();

        try {
            insertBytesAtSelection(data, option);
        } catch (IOException e) {
            showErrorMessage("Ошибка при вставке: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        }
    }

    private void showInsertHexDialog() {
        if (ensureDocumentOpened()) {
            return;
        }

        JTextField hexField = new JTextField(20);
        JComboBox<InsertOption> modeBox = new JComboBox<>(InsertOption.values());
        modeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value == InsertOption.SHIFT_RIGHT) {
                    setText("Со сдвигом");
                } else if (value == InsertOption.OVERWRITE) {
                    setText("С заменой");
                }

                return this;
            }
        });

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.add(new JLabel("Введите байты в hex, например: AA FF 10"));
        panel.add(hexField);
        panel.add(new JLabel("Режим вставки:"));
        panel.add(modeBox);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Вставить hex-байты",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String input = hexField.getText().trim();
        if (input.isEmpty()) {
            showWarningMessage("Введите хотя бы один байт.");
            return;
        }

        try {
            byte[] data = searchService.parseExactPattern(input);
            InsertOption option = (InsertOption) modeBox.getSelectedItem();
            insertBytesAtSelection(data, option);
        } catch (IllegalArgumentException e) {
            showWarningMessage(e.getMessage());
        } catch (IOException e) {
            showErrorMessage("Ошибка при вставке: " + e.getMessage());
        }
    }

    private void insertBytesAtSelection(byte[] data, InsertOption option) throws IOException {
        long offset = editorState.getSelectionModel().getSelectedByteOffset() >= 0 ? editorState.getSelectionModel().getSelectedByteOffset() : 0;

        editorState.getCurrentDocument().insert(offset, data, option);

        editorState.getSelectionModel().updateAfterPaste(offset, data.length,  editorState.getCurrentDocument().length());
        applyRangeSelectionToTable();
        updateSelectedByteInfo();
        setViewportOffsetClamped(editorState.getHexViewport().getTableOffset());
        updateModifiedStatus();
    }

    private boolean ensureDocumentOpened() {
        if ( editorState.getCurrentDocument() != null) {
            return false;
        }
        showWarningMessage("Сначала откройте файл.");
        return true;
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

    private void showInfoMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Файл успешно сохранен.",
                "Информация",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}