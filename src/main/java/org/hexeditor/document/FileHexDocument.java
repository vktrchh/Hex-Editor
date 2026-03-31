package org.hexeditor.document;

import org.hexeditor.io.ByteSource;
import org.hexeditor.io.FileByteSource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileHexDocument implements HexDocument {
    private final ByteSource byteSource;
    private final Map<Long, Byte> modifiedBytes = new HashMap<>();

    public FileHexDocument(File file) throws IOException {
        this.byteSource = new FileByteSource(file);
    }

    @Override
    public long length() throws IOException {
        return byteSource.length();
    }

    @Override
    public byte readByte(long offset) throws IOException {
        Byte modifiedValue = modifiedBytes.get(offset);
        if(modifiedValue != null){
            return modifiedValue;
        }
        return byteSource.readByte(offset);
    }

    @Override
    public void writeByte(long offset, byte value) throws IOException {
        if(offset< 0 || offset >= length()){
            throw new IllegalArgumentException();
        }
        modifiedBytes.put(offset, value);

    }

    @Override
    public boolean isModified() {
        return !modifiedBytes.isEmpty();
    }

    @Override
    public void close() throws IOException {
        byteSource.close();
    }
}