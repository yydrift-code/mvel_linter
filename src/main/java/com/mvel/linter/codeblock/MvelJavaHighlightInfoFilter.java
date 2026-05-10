package com.mvel.linter.codeblock;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.mvel.linter.psi.impl.MvelTemplateBlockImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class MvelJavaHighlightInfoFilter implements HighlightInfoFilter {
    @Override
    public boolean accept(@NotNull HighlightInfo info, @Nullable PsiFile file) {
        if (file == null || file.getLanguage() != JavaLanguage.INSTANCE) {
            return true;
        }

        PsiElement host = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);
        if (!(host instanceof MvelTemplateBlockImpl templateBlock) || !templateBlock.isCodeBlock()) {
            return true;
        }

        if (info.getSeverity() != HighlightSeverity.ERROR && info.getSeverity() != HighlightSeverity.WARNING) {
            return true;
        }

        String description = info.getDescription();
        if (description == null || description.isBlank()) {
            return true;
        }

        String normalized = description.toLowerCase(Locale.ROOT);
        return normalized.contains("expected")
                || normalized.contains("illegal start")
                || normalized.contains("not a statement")
                || normalized.contains("reached end of file")
                || normalized.contains("unclosed")
                || normalized.contains("';'")
                || normalized.contains("')'")
                || normalized.contains("']'")
                || normalized.contains("'}'")
                || normalized.contains("orphaned")
                || normalized.contains("catch without try")
                || normalized.contains("else without if");
    }
}
