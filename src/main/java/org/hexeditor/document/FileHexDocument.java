package org.hexeditor.document;

import org.hexeditor.io.ByteSource;
import org.hexeditor.io.FileByteSource;

import java.io.File;
import java.io.IOException;

public class FileHexDocument implements HexDocument {
    private final ByteSource byteSource;

    public FileHexDocument(File file) throws IOException {
        this.byteSource = new FileByteSource(file);
    }

    @Override
    public long length() throws IOException {
        return byteSource.length();
    }

    @Override
    public byte readByte(long offset) throws IOException {
        return byteSource.readByte(offset);
    }

    @Override
    public void close() throws IOException {
        byteSource.close();
    }
}