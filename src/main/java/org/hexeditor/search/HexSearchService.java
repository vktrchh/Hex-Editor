package org.hexeditor.search;

import org.hexeditor.document.HexDocument;

import java.io.IOException;

public class HexSearchService {

    public byte[] parseExactPattern(String input) {
        String normalized = input.trim().replaceAll("\\s+", " ");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Строка поиска пуста.");
        }

        String[] parts = normalized.split(" ");
        byte[] result = new byte[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (part.length() != 2) {
                throw new IllegalArgumentException(
                        "Каждый байт должен быть записан двумя hex-символами."
                );
            }

            try {
                int value = Integer.parseInt(part, 16);
                result[i] = (byte) value;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Некорректное hex-значение: " + part
                );
            }
        }

        return result;
    }

    public long findExactPattern(HexDocument document, byte[] pattern) throws IOException {
        if (document == null) {
            throw new IllegalStateException("Файл не открыт.");
        }

        if (pattern == null || pattern.length == 0) {
            return -1;
        }

        long fileLength = document.length();
        long maxStart = fileLength - pattern.length;

        for (long start = 0; start <= maxStart; start++) {
            boolean match = true;

            for (int i = 0; i < pattern.length; i++) {
                byte fileByte = document.readByte(start + i);
                if (fileByte != pattern[i]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return start;
            }
        }

        return -1;
    }

    public MaskPattern parseMaskPattern(String input) {
        String normalized = input.trim().replaceAll("\\s+", " ");

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Строка маски пуста.");
        }

        String[] parts = normalized.split(" ");

        byte[] values = new byte[parts.length];
        boolean[] wildcard = new boolean[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toUpperCase();

            if ("??".equals(part)) {
                wildcard[i] = true;
                values[i] = 0;
                continue;
            }

            if (part.length() != 2) {
                throw new IllegalArgumentException(
                        "Каждый байт маски должен быть двумя hex-символами или ??"
                );
            }

            try {
                int value = Integer.parseInt(part, 16);
                values[i] = (byte) value;
                wildcard[i] = false;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Некорректный элемент маски: " + part
                );
            }
        }

        return new MaskPattern(values, wildcard);
    }

    public long findMaskedPattern(HexDocument document, MaskPattern maskPattern) throws IOException {
        if (document == null) {
            throw new IllegalStateException("Файл не открыт.");
        }

        if (maskPattern == null || maskPattern.length() == 0) {
            return -1;
        }

        long fileLength = document.length();
        long maxStart = fileLength - maskPattern.length();

        for (long start = 0; start <= maxStart; start++) {
            boolean match = true;

            for (int i = 0; i < maskPattern.length(); i++) {
                if (maskPattern.getWildcard()[i]) {
                    continue;
                }

                byte fileByte = document.readByte(start + i);
                if (fileByte != maskPattern.getValues()[i]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return start;
            }
        }

        return -1;
    }
}