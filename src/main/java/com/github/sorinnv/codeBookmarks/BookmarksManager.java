package com.github.sorinnv.codeBookmarks;

import com.intellij.ide.favoritesTreeView.FavoritesListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Service
public final class BookmarksManager {
    private final MultiMap<VirtualFile, Bookmark> myBookmarks = MultiMap.createConcurrentSet();

    private final Project myProject;
    private final List<FavoritesListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    public static BookmarksManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, BookmarksManager.class);
    }

    public BookmarksManager(Project myProject) {
        this.myProject = myProject;
    }

    @NotNull
    public Bookmark addTextBookmark(@NotNull VirtualFile file, int lineIndex, @NotNull String description) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        Bookmark b = new Bookmark(myProject, file, lineIndex, description);
        myBookmarks.putValue(file, b);
        listAdded(description);
        return b;
    }

    public void removeBookmark(@NotNull Bookmark bookmark) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        VirtualFile file = bookmark.getFile();
        myBookmarks.remove(file, bookmark);
        listRemoved(bookmark.getDescription());
    }

    @NotNull
    public List<Bookmark> getBookmarks() {
        return ContainerUtil.filter(myBookmarks.values(), b -> true);
    }

    private void rootsChanged() {
        for (FavoritesListener listener : myListeners) {
            listener.rootsChanged();
        }
    }

    private void listAdded(@NotNull String listName) {
        for (FavoritesListener listener : myListeners) {
            listener.listAdded(listName);
        }
    }

    private void listRemoved(@NotNull String listName) {
        for (FavoritesListener listener : myListeners) {
            listener.listRemoved(listName);
        }
    }

    public void addBookmarksListener(final FavoritesListener listener, @NotNull Disposable parent) {
        myListeners.add(listener);
        listener.rootsChanged();
        Disposer.register(parent, () -> myListeners.remove(listener));
    }
}
