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
/*public class BookmarkTest {
    private CodeInsightTestFixture myFixture;

    @BeforeEach
    public void initFixture() {
        IdeaTestFixtureFactory fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
        final TestFixtureBuilder<IdeaProjectTestFixture> bookmarkTestBuilder =
                fixtureFactory.createFixtureBuilder("BookmarkTestBuilder");
        IdeaProjectTestFixture fixture = bookmarkTestBuilder.getFixture();
        TempDirTestFixture tempDirTestFixture = fixtureFactory.createTempDirTestFixture();
        tempDirTestFixture.getTempDirPath();
        myFixture = fixtureFactory.createCodeInsightFixture(fixture, tempDirTestFixture);
        try {
            myFixture.setUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
        myFixture.setTestDataPath("src/test/testData");
        myFixture.configureByFile("BookmarkTestData.java");
        //myFixture.copyFileToProject("BookmarkTestData.java");
    }
    @Test
    public void testCreatingBookmark() {
        //initFixture();
        VirtualFile file = myFixture.getFile().getVirtualFile();
        int line = myFixture.getEditor().getCaretModel().getLogicalPosition().line;
        String description = "description";
        assert(file != null);
        final Bookmark bookmark = new Bookmark(myFixture.getProject(),
                    file,
                    line,
                    description);

        assert(bookmark.getLine() == line);
        assert(bookmark.isValid());
        assert(bookmark.getFile().equals(file));
        assert(bookmark.getDescription().equals(description));
        assert(bookmark.getUrl().equals(file.getUrl()));
        assert(bookmark.getIcon() == BookmarkIcons.GutterIcon);
    }
}*/
