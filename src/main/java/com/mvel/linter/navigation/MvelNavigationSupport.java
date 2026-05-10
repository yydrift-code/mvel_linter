package com.mvel.linter.navigation;

import org.jetbrains.annotations.Nullable;

public final class MvelNavigationSupport {
    private MvelNavigationSupport() {
    }

    @Nullable
    public static Integer inferArgumentCount(CharSequence text, int methodNameEndOffset) {
        int index = methodNameEndOffset;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        if (index >= text.length() || text.charAt(index) != '(') {
            return null;
        }

        int depth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        int argumentCount = 0;
        boolean sawArgumentContent = false;

        for (int i = index + 1; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '"' || ch == '\'') {
                i = skipQuotedLiteral(text, i);
                sawArgumentContent = true;
                continue;
            }

            if (ch == '/' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '/') {
                    i = skipLineComment(text, i + 2);
                    continue;
                }
                if (next == '*') {
                    i = skipBlockComment(text, i + 2);
                    continue;
                }
            }

            switch (ch) {
                case '(' -> {
                    depth++;
                    sawArgumentContent = true;
                }
                case ')' -> {
                    if (depth == 0 && braceDepth == 0 && bracketDepth == 0) {
                        return sawArgumentContent ? argumentCount + 1 : 0;
                    }
                    if (depth > 0) {
                        depth--;
                    }
                }
                case '{' -> {
                    braceDepth++;
                    sawArgumentContent = true;
                }
                case '}' -> {
                    if (braceDepth > 0) {
                        braceDepth--;
                    }
                }
                case '[' -> {
                    bracketDepth++;
                    sawArgumentContent = true;
                }
                case ']' -> {
                    if (bracketDepth > 0) {
                        bracketDepth--;
                    }
                }
                case ',' -> {
                    if (depth == 0 && braceDepth == 0 && bracketDepth == 0) {
                        argumentCount++;
                    }
                }
                default -> {
                    if (!Character.isWhitespace(ch)) {
                        sawArgumentContent = true;
                    }
                }
            }
        }

        return null;
    }

    public static boolean isUppercaseQualifier(@Nullable String qualifier) {
        return qualifier != null && !qualifier.isEmpty() && Character.isUpperCase(qualifier.charAt(0));
    }

    private static int skipQuotedLiteral(CharSequence text, int index) {
        char quote = text.charAt(index);
        for (int i = index + 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\') {
                i++;
                continue;
            }
            if (ch == quote) {
                return i;
            }
        }
        return text.length() - 1;
    }

    private static int skipLineComment(CharSequence text, int index) {
        for (int i = index; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i;
            }
        }
        return text.length() - 1;
    }

    private static int skipBlockComment(CharSequence text, int index) {
        for (int i = index; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '*' && text.charAt(i + 1) == '/') {
                return i + 1;
            }
        }
        return text.length() - 1;
    }
}
