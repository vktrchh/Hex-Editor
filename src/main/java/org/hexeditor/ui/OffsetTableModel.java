package org.hexeditor.ui;

import org.hexeditor.model.HexViewport;


import javax.swing.table.AbstractTableModel;

public class OffsetTableModel extends AbstractTableModel {
    private final HexViewport viewport;

    public OffsetTableModel(HexViewport viewport){
        this.viewport = viewport;
    }

    @Override
    public int getRowCount() {
        return viewport.getVisibleRows();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return String.format("%08X", viewport.getRowOffset(rowIndex));
    }

    @Override
    public String getColumnName(int column) {
        return "Offset";
    }
}
