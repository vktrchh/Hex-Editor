package org.hexeditor.document;

/*
    Класс описание сегмента документа
    Хранит источник, длину и начальное смещение.
 */
public class Segment {
    private final SegmentType type;
    private final long start;
    private long length;

    public Segment(SegmentType type, long start, long length) {
        this.type = type;
        this.start = start;
        this.length = length;
    }

    public SegmentType getType() {
        return type;
    }

    public long getStart() {
        return start;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}