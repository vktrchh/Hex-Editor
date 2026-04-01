package org.hexeditor.document;

import java.util.Arrays;

public class AddBuffer {
    private byte[] data = new byte[1024];
    private int size = 0;

    public long append(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return size;
        }

        ensureCapacity(size + bytes.length);

        long startOffset = size;
        System.arraycopy(bytes, 0, data, size, bytes.length);
        size += bytes.length;

        return startOffset;
    }

    public byte readByte(long offset) {
        if (offset < 0 || offset >= size) {
            throw new IllegalArgumentException("Смещение вне границ add-buffer.");
        }

        return data[(int) offset];
    }

    public int size() {
        return size;
    }

    private void ensureCapacity(int needed) {
        if (needed <= data.length) {
            return;
        }

        int newCapacity = data.length;
        while (newCapacity < needed) {
            newCapacity *= 2;
        }

        data = Arrays.copyOf(data, newCapacity);
    }
}