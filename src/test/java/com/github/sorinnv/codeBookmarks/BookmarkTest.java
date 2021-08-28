package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BookmarkTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    protected void initFixture() {
        myFixture.configureByFile("BookmarkTestData.java");
    }

    public void testToggleBookmark() {
        initFixture();
        myFixture.performEditorAction(ToggleBookmarkAction.class.getName());
        assertSize(1, myFixture.findAllGutters());
        assertEquals(myFixture.findAllGutters().get(0).getIcon(), BookmarkIcons.GutterIcon);

        RangeHighlighter[] highlighters = DocumentMarkupModel.forDocument(myFixture.getEditor().getDocument(),
                myFixture.getProject(), true).getAllHighlighters();
        assertSize(1, highlighters);
        assertEquals(highlighters[0].getStartOffset(), myFixture.getCaretOffset());

        myFixture.performEditorAction(ToggleBookmarkAction.class.getName());
        assertSize(0, myFixture.findAllGutters());
    }
}