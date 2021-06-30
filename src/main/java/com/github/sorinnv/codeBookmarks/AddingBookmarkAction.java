package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddingBookmarkAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        int line = caretModel.getPrimaryCaret().getVisualPosition().line;
        VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
        Project project = e.getProject();
        if (project != null) {
            BookmarksManager.getInstance(project)
                    .addTextBookmark(virtualFile, line, "Some bookmark desription");
        }
        System.out.println(line);
        caretModel.getPrimaryCaret().moveToVisualPosition(new VisualPosition(2, 0));
        System.out.println(virtualFile.getPath());
    }
}
