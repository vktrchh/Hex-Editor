package org.hexeditor.io;

import java.io.IOException;

public interface ByteSource {
    long length() throws IOException;
    byte readByte(long offset) throws IOException;
}
