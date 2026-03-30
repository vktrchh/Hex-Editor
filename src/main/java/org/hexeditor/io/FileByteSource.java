package org.hexeditor.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileByteSource implements ByteSource{
    private final RandomAccessFile raf;

    public FileByteSource(File file) throws IOException{
        this.raf = new RandomAccessFile(file, "r");
    }

    @Override
    public long length() throws IOException {
        return raf.length();
    }

    @Override
    public byte readByte(long offset) throws IOException {
        raf.seek(offset);
        return raf.readByte();
    }

    @Override
    public void close() throws IOException{
        raf.close();
    }
}
