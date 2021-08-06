package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface BookmarkIcons {
    Icon GutterIcon = IconLoader.getIcon("/META-INF/PluginIcons/gutterIcon.svg", BookmarkIcons.class);
    Icon ToolWindowIcon = IconLoader.getIcon("/META-INF/PluginIcons/toolWindowIcon.svg", BookmarkIcons.class);
}
