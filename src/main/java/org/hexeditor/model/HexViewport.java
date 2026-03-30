package org.hexeditor.model;

public class HexViewport {
    private long tableOffset = 0;
    private int bytesPerRow = 16;
    private int visibleRows = 16;



    public long getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(long tableOffset) {
        this.tableOffset = (tableOffset >= 0) ? tableOffset : 0;
    }

    public int getBytesPerRow() {
        return bytesPerRow;
    }

    public void setBytesPerRow(int bytesPerRow) {
        if(bytesPerRow <= 0){
            throw new IllegalArgumentException("Количество байтов должно быть больше нуля");
        }

        this.bytesPerRow = bytesPerRow;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public void setVisibleRows(int visibleRows) {
        if(visibleRows <= 0){
            throw new IllegalArgumentException("Количество отображаемых строк должно быть больше нуля");
        }

        this.visibleRows = visibleRows;
    }

    public long getRowOffset(int rowIndex) {
        return tableOffset + (long) rowIndex * bytesPerRow;
    }

    public long getByteOffset(int rowIndex, int columnIndex) {
        return getRowOffset(rowIndex) + columnIndex;
    }

    public int getPageBytesSize() {
        return visibleRows * bytesPerRow;
    }

    public long alignOffsetToRowStart(long offset) {
        if (offset <= 0) {
            return 0;
        }
        return offset - (offset % bytesPerRow);
    }
}
