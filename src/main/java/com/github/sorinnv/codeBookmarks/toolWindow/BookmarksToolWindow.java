package com.github.sorinnv.codeBookmarks.toolWindow;

import com.github.sorinnv.codeBookmarks.Bookmark;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectViewTree;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.EditSourceOnEnterKeyHandler;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class BookmarksToolWindow {
    private final JPanel content = new JPanel();
    private final Project project;
    final DnDAwareTree tree;

    public BookmarksToolWindow(ToolWindow toolWindow, Project project) {
        this.project = project;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.setUserObject(new ProjectViewProjectNode(project, new ViewSettings() {
                }));

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new DnDAwareTree(treeModel) {
            @Override
            public boolean isFileColorsEnabled() {
                return ProjectViewTree.isFileColorsEnabledFor(this);
            }

            @Override
            public Color getFileColorFor(Object object) {
                return ProjectViewTree.getColorForElement(getPsiElement(object));
            }
        };

        tree.add(new JTextField("some text"));
        tree.add(new JTextField("some text1"));
        tree.add(new JTextField("some text2"));
        tree.repaint();

        TreeUtil.installActions(tree);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        new TreeSpeedSearch(tree);
        ToolTipManager.sharedInstance().registerComponent(tree);

        tree.setCellRenderer(new NodeRenderer() {
            @Override
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        });

        EditSourceOnDoubleClickHandler.install(tree);
        EditSourceOnEnterKeyHandler.install(tree);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(tree)
                .initPosition()
                .disableAddAction().disableRemoveAction().disableDownAction().disableUpAction();

        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_ELEMENT);
        action.registerCustomShortcutSet(action.getShortcutSet(), tree);
        JPanel panel = decorator.createPanel();
        panel.setBorder(JBUI.Borders.empty());
        content.add(new JBScrollPane(panel), BorderLayout.CENTER);
        content.setBorder(JBUI.Borders.empty());
    }

    public JPanel getContent() {
        return content;
    }

    @Nullable
    private PsiElement getPsiElement(@Nullable Object element) {
        if (element instanceof Bookmark) {
            element = ((Bookmark)element).getFile();
        }
        if (element instanceof PsiElement) {
            return (PsiElement)element;
        }
        else if (element instanceof SmartPsiElementPointer) {
            return ((SmartPsiElementPointer)element).getElement();
        }
        return null;
    }
}
