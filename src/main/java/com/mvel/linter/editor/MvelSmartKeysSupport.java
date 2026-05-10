package com.mvel.linter.editor;

import org.jetbrains.annotations.Nullable;

public final class MvelSmartKeysSupport {
    private static final String DEFAULT_INDENT = "    ";

    private MvelSmartKeysSupport() {
    }

    public static boolean isOpeningBrace(char ch) {
        return ch == '{' || ch == '(' || ch == '[';
    }

    @Nullable
    public static Character closingBrace(char ch) {
        return switch (ch) {
            case '{' -> '}';
            case '(' -> ')';
            case '[' -> ']';
            default -> null;
        };
    }

    public static boolean shouldAutoInsertClosingBrace(CharSequence text, int caretOffset, char openingBrace) {
        Character closingBrace = closingBrace(openingBrace);
        if (closingBrace == null) {
            return false;
        }

        if (caretOffset >= text.length()) {
            return true;
        }

        char next = text.charAt(caretOffset);
        return Character.isWhitespace(next) ||
                next == closingBrace ||
                next == ')' ||
                next == ']' ||
                next == '}' ||
                next == ',' ||
                next == ';' ||
                next == ':';
    }

    @Nullable
    public static BraceEnterExpansion buildBraceEnterExpansion(CharSequence text, int caretOffset) {
        if (caretOffset <= 0 || caretOffset >= text.length()) {
            return null;
        }

        if (text.charAt(caretOffset - 1) != '{' || text.charAt(caretOffset) != '}') {
            return null;
        }

        String baseIndent = extractLineIndent(text, caretOffset - 1);
        String innerIndent = baseIndent + inferIndentUnit(baseIndent);
        String replacement = "{\n" + innerIndent + "\n" + baseIndent + "}";
        int caretShift = 2 + innerIndent.length();
        return new BraceEnterExpansion(caretOffset - 1, caretOffset + 1, replacement, caretShift);
    }

    private static String extractLineIndent(CharSequence text, int offset) {
        int lineStart = offset;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        int indentEnd = lineStart;
        while (indentEnd < text.length()) {
            char ch = text.charAt(indentEnd);
            if (ch != ' ' && ch != '\t') {
                break;
            }
            indentEnd++;
        }
        return text.subSequence(lineStart, indentEnd).toString();
    }

    private static String inferIndentUnit(String baseIndent) {
        return baseIndent.indexOf('\t') >= 0 ? "\t" : DEFAULT_INDENT;
    }

    public static final class BraceEnterExpansion {
        private final int startOffset;
        private final int endOffset;
        private final String replacement;
        private final int caretShift;

        public BraceEnterExpansion(int startOffset, int endOffset, String replacement, int caretShift) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.replacement = replacement;
            this.caretShift = caretShift;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public String getReplacement() {
            return replacement;
        }

        public int getCaretShift() {
            return caretShift;
        }
    }
}
