package com.mvel.linter.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.mvel.linter.MvelLanguage;
import org.jetbrains.annotations.NotNull;

public abstract class MvelElement extends ASTWrapperPsiElement {
    public MvelElement(@NotNull ASTNode node) {
        super(node);
    }
}

