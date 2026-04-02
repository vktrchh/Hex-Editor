package org.hexeditor.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/*
    класс для чтения файлов через RandomAccessFile
 */

public class FileByteSource implements Closeable {
    private final RandomAccessFile raf;

    public FileByteSource(File file) throws IOException{
        this.raf = new RandomAccessFile(file, "r");
    }

    public long length() throws IOException {
        return raf.length();
    }

    public byte readByte(long offset) throws IOException {
        raf.seek(offset);
        return raf.readByte();
    }

    @Override
    public void close() throws IOException{
        raf.close();
    }
}
