package org.hexeditor.document;

public class Segment {
    private SegmentType type;
    private long start;
    private long length;

    public Segment(SegmentType type, long start, long length) {
        this.type = type;
        this.start = start;
        this.length = length;
    }

    public SegmentType getType() {
        return type;
    }

    public void setType(SegmentType type) {
        this.type = type;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}