package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Service
public final class BookmarkManager {
    private static final int MAX_AUTO_DESCRIPTION_SIZE = 50;
    private final MultiMap<VirtualFile, Bookmark> myBookmarks = MultiMap.createConcurrentSet();

    private final Project myProject;

    public static BookmarkManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, BookmarkManager.class);
    }

    public BookmarkManager(Project project) {
        this.myProject = project;
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(EditorColorsManager.TOPIC, __ -> colorsChanged());
    }

    @NotNull
    public Bookmark addTextBookmark(@NotNull VirtualFile file, int lineIndex, @NotNull String description) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        Bookmark b = new Bookmark(myProject, file, lineIndex, description);
        myBookmarks.putValue(file, b);
        getPublisher().bookmarkAdded(b);
        return b;
    }

    public void addEditorBookmark(@NotNull Editor editor, int lineIndex) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        if (psiFile == null) return;

        final VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) return;

        addTextBookmark(virtualFile, lineIndex, getAutoDescription(editor, lineIndex));
    }

    @NotNull
    private static String getAutoDescription(@NotNull final Editor editor, final int lineIndex) {
        String autoDescription = editor.getSelectionModel().getSelectedText();
        if (autoDescription == null) {
            Document document = editor.getDocument();
            autoDescription = document.getCharsSequence()
                    .subSequence(document.getLineStartOffset(lineIndex), document.getLineEndOffset(lineIndex)).toString().trim();
        }
        if (autoDescription.length() > MAX_AUTO_DESCRIPTION_SIZE) {
            return autoDescription.substring(0, MAX_AUTO_DESCRIPTION_SIZE) + "...";
        }
        return autoDescription;
    }

    public void removeBookmark(@NotNull Bookmark bookmark) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        VirtualFile file = bookmark.getFile();
        if (myBookmarks.remove(file, bookmark)) {
            bookmark.release();
            getPublisher().bookmarkRemoved(bookmark);
        }
    }

    @NotNull
    public List<Bookmark> getValidBookmarks() {
        return ContainerUtil.filter(myBookmarks.values(), Bookmark::isValid);
    }

    @Nullable
    public Bookmark findEditorBookmark(@NotNull Document document, int line) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) return null;
        return ContainerUtil.find(myBookmarks.get(file), bookmark -> bookmark.getLine() == line);
    }

    @Nullable
    public Bookmark findFileBookmark(@NotNull VirtualFile file) {
        return ContainerUtil.find(myBookmarks.get(file), bookmark -> bookmark.getLine() == -1);
    }

    @NotNull
    private BookmarkListener getPublisher() {
        return myProject.getMessageBus().syncPublisher(BookmarkListener.TOPIC);
    }

    private void colorsChanged() {
        for (Bookmark bookmark : myBookmarks.values()) {
            bookmark.updateHighlighter();
        }
    }
}
