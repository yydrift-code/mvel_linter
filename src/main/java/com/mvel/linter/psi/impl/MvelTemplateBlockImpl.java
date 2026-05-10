package com.mvel.linter.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.codeblock.MvelJavaCodeBlockEscaper;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

public class MvelTemplateBlockImpl extends MvelExpressionImpl implements PsiLanguageInjectionHost {
    public MvelTemplateBlockImpl(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isCodeBlock() {
        ASTNode firstChild = getNode().getFirstChildNode();
        return firstChild != null && firstChild.getElementType() == MvelTokenTypes.TEMPLATE_CODE;
    }

    public @NotNull TextRange getContentRangeInElement() {
        ASTNode leftBrace = null;
        ASTNode rightBrace = null;
        for (ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (type == MvelTokenTypes.LBRACE && leftBrace == null) {
                leftBrace = child;
            } else if (type == MvelTokenTypes.RBRACE) {
                rightBrace = child;
            }
        }

        if (leftBrace == null || rightBrace == null) {
            return TextRange.EMPTY_RANGE;
        }

        int start = leftBrace.getStartOffsetInParent() + leftBrace.getTextLength();
        int end = rightBrace.getStartOffsetInParent();
        if (end < start) {
            return TextRange.EMPTY_RANGE;
        }
        return new TextRange(start, end);
    }

    @Override
    public boolean isValidHost() {
        return isCodeBlock() && !getContentRangeInElement().isEmpty();
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        return this;
    }

    @Override
    public @NotNull LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new MvelJavaCodeBlockEscaper(this);
    }
}
