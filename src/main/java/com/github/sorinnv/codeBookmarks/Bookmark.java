package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

public class Bookmark {
    private Project project;
    private final String url;
    private final int line;
    @NotNull
    private String description;

    public Bookmark(@NotNull String url, int line, @NotNull String description) {
        this.url = url;
        this.line = line;
        this.description = description;
    }

    Bookmark(@NotNull Project project, @NotNull VirtualFile file, int line, @NotNull String description) {
        this.description = description;
        this.line = line;
        this.url = file.getUrl();
        this.project = project;
    }

    public VirtualFile getFile() {
        return VirtualFileManager.getInstance().findFileByUrl(url);
    }
    public String getUrl() {
        return url;
    }

    public int getLine() {
        return line;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }
}
