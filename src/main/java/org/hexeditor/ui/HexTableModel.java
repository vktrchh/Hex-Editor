package org.hexeditor.ui;

import org.hexeditor.io.ByteSource;
import org.hexeditor.model.HexViewport;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;

public class HexTableModel extends AbstractTableModel {
    private final ByteSource byteSource;
    HexViewport viewport;

    public HexTableModel(ByteSource byteSource, HexViewport viewport){
        this.byteSource = byteSource;
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
            if(offset >= byteSource.length()){
                return "";
            }

            int value = byteSource.readByte(offset) & 0xFF;
            return String.format("%02X", value);
        } catch (IOException e){
            return "??"; //заглушка
        }
    }
}
