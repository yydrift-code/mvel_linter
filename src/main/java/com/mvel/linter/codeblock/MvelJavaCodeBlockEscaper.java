package com.mvel.linter.codeblock;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.mvel.linter.psi.impl.MvelTemplateBlockImpl;
import org.jetbrains.annotations.NotNull;

public final class MvelJavaCodeBlockEscaper extends LiteralTextEscaper<MvelTemplateBlockImpl> {
    public MvelJavaCodeBlockEscaper(@NotNull MvelTemplateBlockImpl host) {
        super(host);
    }

    @Override
    public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        MvelJavaCodeBlockModel model = MvelJavaCodeBlockModelService.getInstance(myHost.getProject()).getModel(myHost);
        int hostAbsoluteStart = myHost.getTextRange().getStartOffset() + rangeInsideHost.getStartOffset();
        int hostAbsoluteEnd = myHost.getTextRange().getStartOffset() + rangeInsideHost.getEndOffset();
        int javaStart = model.mapHostToJavaOffset(hostAbsoluteStart);
        int javaEnd = hostAbsoluteEnd <= hostAbsoluteStart
                ? javaStart
                : Math.min(model.javaText().length(), model.mapHostToJavaOffset(hostAbsoluteEnd - 1) + 1);
        outChars.append(model.javaText(), Math.min(javaStart, javaEnd), Math.max(javaStart, javaEnd));
        return true;
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        MvelJavaCodeBlockModel model = MvelJavaCodeBlockModelService.getInstance(myHost.getProject()).getModel(myHost);
        int hostOffset = model.mapJavaToHostOffset(offsetInDecoded) - myHost.getTextRange().getStartOffset();
        return Math.max(rangeInsideHost.getStartOffset(), Math.min(hostOffset, rangeInsideHost.getEndOffset()));
    }

    @Override
    public @NotNull TextRange getRelevantTextRange() {
        return myHost.getContentRangeInElement();
    }

    @Override
    public boolean isOneLine() {
        return false;
    }
}
