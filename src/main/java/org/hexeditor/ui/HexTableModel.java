package org.hexeditor.ui;

import org.hexeditor.document.HexDocument;
import org.hexeditor.model.HexViewport;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;


/*
    Модель главной таблицы.
    Отображает байты документа в 16-ных значениях
    поддерживает редактирование байтов через ячейки таблицы.
 */
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

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        long offset = viewport.getByteOffset(rowIndex, columnIndex);

        try {
            return offset < document.length();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        long offset = viewport.getByteOffset(rowIndex, columnIndex);

        try {
            if (offset >= document.length()) {
                return;
            }

            byte newValue = parseHexByte(aValue);
            document.writeByte(offset, newValue);
            fireTableCellUpdated(rowIndex, columnIndex);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи байта: " + e.getMessage(), e);
        }
    }

    private byte parseHexByte(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Значение не введено.");
        }

        String text = value.toString().trim().toUpperCase();

        if (text.length() != 2) {
            throw new IllegalArgumentException("Байт должен состоять из двух hex-символов.");
        }

        try {
            int parsed = Integer.parseInt(text, 16);
            return (byte) parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректное hex-значение: " + text);
        }
    }
}
