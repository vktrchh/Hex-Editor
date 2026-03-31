package org.hexeditor.document;

import java.io.Closeable;
import java.io.IOException;

public interface HexDocument extends Closeable {
    long length() throws IOException;
    byte readByte(long offset) throws IOException;

    @Override
    default void close() throws IOException {
    }
}