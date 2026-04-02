package org.hexeditor.document;

import org.hexeditor.io.FileByteSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/*
    Класс хранит текущее состояние редактируемого документа.
    Данные хранятся как набор сегментов из исходного файла и байтов добавленных при редактировании.
 */

public class FileHexDocument implements HexDocument {
    private final FileByteSource byteSource;
    private final AddBuffer addBuffer = new AddBuffer();
    private final List<Segment> segments = new ArrayList<>();

    private long logicalLength;
    private boolean modified;


    public FileHexDocument(File file) throws IOException {
        this.byteSource = new FileByteSource(file);
        this.segments.add(new Segment(SegmentType.SOURCE, 0, byteSource.length()));
        this.logicalLength = byteSource.length();
        this.modified = false;
    }

    @Override
    public long length() throws IOException {
        return logicalLength;
    }

    @Override
    public byte readByte(long offset) throws IOException {
        if (offset < 0 || offset >= logicalLength) {
            throw new IllegalArgumentException("Смещение вне границ документа.");
        }

        SegmentLocation location = locateSegment(offset);
        Segment segment = segments.get(location.segmentIndex);

        long physicalOffset = segment.getStart() + location.offsetInsideSegment;

        if (segment.getType() == SegmentType.SOURCE) {
            return byteSource.readByte(physicalOffset);
        } else {
            return addBuffer.readByte(physicalOffset);
        }
    }

    @Override
    public void writeByte(long offset, byte value) throws IOException {
        if(offset< 0 || offset >= logicalLength){
            throw new IllegalArgumentException();
        }

        byte currentValue = readByte(offset);

        if(currentValue == value){
            return;
        }
        replaceRange(offset, 1, new byte[]{value});

    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void close() throws IOException {
        byteSource.close();
    }

    @Override
    public void saveTo(File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            final int bufferSize = 8192;
            byte[] temp = new byte[bufferSize];

            for (Segment segment : segments) {
                long remaining = segment.getLength();
                long sourceOffset = segment.getStart();

                while (remaining > 0) {
                    int chunk = (int) Math.min(bufferSize, remaining);

                    for (int i = 0; i < chunk; i++) {
                        long absoluteOffset = sourceOffset + i;

                        if (segment.getType() == SegmentType.SOURCE) {
                            temp[i] = byteSource.readByte(absoluteOffset);
                        } else {
                            temp[i] = addBuffer.readByte(absoluteOffset);
                        }
                    }

                    out.write(temp, 0, chunk);

                    sourceOffset += chunk;
                    remaining -= chunk;
                }
            }
        }
    }

    @Override
    public void delete(long startOffset, long length, DeleteOption deleteOption)  throws IOException{
        if (startOffset < 0 || length < 0) {
            throw new IllegalArgumentException("Отрицательные значения недопустимы.");
        }

        if (startOffset >= logicalLength) {
            throw new IllegalArgumentException("Начало удаления вне границ документа.");
        }

        long actualLength = Math.min(length, logicalLength - startOffset);
        if (actualLength == 0) {
            return;
        }

        if (deleteOption == DeleteOption.SHIFT_LEFT) {
            replaceRange(startOffset, actualLength, new byte[0]);
            return;
        }

        if (deleteOption == DeleteOption.ZERO_FILL) {
            zeroFillRange(startOffset, actualLength);
            return;
        }

        throw new IllegalArgumentException("Неизвестный режим удаления.");
    }

    private void zeroFillRange(long startOffset, long length) throws IOException {
        final int chunkSize = 8192;
        byte[] zeros = new byte[chunkSize];

        long remaining = length;
        long currentOffset = startOffset;

        while (remaining > 0) {
            int chunk = (int) Math.min(chunkSize, remaining);
            byte[] chunkBytes = (chunk == chunkSize) ? zeros : Arrays.copyOf(zeros, chunk);

            replaceRange(currentOffset, chunk, chunkBytes);

            currentOffset += chunk;
            remaining -= chunk;
        }
    }


    private static class SegmentLocation {
        private final int segmentIndex;
        private final long offsetInsideSegment;

        private SegmentLocation(int segmentIndex, long offsetInsideSegment) {
            this.segmentIndex = segmentIndex;
            //this.segmentLogicalStart = segmentLogicalStart;
            this.offsetInsideSegment = offsetInsideSegment;
        }
    }

    private SegmentLocation locateSegment(long logicalOffset) throws IOException {
        long cursor = 0;

        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            long nextCursor = cursor + segment.getLength();

            if (logicalOffset < nextCursor) {
                return new SegmentLocation(i, logicalOffset - cursor);
            }

            cursor = nextCursor;
        }

        throw new IOException("Не удалось определить сегмент для logicalOffset=" + logicalOffset);
    }

    private void splitAt(long logicalOffset) throws IOException {
        if (logicalOffset <= 0 || logicalOffset >= logicalLength) {
            return;
        }

        SegmentLocation location = locateSegment(logicalOffset);
        Segment segment = segments.get(location.segmentIndex);

        if (location.offsetInsideSegment == 0 || location.offsetInsideSegment == segment.getLength()) {
            return;
        }

        Segment left = new Segment(
                segment.getType(),
                segment.getStart(),
                location.offsetInsideSegment
        );

        Segment right = new Segment(
                segment.getType(),
                segment.getStart() + location.offsetInsideSegment,
                segment.getLength() - location.offsetInsideSegment
        );

        segments.set(location.segmentIndex, left);
        segments.add(location.segmentIndex + 1, right);
    }

    private void mergeAdjacentSegments() {
        if (segments.isEmpty()) {
            return;
        }

        List<Segment> merged = new ArrayList<>();
        Segment current = segments.get(0);

        for (int i = 1; i < segments.size(); i++) {
            Segment next = segments.get(i);

            boolean sameType = current.getType() == next.getType();
            boolean contiguous = current.getStart() + current.getLength() == next.getStart();

            if (sameType && contiguous) {
                current.setLength(current.getLength() + next.getLength());
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);

        segments.clear();
        segments.addAll(merged);
    }

    private void replaceRange(long startOffset, long deleteLength, byte[] insertedBytes) throws IOException {
        if (startOffset < 0 || deleteLength < 0) {
            throw new IllegalArgumentException("Отрицательные значения недопустимы.");
        }

        if (startOffset > logicalLength) {
            throw new IllegalArgumentException("Начало операции вне границ документа.");
        }

        long boundedDeleteLength = Math.min(deleteLength, logicalLength - startOffset);

        splitAt(startOffset);
        splitAt(startOffset + boundedDeleteLength);

        int startIndex = findSegmentIndexAtBoundary(startOffset);
        int endIndex = findSegmentIndexAtBoundary(startOffset + boundedDeleteLength);

        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
            throw new IllegalStateException("Не удалось корректно выделить диапазон сегментов.");
        }

        segments.subList(startIndex, endIndex).clear();

        if (insertedBytes != null && insertedBytes.length > 0) {
            long addStart = addBuffer.append(insertedBytes);
            segments.add(startIndex, new Segment(SegmentType.ADDED, addStart, insertedBytes.length));
        }

        logicalLength = logicalLength - boundedDeleteLength + (insertedBytes == null ? 0 : insertedBytes.length);
        modified = true;

        mergeAdjacentSegments();
    }
    private int findSegmentIndexAtBoundary(long logicalOffset) {
        long cursor = 0;

        for (int i = 0; i < segments.size(); i++) {
            if (cursor == logicalOffset) {
                return i;
            }

            cursor += segments.get(i).getLength();
        }

        if (cursor == logicalOffset) {
            return segments.size();
        }

        return -1;
    }

    @Override
    public void insert(long offset, byte[] data, InsertOption insertOption) throws IOException {
        if (data == null || data.length == 0) {
            return;
        }

        if (offset < 0 || offset > logicalLength) {
            throw new IllegalArgumentException("Смещение вне границ документа.");
        }

        if (insertOption == InsertOption.SHIFT_RIGHT) {
            replaceRange(offset, 0, data);
            return;
        }

        if (insertOption == InsertOption.OVERWRITE) {
            long overwriteLength = Math.min(data.length, logicalLength - offset);
            replaceRange(offset, overwriteLength, data);
            return;
        }

        throw new IllegalArgumentException("Неизвестный режим вставки.");
    }
}