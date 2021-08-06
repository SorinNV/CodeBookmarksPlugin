package com.github.sorinnv.codeBookmarks;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.*;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class ToggleBookmarkAction extends AnAction implements DumbAware, Toggleable {
    public ToggleBookmarkAction() {
        getTemplatePresentation().setText(IdeBundle.messagePointer("action.bookmark.toggle"));
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        DataContext dataContext = event.getDataContext();
        event.getPresentation().setEnabled(project != null &&
                (CommonDataKeys.EDITOR.getData(dataContext) != null ||
                        CommonDataKeys.VIRTUAL_FILE.getData(dataContext) != null));

        if (ActionPlaces.TOUCHBAR_GENERAL.equals(event.getPlace())) {
            event.getPresentation().setIcon(AllIcons.Actions.Checked);
        }

        final BookmarkInContextInfo info = getBookmarkInfo(event);
        final boolean selected = info != null && info.getBookmarkAtPlace() != null;
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            event.getPresentation().setText(selected ? LangBundle.message("action.clear.bookmark.text") :
                    LangBundle.message("action.set.bookmark.text"));
        }
        else {
            event.getPresentation().setText(IdeBundle.messagePointer("action.bookmark.toggle"));
            Toggleable.setSelected(event.getPresentation(), selected);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        final BookmarkInContextInfo info = getBookmarkInfo(e);
        if (info == null) return;

        final boolean selected = info.getBookmarkAtPlace() != null;
        Toggleable.setSelected(e.getPresentation(), selected);

        if (selected) {
            BookmarkManager.getInstance(project).removeBookmark(info.getBookmarkAtPlace());
        }
        else {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                BookmarkManager.getInstance(project).addEditorBookmark(editor, info.getLine());
            }
            else {
                BookmarkManager.getInstance(project).addTextBookmark(info.getFile(), info.getLine(), "");
            }
        }
    }

    public static BookmarkInContextInfo getBookmarkInfo(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return null;

        final BookmarkInContextInfo info = new BookmarkInContextInfo(e.getDataContext(), project).invoke();
        return info.getFile() == null ? null : info;
    }

    protected static class BookmarkInContextInfo {
        private final DataContext myDataContext;
        private final Project myProject;
        private Bookmark myBookmarkAtPlace;
        private VirtualFile myFile;
        private int myLine;

        public BookmarkInContextInfo(DataContext dataContext, Project project) {
            myDataContext = dataContext;
            myProject = project;
        }

        public Bookmark getBookmarkAtPlace() {
            return myBookmarkAtPlace;
        }

        public VirtualFile getFile() {
            return myFile;
        }

        public int getLine() {
            return myLine;
        }

        public BookmarkInContextInfo invoke() {
            myBookmarkAtPlace = null;
            myFile = null;
            myLine = -1;

            BookmarkManager bookmarkManager = BookmarkManager.getInstance(myProject);
            Editor editor = CommonDataKeys.EDITOR.getData(myDataContext);
            if (editor == null) {
                editor = CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(myDataContext);
            }
            if (editor != null && !editor.isOneLineMode()) {
                Document document = editor.getDocument();
                myFile = FileDocumentManager.getInstance().getFile(document);
                if (myFile instanceof LightVirtualFile) {
                    myFile = null;
                    return this;
                }
                Integer gutterLineAtCursor = EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR.getData(myDataContext);
                myLine = gutterLineAtCursor != null ? gutterLineAtCursor : editor.getCaretModel().getLogicalPosition().line;
                myBookmarkAtPlace = bookmarkManager.findEditorBookmark(document, myLine);
            }

            if (myFile == null) {
                myFile = CommonDataKeys.VIRTUAL_FILE.getData(myDataContext);
                myLine = -1;

                if (myBookmarkAtPlace == null && myFile != null) {
                    myBookmarkAtPlace = bookmarkManager.findFileBookmark(myFile);
                }
            }
            return this;
        }
    }
}
