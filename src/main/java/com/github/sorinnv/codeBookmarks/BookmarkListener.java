package com.github.sorinnv.codeBookmarks;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface BookmarkListener {
    Topic<BookmarkListener> TOPIC = Topic.create("Bookmarks", BookmarkListener.class);

    default void bookmarkAdded(@NotNull Bookmark b) { }

    default void bookmarkRemoved(@NotNull Bookmark b) { }

    default void bookmarkChanged(@NotNull Bookmark b) { }

    default void bookmarksOrderChanged() { }
}
