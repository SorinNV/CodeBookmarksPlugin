package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;

import javax.swing.*;

public interface BookmarkIcons {
    Icon GutterIcon = IconLoader.getIcon("/META-INF/PluginIcons/gutterIcon.svg", BookmarkIcons.class);
    Icon ToolWindowIcon = IconManager.getInstance().getIcon("/META-INF/PluginIcons/toolWindowIcon.svg", BookmarkIcons.class);
}
