package org.hexeditor.search;

public class MaskPattern {
    private final byte[] values;
    private final boolean[] wildcard;

    public MaskPattern(byte[] values, boolean[] wildcard) {
        this.values = values;
        this.wildcard = wildcard;
    }

    public int length() {
        return values.length;
    }

    public byte[] getValues() {
        return values;
    }

    public boolean[] getWildcard() {
        return wildcard;
    }
}
