package org.hexeditor.ui;

import org.hexeditor.io.ByteSource;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;

public class HexTableModel extends AbstractTableModel {
    private final ByteSource byteSource;
    private final int rows = 16;
    private final int cols = 16;

    public HexTableModel(ByteSource byteSource){
        this.byteSource = byteSource;
    }


    @Override
    public String getColumnName(int column){
        return String.format("%02X", column);
    }

    @Override
    public int getRowCount() {
        return rows;
    }

    @Override
    public int getColumnCount(){
        return cols;
    }

    @Override
    public Object getValueAt(int  rowIndex, int columIndex){
        long offset = rowIndex * cols + columIndex;

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
