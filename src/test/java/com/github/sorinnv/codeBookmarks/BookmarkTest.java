package com.github.sorinnv.codeBookmarks;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class BookmarkTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    protected void initFixture() {
        myFixture.configureByFile("BookmarkTestData.java");
    }

    public void testCreatingBookmark() {
        initFixture();
        VirtualFile file = myFixture.getFile().getVirtualFile();
        int line = myFixture.getEditor().getCaretModel().getLogicalPosition().line;
        String description = "description";
        assertNotNull(file);
        Bookmark bookmark = new Bookmark(myFixture.getProject(),
                file,
                line,
                description);
        assertEquals(line, bookmark.getLine());
        assertEquals(bookmark.getFile(), file);
        assertEquals(description, bookmark.getDescription());
        assertTrue(bookmark.isValid());
        assertEquals(file.getUrl(), bookmark.getUrl());
        assertEquals(BookmarkIcons.GutterIcon, bookmark.getIcon());
    }
}
