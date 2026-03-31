package org.hexeditor.document;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public interface HexDocument extends Closeable {
    long length() throws IOException;
    byte readByte(long offset) throws IOException;

    void writeByte(long offset, byte value) throws IOException;
    boolean isModified();

    @Override
    default void close() throws IOException {
    }

    void saveTo(File file) throws IOException;
}