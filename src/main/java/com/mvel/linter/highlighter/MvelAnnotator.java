package com.mvel.linter.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.mvel.linter.lexer.MvelTokenTypes;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

public class MvelAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only process MVEL files
        if (!(element.getContainingFile() instanceof MvelFile)) {
            return;
        }

        IElementType elementType = element.getNode().getElementType();
        
        // Apply syntax highlighting based on token type
        if (elementType == MvelTokenTypes.IF ||
            elementType == MvelTokenTypes.ELSE ||
            elementType == MvelTokenTypes.FOR ||
            elementType == MvelTokenTypes.FOREACH ||
            elementType == MvelTokenTypes.WHILE ||
            elementType == MvelTokenTypes.DO ||
            elementType == MvelTokenTypes.UNTIL ||
            elementType == MvelTokenTypes.RETURN ||
            elementType == MvelTokenTypes.NEW ||
            elementType == MvelTokenTypes.FUNCTION ||
            elementType == MvelTokenTypes.DEF ||
            elementType == MvelTokenTypes.ISDEF ||
            elementType == MvelTokenTypes.WITH ||
            elementType == MvelTokenTypes.ASSERT) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.KEYWORD)
                .create();
        } else if (elementType == MvelTokenTypes.STRING_LITERAL) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.STRING)
                .create();
        } else if (elementType == MvelTokenTypes.NUMBER_LITERAL) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.NUMBER)
                .create();
        } else if (elementType == MvelTokenTypes.IDENTIFIER) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.IDENTIFIER)
                .create();
        } else if (elementType == MvelTokenTypes.LINE_COMMENT ||
                   elementType == MvelTokenTypes.COMMENT) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.COMMENT)
                .create();
        } else if (elementType == MvelTokenTypes.EQ ||
                   elementType == MvelTokenTypes.NE ||
                   elementType == MvelTokenTypes.LT ||
                   elementType == MvelTokenTypes.GT ||
                   elementType == MvelTokenTypes.LE ||
                   elementType == MvelTokenTypes.GE ||
                   elementType == MvelTokenTypes.AND ||
                   elementType == MvelTokenTypes.OR ||
                   elementType == MvelTokenTypes.NOT ||
                   elementType == MvelTokenTypes.ASSIGN ||
                   elementType == MvelTokenTypes.PLUS ||
                   elementType == MvelTokenTypes.MINUS ||
                   elementType == MvelTokenTypes.MUL ||
                   elementType == MvelTokenTypes.DIV ||
                   elementType == MvelTokenTypes.MOD) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.OPERATOR)
                .create();
        } else if (elementType == MvelTokenTypes.AT ||
                   elementType == MvelTokenTypes.TEMPLATE_COMMENT ||
                   elementType == MvelTokenTypes.TEMPLATE_CODE ||
                   elementType == MvelTokenTypes.TEMPLATE_INCLUDE ||
                   elementType == MvelTokenTypes.TEMPLATE_INCLUDE_NAMED ||
                   elementType == MvelTokenTypes.TEMPLATE_FOREACH ||
                   elementType == MvelTokenTypes.TEMPLATE_IF ||
                   elementType == MvelTokenTypes.TEMPLATE_ELSE ||
                   elementType == MvelTokenTypes.TEMPLATE_END ||
                   elementType == MvelTokenTypes.TEMPLATE_DECLARE) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(MvelSyntaxHighlighter.TEMPLATE_TAG)
                .create();
        }
    }
}

