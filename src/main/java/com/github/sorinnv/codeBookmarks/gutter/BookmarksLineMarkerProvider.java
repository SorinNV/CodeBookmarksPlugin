package com.github.sorinnv.codeBookmarks.gutter;

import com.github.sorinnv.codeBookmarks.BookmarkIcons;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BookmarksLineMarkerProvider extends RelatedItemLineMarkerProvider {
    @Override
    public String getName() {
        return "BookmarkGutter";
    }

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        final ArrayList<GotoRelatedItem> targets = new ArrayList<>();
        collectTargets(element, targets, GotoRelatedItem::new);

        if (targets.isEmpty()) return;

        RelatedItemLineMarkerInfo<PsiElement> info =
                new RelatedItemLineMarkerInfo<>(element,
                        element.getTextRange(),
                        BookmarkIcons.GutterIcon,
                        null,
                        null,
                        GutterIconRenderer.Alignment.LEFT,
                        () -> targets);
        result.add(info);
    }
    private static <T> void collectTargets(PsiElement element,
                                           List<T> targets,
                                           final Function<PsiElement, T> fun) {
                    targets.add(fun.fun(element));
                }
}
