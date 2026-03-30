package org.hexeditor.io;

import java.io.Closeable;
import java.io.IOException;

public interface ByteSource extends Closeable {
    long length() throws IOException;
    byte readByte(long offset) throws IOException;

    @Override
    default void close() throws IOException{}
}
