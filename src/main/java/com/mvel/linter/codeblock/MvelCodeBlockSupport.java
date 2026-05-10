package com.mvel.linter.codeblock;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.mvel.linter.psi.impl.MvelCodeBlockTextPsi;
import com.mvel.linter.psi.impl.MvelTemplateBlockImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MvelCodeBlockSupport {
    private static final boolean JAVA_INJECTION_ENABLED = false;

    private MvelCodeBlockSupport() {
    }

    public static boolean isInsideJavaCodeBlock(@Nullable PsiElement element) {
        if (!JAVA_INJECTION_ENABLED) {
            return false;
        }
        return findEnclosingCodeBlockText(element) != null || findEnclosingJavaCodeBlock(element) != null;
    }

    public static @Nullable MvelCodeBlockTextPsi findEnclosingCodeBlockText(@Nullable PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof MvelCodeBlockTextPsi codeBlockText) {
                return codeBlockText;
            }
            current = current.getParent();
        }
        return null;
    }

    public static @Nullable MvelTemplateBlockImpl findEnclosingJavaCodeBlock(@Nullable PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof MvelTemplateBlockImpl templateBlock && templateBlock.isCodeBlock()) {
                return templateBlock;
            }
            current = current.getParent();
        }
        return null;
    }

    public static boolean isInsideJavaCodeBlockContent(@Nullable PsiElement element) {
        if (!JAVA_INJECTION_ENABLED) {
            return false;
        }
        if (findEnclosingCodeBlockText(element) != null) {
            return true;
        }
        MvelTemplateBlockImpl block = findEnclosingJavaCodeBlock(element);
        if (block == null || element == null) {
            return false;
        }

        TextRange contentRange = toAbsoluteRange(block);
        return contentRange.contains(element.getTextRange().getStartOffset());
    }

    public static boolean isInsideJavaCodeBlockContent(@NotNull PsiFile file, int offset) {
        if (!JAVA_INJECTION_ENABLED) {
            return false;
        }
        PsiElement element = file.findElementAt(Math.max(0, offset - 1));
        if (element == null) {
            element = file.findElementAt(offset);
        }
        return isInsideJavaCodeBlockContent(element);
    }

    private static @NotNull TextRange toAbsoluteRange(@NotNull MvelTemplateBlockImpl block) {
        TextRange relative = block.getContentRangeInElement();
        int start = block.getTextRange().getStartOffset() + relative.getStartOffset();
        int end = block.getTextRange().getStartOffset() + relative.getEndOffset();
        return new TextRange(start, end);
    }
}
