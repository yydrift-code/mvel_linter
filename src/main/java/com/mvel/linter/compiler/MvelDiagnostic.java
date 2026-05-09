package com.mvel.linter.compiler;

import com.intellij.openapi.util.TextRange;

public record MvelDiagnostic(
        String message,
        Severity severity,
        SourceKind sourceKind,
        int startOffset,
        int endOffset
) {
    public MvelDiagnostic {
        int safeStart = Math.max(0, startOffset);
        int safeEnd = Math.max(safeStart + 1, endOffset);

        message = message == null || message.isBlank() ? "MVEL compile error" : message;
        severity = severity == null ? Severity.ERROR : severity;
        sourceKind = sourceKind == null ? SourceKind.SCRIPT : sourceKind;
        startOffset = safeStart;
        endOffset = safeEnd;
    }

    public TextRange toTextRange(int textLength) {
        if (textLength <= 0) {
            return TextRange.EMPTY_RANGE;
        }

        int safeStart = Math.min(startOffset, textLength - 1);
        int safeEnd = Math.max(safeStart + 1, Math.min(endOffset, textLength));
        return new TextRange(safeStart, safeEnd);
    }

    public enum Severity {
        ERROR,
        WARNING
    }

    public enum SourceKind {
        SCRIPT,
        TEMPLATE,
        CODE_BLOCK,
        ORB,
        IF,
        FOREACH,
        ELSE,
        END,
        INCLUDE,
        INCLUDE_NAMED,
        DECLARE,
        COMMENT
    }
}
