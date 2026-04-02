package org.hexeditor.ui;

import javax.swing.*;
import java.awt.*;

/*
    Нижняя информационная панель, показывает информацию о выделенном байте(блоке)
    Показывает смещение, hex-значение, знаковое и беззнаковое представление.
    Показывает 2, 4, 8, блоки байтов от текущего выбранного байта.(little-endian)
 */
public class ByteInfoPanel extends JPanel {
    private final JLabel viewOffsetLabel = new JLabel("offset: 00000000");
    private final JLabel selectedOffsetLabel = new JLabel("Selected: --");
    private final JLabel selectedHexLabel = new JLabel("Hex: --");
    private final JLabel selectedUnsignedLabel = new JLabel("Unsigned: --");
    private final JLabel selectedSignedLabel = new JLabel("Signed: --");

    private final JLabel int16Label = new JLabel("Int16: --");
    private final JLabel uint16Label = new JLabel("UInt16: --");

    private final JLabel int32Label = new JLabel("Int32: --");
    private final JLabel uint32Label = new JLabel("UInt32: --");
    private final JLabel floatLabel = new JLabel("Float: --");

    private final JLabel int64Label = new JLabel("Int64: --");
    private final JLabel uint64Label = new JLabel("UInt64: --");
    private final JLabel doubleLabel = new JLabel("Double: --");

    private final JLabel blockLengthLabel = new JLabel("Block: 1 byte");

    public ByteInfoPanel() {
        setLayout(new GridLayout(2, 1));

        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstRow.add(viewOffsetLabel);
        firstRow.add(Box.createHorizontalStrut(20));
        firstRow.add(selectedOffsetLabel);
        firstRow.add(Box.createHorizontalStrut(20));
        firstRow.add(selectedHexLabel);
        firstRow.add(Box.createHorizontalStrut(20));
        firstRow.add(selectedUnsignedLabel);
        firstRow.add(Box.createHorizontalStrut(20));
        firstRow.add(selectedSignedLabel);
        firstRow.add(Box.createHorizontalStrut(20));
        firstRow.add(blockLengthLabel);

        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        secondRow.add(int16Label);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(uint16Label);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(int32Label);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(uint32Label);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(floatLabel);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(int64Label);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(uint64Label);
        secondRow.add(Box.createHorizontalStrut(15));
        secondRow.add(doubleLabel);

        add(firstRow);
        add(secondRow);

        clearSelectionInfo();
    }

    public void setViewOffset(long offset) {
        viewOffsetLabel.setText(String.format("offset: %08X", offset));
    }

    public void clearSelectionInfo() {
        selectedOffsetLabel.setText("Selected: --");
        selectedHexLabel.setText("Hex: --");
        selectedUnsignedLabel.setText("Unsigned: --");
        selectedSignedLabel.setText("Signed: --");

        clearInt16Info();
        clearInt32Info();
        clearInt64Info();

        blockLengthLabel.setText("Block: --");
    }

    public void showSingleByte(long offset, int unsignedValue, int signedValue) {
        selectedOffsetLabel.setText(String.format("Selected: %08X", offset));
        selectedHexLabel.setText(String.format("Hex: %02X", unsignedValue));
        selectedUnsignedLabel.setText("Unsigned: " + unsignedValue);
        selectedSignedLabel.setText("Signed: " + signedValue);
    }

    public void showInt16(short int16Value, int uint16Value) {
        int16Label.setText("Int16: " + int16Value);
        uint16Label.setText("UInt16: " + uint16Value);
    }

    public void clearInt16Info() {
        int16Label.setText("Int16: --");
        uint16Label.setText("UInt16: --");
    }

    public void showInt32(int int32Value, long uint32Value, float floatValue) {
        int32Label.setText("Int32: " + int32Value);
        uint32Label.setText("UInt32: " + uint32Value);
        floatLabel.setText("Float: " + floatValue);
    }

    public void clearInt32Info() {
        int32Label.setText("Int32: --");
        uint32Label.setText("UInt32: --");
        floatLabel.setText("Float: --");
    }

    public void showInt64(long int64Value, String uint64Value, double doubleValue) {
        int64Label.setText("Int64: " + int64Value);
        uint64Label.setText("UInt64: " + uint64Value);
        doubleLabel.setText("Double: " + doubleValue);
    }

    public void clearInt64Info() {
        int64Label.setText("Int64: --");
        uint64Label.setText("UInt64: --");
        doubleLabel.setText("Double: --");
    }

    public void showBlockLength(long length) {
        if (length <= 1) {
            blockLengthLabel.setText("Block: 1 byte");
        } else {
            blockLengthLabel.setText("Block: " + length + " bytes");
        }
    }
}