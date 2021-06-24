package com.github.sorinnv.codeBookmarks.toolWindow;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;


import javax.swing.*;
import java.awt.*;

public class BookmarksToolWindow {
    private final JPanel content = new JPanel();
    public BookmarksToolWindow(ToolWindow toolWindow) {
        String[] data = {"Bookmark1 ffffffffffffffffffffffffffffffffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "Bookmark2 ffffffffffffffffffffffffffffffffffffffffffff",
                "Bookmark3 ffffffffffffffffffffffffffffffffffffffffffff"};
        JBList<String> list = new JBList<>(data);

        content.setLayout(new FlowLayout(FlowLayout.LEFT));
        content.add("Bookmarks", new JBScrollPane(list));
    }

    public JPanel getContent() {
        return content;
    }
}
