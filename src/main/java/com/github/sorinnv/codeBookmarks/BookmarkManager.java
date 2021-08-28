package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentListener;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@State(name = "Bookmark_manager", storages = {
        @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)
})
public final class BookmarkManager implements PersistentStateComponent<Element> {
    private static final int MAX_AUTO_DESCRIPTION_SIZE = 50;
    private final MultiMap<VirtualFile, Bookmark> myBookmarks = MultiMap.createConcurrentSet();

    private final Project myProject;

    private final AtomicReference<List<Bookmark>> myPendingState = new AtomicReference<>();

    public static BookmarkManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, BookmarkManager.class);
    }

    public BookmarkManager(Project project) {
        this.myProject = project;
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(EditorColorsManager.TOPIC, __ -> colorsChanged());
    }

    static final class MyStartupActivity implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            project.getMessageBus().connect().subscribe(PsiDocumentListener.TOPIC,
                    BookmarkManager.MyStartupActivity::documentCreated);

            BookmarkManager bookmarkManager = getInstance(project);
            if (bookmarkManager.myBookmarks.isEmpty() && bookmarkManager.myPendingState.get() == null) {
                return;
            }

            ReadAction.nonBlocking(() -> {
                FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

                for (VirtualFile file : fileEditorManager.getOpenFiles()) {
                    Document document = fileDocumentManager.getDocument(file);
                    if (document != null) {
                        checkFile(document, file, bookmarkManager, project);
                    }
                }
            })
                    .expireWith(project)
                    .submit(NonUrgentExecutor.getInstance());
        }

        private static void documentCreated(@NotNull Document document, @Nullable PsiFile psiFile, @NotNull Project project) {
            BookmarkManager bookmarkManager = getInstance(project);
            if (bookmarkManager.myBookmarks.isEmpty()) {
                return;
            }

            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file == null) {
                return;
            }

            checkFile(document, file, bookmarkManager, project);
        }

        private static void checkFile(@NotNull Document document, @NotNull VirtualFile file, @NotNull BookmarkManager bookmarkManager, @NotNull Project project) {
            Collection<Bookmark> fileBookmarks = bookmarkManager.myBookmarks.get(file);
            if (fileBookmarks.isEmpty()) {
                return;
            }

            AppUIUtil.invokeLaterIfProjectAlive(project, () -> {
                MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);
                for (Bookmark bookmark : fileBookmarks) {
                    bookmark.createHighlighter(markup);
                }
            });
        }
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

    @Override
    public @Nullable Element getState() {
        Element container = new Element("Bookmark_manager");
        writeExternal(container);
        return container;
    }

    @Override
    public void loadState(@NotNull Element state) {
        myPendingState.set(readExternal(state));

        StartupManager.getInstance(myProject).runAfterOpened(() -> ApplicationManager.getApplication().invokeLater(() -> {
            List<Bookmark> newList = myPendingState.getAndSet(null);
            if (newList != null) {
                applyNewState(newList, true);
            }
        }, ModalityState.NON_MODAL, myProject.getDisposed()));
    }

    private void applyNewState(@NotNull List<Bookmark> newList, boolean fireEvents) {
        if (!myBookmarks.isEmpty()) {
            Bookmark[] bookmarks = myBookmarks.values().toArray(new Bookmark[0]);
            for (Bookmark bookmark : bookmarks) {
                bookmark.release();
            }
            myBookmarks.clear();
        }

        List<Bookmark> addedBookmarks = new ArrayList<>(newList.size());
        for (Bookmark bookmark : newList) {
            OpenFileDescriptor target = bookmark.init(myProject);
            if (target == null) {
                continue;
            }

            if (target.getLine() == -1 && findFileBookmark(target.getFile()) != null) {
                continue;
            }

            myBookmarks.putValue(target.getFile(), bookmark);
            addedBookmarks.add(bookmark);
        }

        if (fireEvents) {
            for (Bookmark bookmark : addedBookmarks) {
                getPublisher().bookmarkAdded(bookmark);
            }
        }
    }

    @NotNull
    private static List<Bookmark> readExternal(@NotNull Element element) {
        List<Bookmark> result = new ArrayList<>();
        for (Element bookmarkElement : element.getChildren("bookmark")) {
            String url = bookmarkElement.getAttributeValue("url");
            if (StringUtil.isEmptyOrSpaces(url)) {
                continue;
            }

            String line = bookmarkElement.getAttributeValue("line");
            String description = StringUtil.notNullize(bookmarkElement.getAttributeValue("description"));

            int lineIndex = -1;
            if (line != null) {
                try {
                    lineIndex = Integer.parseInt(line);
                }
                catch (NumberFormatException ignore) {
                    // Ignore. Will miss bookmark if line number cannot be parsed
                    continue;
                }
            }
            Bookmark bookmark = new Bookmark(url, lineIndex, description);
            result.add(bookmark);
        }
        return result;
    }

    private void writeExternal(Element element) {
        List<Bookmark> bookmarks = new ArrayList<>(myBookmarks.values());
        // store in reverse order so that loadExternal() will assign them correct indices
        //bookmarks.sort(Comparator.<Bookmark>comparingInt(o -> o.index).reversed());

        for (Bookmark bookmark : bookmarks) {
            if (!bookmark.isValid()) continue;
            Element bookmarkElement = new Element("bookmark");

            bookmarkElement.setAttribute("url", bookmark.getFile().getUrl());

            String description = bookmark.nullizeEmptyDescription();
            if (description != null) {
                bookmarkElement.setAttribute("description", description);
            }

            int line = bookmark.getLine();
            if (line >= 0) {
                bookmarkElement.setAttribute("line", String.valueOf(line));
            }

            element.addContent(bookmarkElement);
        }
    }
}
