package org.hexeditor.model;

import org.hexeditor.document.HexDocument;
import org.hexeditor.ui.HexTableModel;
import org.hexeditor.ui.OffsetTableModel;

import java.io.File;

public class EditorState {
    private final HexViewport hexViewport = new HexViewport();
    private final SelectionModel selectionModel = new SelectionModel();

    private File currentFile;
    private HexDocument currentDocument;
    private HexTableModel currentHexModel;
    private OffsetTableModel currentOffsetModel;
    private boolean selectionUpdating;

    public void clearDocumentState() {
        currentFile = null;
        currentDocument = null;
        currentHexModel = null;
        currentOffsetModel = null;
        selectionUpdating = false;
    }

    public HexViewport getHexViewport() {
        return hexViewport;
    }

    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    public HexDocument getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(HexDocument currentDocument) {
        this.currentDocument = currentDocument;
    }

    public HexTableModel getCurrentHexModel() {
        return currentHexModel;
    }

    public void setCurrentHexModel(HexTableModel currentHexModel) {
        this.currentHexModel = currentHexModel;
    }

    public OffsetTableModel getCurrentOffsetModel() {
        return currentOffsetModel;
    }

    public void setCurrentOffsetModel(OffsetTableModel currentOffsetModel) {
        this.currentOffsetModel = currentOffsetModel;
    }

    public boolean isSelectionUpdating() {
        return selectionUpdating;
    }

    public void setSelectionUpdating(boolean selectionUpdating) {
        this.selectionUpdating = selectionUpdating;
    }
}
