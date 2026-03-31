package org.hexeditor.ui;

import org.hexeditor.document.HexDocument;
import org.hexeditor.model.HexViewport;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;

public class HexTableModel extends AbstractTableModel {
    private final HexDocument document;
    private final HexViewport viewport;

    public HexTableModel(HexDocument document, HexViewport viewport){
        this.document = document;
        this.viewport = viewport;
    }


    @Override
    public String getColumnName(int column){
        return String.format("%02X", column);
    }

    @Override
    public int getRowCount() {
        return viewport.getVisibleRows();
    }

    @Override
    public int getColumnCount(){
        return viewport.getBytesPerRow();
    }

    @Override
    public Object getValueAt(int  rowIndex, int columnIndex){
        long offset = viewport.getByteOffset(rowIndex, columnIndex);

        try{
            if(offset >= document.length()){
                return "";
            }

            int value = document.readByte(offset) & 0xFF;
            return String.format("%02X", value);
        } catch (IOException e){
            return "??"; //заглушка
        }
    }
}
