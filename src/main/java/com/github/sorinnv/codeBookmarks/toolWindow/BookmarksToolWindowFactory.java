package com.github.sorinnv.codeBookmarks.toolWindow;

import com.github.sorinnv.codeBookmarks.BookmarkIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BookmarksToolWindowFactory implements ToolWindowFactory {
    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Objects.requireNonNull(ToolWindowManager.getInstance(project).getToolWindow("Code Bookmarks")).setIcon(BookmarkIcons.ToolWindowIcon);
        BookmarksToolWindow bookmarksToolWindow = new BookmarksToolWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(bookmarksToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
