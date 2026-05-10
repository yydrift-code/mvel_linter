package com.mvel.linter.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.mvel.linter.codeblock.MvelCodeBlockSupport;
import org.jetbrains.annotations.Nullable;

public class MvelGotoDeclarationHandler implements GotoDeclarationHandler {
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(PsiElement sourceElement, int offset, Editor editor) {
        if (MvelCodeBlockSupport.isInsideJavaCodeBlockContent(sourceElement)) {
            return null;
        }
        return MvelNavigationResolver.resolveTargets(sourceElement);
    }
}
