package org.hexeditor.ui;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class MainMenuBar extends JMenuBar {
    private final JMenuItem openItem = new JMenuItem("Открыть");
    private final JMenuItem saveItem = new JMenuItem("Сохранить");
    private final JMenuItem saveAsItem = new JMenuItem("Сохранить как");
    private final JMenuItem exitItem = new JMenuItem("Выход");

    private final JMenuItem copyItem = new JMenuItem("Копировать");
    private final JMenuItem cutItem = new JMenuItem("Вырезать");
    private final JMenuItem pasteItem = new JMenuItem("Вставить");
    private final JMenuItem pasteOverwriteItem = new JMenuItem("Заменить");
    private final JMenuItem insertHexItem = new JMenuItem("Вставить hex");
    private final JMenuItem deleteItem = new JMenuItem("Удалить");
    private final JMenuItem makeZeroItem = new JMenuItem("Обнулить");

    private final JMenuItem searchItem = new JMenuItem("Поиск");

    private final JMenuItem startItem = new JMenuItem("|<");
    private final JMenuItem lineUpItem = new JMenuItem("<");
    private final JMenuItem pageUpItem = new JMenuItem("<<");
    private final JMenuItem pageDownItem = new JMenuItem(">>");
    private final JMenuItem lineDownItem = new JMenuItem(">");
    private final JMenuItem endItem = new JMenuItem(">|");

    public MainMenuBar() {
        JMenu fileMenu = new JMenu("Файл");
        JMenu editMenu = new JMenu("Редактирование");
        JMenu searchMenu = new JMenu("Поиск");
        JMenu navigationMenu = new JMenu("Навигация");

        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
        ));

        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        pasteOverwriteItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_V,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
        ));
        searchItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        editMenu.add(copyItem);
        editMenu.add(cutItem);
        editMenu.addSeparator();
        editMenu.add(pasteItem);
        editMenu.add(pasteOverwriteItem);
        editMenu.add(insertHexItem);
        editMenu.addSeparator();
        editMenu.add(deleteItem);
        editMenu.add(makeZeroItem);

        searchMenu.add(searchItem);

        navigationMenu.add(startItem);
        navigationMenu.add(lineUpItem);
        navigationMenu.add(pageUpItem);
        navigationMenu.add(pageDownItem);
        navigationMenu.add(lineDownItem);
        navigationMenu.add(endItem);

        add(fileMenu);
        add(editMenu);
        add(searchMenu);
        add(navigationMenu);
    }

    public JMenuItem getOpenItem() {
        return openItem;
    }

    public JMenuItem getSaveItem() {
        return saveItem;
    }

    public JMenuItem getSaveAsItem() {
        return saveAsItem;
    }

    public JMenuItem getExitItem() {
        return exitItem;
    }

    public JMenuItem getCopyItem() {
        return copyItem;
    }

    public JMenuItem getCutItem() {
        return cutItem;
    }

    public JMenuItem getPasteItem() {
        return pasteItem;
    }

    public JMenuItem getPasteOverwriteItem() {
        return pasteOverwriteItem;
    }

    public JMenuItem getInsertHexItem() {
        return insertHexItem;
    }

    public JMenuItem getDeleteItem() {
        return deleteItem;
    }

    public JMenuItem getMakeZeroItem() {
        return makeZeroItem;
    }

    public JMenuItem getSearchItem() {
        return searchItem;
    }

    public JMenuItem getStartItem() {
        return startItem;
    }

    public JMenuItem getLineUpItem() {
        return lineUpItem;
    }

    public JMenuItem getPageUpItem() {
        return pageUpItem;
    }

    public JMenuItem getPageDownItem() {
        return pageDownItem;
    }

    public JMenuItem getLineDownItem() {
        return lineDownItem;
    }

    public JMenuItem getEndItem() {
        return endItem;
    }
}
