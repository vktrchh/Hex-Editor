package org.hexeditor.editing;

import java.util.Arrays;

public class HexClipboard {
    private byte[] data = new byte[0];

    public void setBytes(byte[] bytes) {
        if (bytes == null) {
            data = new byte[0];
            return;
        }
        data = Arrays.copyOf(bytes, bytes.length);
    }

    public byte[] getBytes() {
        return Arrays.copyOf(data, data.length);
    }

    public boolean hasData() {
        return data.length > 0;
    }

    public int size() {
        return data.length;
    }

    public void clear() {
        data = new byte[0];
    }
}
