package org.hexeditor.model;

/*
    Класс для работы с выделением байтов.
 */

public class SelectionModel {
    private long selectedByteOffset = -1;
    private long selectionAnchorOffset = -1;
    private long rangeStartOffset = -1;
    private long rangeEndOffset = -1;

    public long getSelectedByteOffset() {
        return selectedByteOffset;
    }

    public long getSelectionAnchorOffset() {
        return selectionAnchorOffset;
    }

    public void clear() {
        selectedByteOffset = -1;
        selectionAnchorOffset = -1;
        rangeStartOffset = -1;
        rangeEndOffset = -1;
    }

    public void selectSingleByte(long offset) {
        selectedByteOffset = offset;
        selectionAnchorOffset = offset;
        rangeStartOffset = offset;
        rangeEndOffset = offset;
    }

    public void extendSelectionTo(long offset) {
        if (selectionAnchorOffset < 0) {
            selectSingleByte(offset);
            return;
        }

        selectedByteOffset = offset;
        rangeStartOffset = selectionAnchorOffset;
        rangeEndOffset = offset;
    }

    public void setRange(long selectedByteOffset, long selectionAnchorOffset, long rangeStartOffset, long rangeEndOffset) {
        this.selectedByteOffset = selectedByteOffset;
        this.selectionAnchorOffset = selectionAnchorOffset;
        this.rangeStartOffset = rangeStartOffset;
        this.rangeEndOffset = rangeEndOffset;
    }

    public long getRangeMinOffset() {
        if (rangeStartOffset < 0 || rangeEndOffset < 0) {
            return -1;
        }
        return Math.min(rangeStartOffset, rangeEndOffset);
    }

    public long getRangeMaxOffset() {
        if (rangeStartOffset < 0 || rangeEndOffset < 0) {
            return -1;
        }
        return Math.max(rangeStartOffset, rangeEndOffset);
    }

    public long getSelectedRangeLength() {
        long min = getRangeMinOffset();
        long max = getRangeMaxOffset();

        if (min < 0 || max < 0) {
            return 0;
        }
        return max - min + 1;
    }

    public boolean clearIfDocumentEmpty(long docLength) {
        if (docLength == 0) {
            clear();
            return true;
        }
        return false;
    }

    public void updateAfterZeroFill(long start, long deletedLength, long docLength) {
        if (clearIfDocumentEmpty(docLength)) {
            return;
        }

        long newEnd = Math.min(start + deletedLength - 1, docLength - 1);
        setRange(start, start, start, newEnd);
    }

    public void updateAfterShiftDelete(long start, long docLength) {
        if (clearIfDocumentEmpty(docLength)) {
            return;
        }

        long newOffset = Math.min(start, docLength - 1);
        selectSingleByte(newOffset);
    }

    public void updateAfterPaste(long startOffset, long insertedLength, long docLength) {
        if (clearIfDocumentEmpty(docLength)) {
            return;
        }

        long newEnd = Math.min(startOffset + insertedLength - 1, docLength - 1);
        setRange(startOffset, startOffset, startOffset, newEnd);
    }
}