package com.mvel.linter.psi.impl;

import com.intellij.lang.ASTNode;
import com.mvel.linter.psi.MvelElement;
import org.jetbrains.annotations.NotNull;

public class MvelElementImpl extends MvelElement {
    public MvelElementImpl(@NotNull ASTNode node) {
        super(node);
    }
}

