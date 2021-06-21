package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import org.jetbrains.annotations.NotNull;

public class AddingBookmarkAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        System.out.println(caretModel.getPrimaryCaret().getVisualPosition().line);
        caretModel.getPrimaryCaret().moveToVisualPosition(new VisualPosition(2, 0));
        System.out.println(e.getRequiredData(CommonDataKeys.VIRTUAL_FILE).getPath());
    }
}
