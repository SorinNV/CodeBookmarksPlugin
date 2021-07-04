package com.github.sorinnv.codeBookmarks;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.LangBundle;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterDraggableObject;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class Bookmark implements Navigatable {
    static final Icon DEFAULT_ICON = BookmarkIcons.GutterIcon;

    private OpenFileDescriptor myTarget;
    private Reference<RangeHighlighterEx> myHighlighterRef;

    private final String myUrl;
    @NotNull
    private String myDescription;

    Bookmark(@NotNull Project project, @NotNull VirtualFile file, int line, @NotNull String description) {
        myDescription = description;
        myUrl = file.getUrl();
        initTarget(project, file, line);
    }

    private void initTarget(@NotNull Project project, @NotNull VirtualFile file, int line) {
        myTarget = new OpenFileDescriptor(project, file, line, -1, true);
        addHighlighter();
    }

    void updateHighlighter() {
        release();
        addHighlighter();
    }

    private void addHighlighter() {
        Document document = getCachedDocument();
        if (document != null) {
            createHighlighter((MarkupModelEx) DocumentMarkupModel.forDocument(document, myTarget.getProject(), true));
        }
    }

    public RangeHighlighter createHighlighter(@NotNull MarkupModelEx markup) {
        final RangeHighlighterEx highlighter;
        int line = getLine();
        if (line >= 0) {
            highlighter = markup.addPersistentLineHighlighter(CodeInsightColors.BOOKMARKS_ATTRIBUTES, line, HighlighterLayer.ERROR + 1);
            if (highlighter != null) {
                highlighter.setGutterIconRenderer(new MyGutterIconRenderer(this));
                highlighter.setErrorStripeTooltip(getBookmarkTooltip());
            }
        }
        else {
            highlighter = null;
        }
        myHighlighterRef = highlighter == null ? null : new WeakReference<>(highlighter);
        return highlighter;
    }

    Document getCachedDocument() {
        return FileDocumentManager.getInstance().getCachedDocument(getFile());
    }

    public void release() {
        int line = getLine();
        if (line < 0) {
            return;
        }
        final Document document = getCachedDocument();
        if (document == null) return;
        MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myTarget.getProject(), true);
        final Document markupDocument = markup.getDocument();
        if (markupDocument.getLineCount() <= line) return;
        RangeHighlighterEx highlighter = findMyHighlighter();
        if (highlighter != null) {
            myHighlighterRef = null;
            highlighter.dispose();
        }
    }

    private RangeHighlighterEx findMyHighlighter() {
        final Document document = getCachedDocument();
        if (document == null) return null;
        RangeHighlighterEx result = SoftReference.dereference(myHighlighterRef);
        if (result != null) {
            return result;
        }
        MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myTarget.getProject(), true);
        final Document markupDocument = markup.getDocument();
        final int startOffset = 0;
        final int endOffset = markupDocument.getTextLength();

        final Ref<RangeHighlighterEx> found = new Ref<>();
        markup.processRangeHighlightersOverlappingWith(startOffset, endOffset, highlighter -> {
            GutterMark renderer = highlighter.getGutterIconRenderer();
            if (renderer instanceof MyGutterIconRenderer && ((MyGutterIconRenderer)renderer).myBookmark == this) {
                found.set(highlighter);
                return false;
            }
            return true;
        });
        result = found.get();
        myHighlighterRef = result == null ? null : new WeakReference<>(result);
        return result;
    }

    public Icon getIcon() {
        return DEFAULT_ICON;
    }

    public VirtualFile getFile() {
        return myTarget.getFile();
    }

    @Nullable
    String nullizeEmptyDescription() {
        return StringUtil.nullize(myDescription);
    }

    public String getUrl() {
        return myUrl;
    }

    @NotNull
    public String getDescription() {
        return myDescription;
    }

    public void setDescription(@NotNull String myDescription) {
        this.myDescription = myDescription;
    }

    public boolean isValid() {
        if (!getFile().isValid()) {
            return false;
        }
        if (getLine() == -1) {
            return true;
        }
        RangeHighlighterEx highlighter = findMyHighlighter();
        return highlighter != null && highlighter.isValid();
    }

    @NotNull
    private OpenFileDescriptor getTarget() {
        int line = getLine();
        if (line != myTarget.getLine()) {
            myTarget = new OpenFileDescriptor(myTarget.getProject(), myTarget.getFile(), line, -1, true);
        }
        return myTarget;
    }

    @Override
    public void navigate(boolean requestFocus) {
        getTarget().navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return getTarget().canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return getTarget().canNavigateToSource();
    }

    public int getLine() {
        int targetLine = myTarget.getLine();
        if (targetLine == -1) return -1;
        //What user sees in gutter
        RangeHighlighterEx highlighter = findMyHighlighter();
        if (highlighter != null && highlighter.isValid()) {
            Document document = highlighter.getDocument();
            return document.getLineNumber(highlighter.getStartOffset());
        }
        RangeMarker marker = myTarget.getRangeMarker();
        if (marker != null && marker.isValid()) {
            Document document = marker.getDocument();
            return document.getLineNumber(marker.getStartOffset());
        }
        return targetLine;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(myTarget == null ? myUrl : getQualifiedName());
        String text = nullizeEmptyDescription();
        String description = text == null ? null : StringUtil.escapeXmlEntities(text);
        if (description != null) {
            result.append(": ").append(description);
        }
        return result.toString();
    }

    @NotNull
    public String getQualifiedName() {
        String presentableUrl = myTarget.getFile().getPresentableUrl();
        if (myTarget.getFile().isDirectory()) {
            return presentableUrl;
        }

        final PsiFile psiFile = PsiManager.getInstance(myTarget.getProject()).findFile(myTarget.getFile());

        if (psiFile == null) return presentableUrl;

        StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(psiFile);
        if (builder instanceof TreeBasedStructureViewBuilder) {
            StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(null);
            Object element;
            try {
                element = model.getCurrentEditorElement();
            }
            finally {
                Disposer.dispose(model);
            }
            if (element instanceof NavigationItem) {
                ItemPresentation presentation = ((NavigationItem)element).getPresentation();
                if (presentation != null) {
                    presentableUrl = ((NavigationItem)element).getName() + " " + presentation.getLocationString();
                }
            }
        }

        return IdeBundle.message("bookmark.file.X.line.Y", presentableUrl, getLine() + 1);
    }

    @NotNull
    private String getBookmarkTooltip() {
        StringBuilder result = new StringBuilder("Bookmark");

        String text = nullizeEmptyDescription();
        String description = text == null ? null : StringUtil.escapeXmlEntities(text);
        if (description != null) {
            result.append(": ").append(description);
        }

        StringBuilder shortcutDescription = new StringBuilder();

        if (shortcutDescription.length() == 0) {
            String shortcutText = KeymapUtil.getFirstKeyboardShortcutText("com.github.sorinnv.codeBookmarks.ToggleBookmarkAction");
            if (shortcutText.length() > 0) {
                shortcutDescription.append(shortcutText).append(" to toggle");
            }
        }

        if (shortcutDescription.length() > 0) {
            result.append(" (").append(shortcutDescription).append(")");
        }

        return result.toString();
    }

    private static class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
        private final Bookmark myBookmark;

        MyGutterIconRenderer(@NotNull Bookmark bookmark) {
            myBookmark = bookmark;
        }

        @Override
        @NotNull
        public Icon getIcon() {
            return myBookmark.getIcon();
        }

        @Override
        @NotNull
        public String getTooltipText() {
            return myBookmark.getBookmarkTooltip();
        }

        @NotNull
        @Override
        public GutterDraggableObject getDraggableObject() {
            return (line, file, actionId) -> {
                myBookmark.myTarget = new OpenFileDescriptor(myBookmark.myTarget.getProject(), file, line, -1, true);
                myBookmark.updateHighlighter();
                return true;
            };
        }

        @NotNull
        @Override
        public String getAccessibleName() {
            return LangBundle.message("accessible.name.icon.bookmark.0", 0);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MyGutterIconRenderer &&
                    Objects.equals(getTooltipText(), ((MyGutterIconRenderer)obj).getTooltipText()) &&
                    Comparing.equal(getIcon(), ((MyGutterIconRenderer)obj).getIcon());
        }

        @Override
        public int hashCode() {
            return getIcon().hashCode();
        }

        @Nullable
        @Override
        public ActionGroup getPopupMenuActions() {
            return (ActionGroup) ActionManager.getInstance().getAction("popup@BookmarkContextMenu");
        }
    }
}