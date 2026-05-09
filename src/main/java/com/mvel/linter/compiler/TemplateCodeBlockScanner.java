package com.mvel.linter.compiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

final class TemplateCodeBlockScanner {
    private static final List<TagPattern> TAG_PATTERNS = List.of(
            new TagPattern("@includeNamed{", MvelDiagnostic.SourceKind.INCLUDE_NAMED),
            new TagPattern("@comment{", MvelDiagnostic.SourceKind.COMMENT),
            new TagPattern("@include{", MvelDiagnostic.SourceKind.INCLUDE),
            new TagPattern("@foreach{", MvelDiagnostic.SourceKind.FOREACH),
            new TagPattern("@declare{", MvelDiagnostic.SourceKind.DECLARE),
            new TagPattern("@code{", MvelDiagnostic.SourceKind.CODE_BLOCK),
            new TagPattern("@else{", MvelDiagnostic.SourceKind.ELSE),
            new TagPattern("@end{", MvelDiagnostic.SourceKind.END),
            new TagPattern("@if{", MvelDiagnostic.SourceKind.IF),
            new TagPattern("@{", MvelDiagnostic.SourceKind.ORB)
    );

    private TemplateCodeBlockScanner() {
    }

    static TemplateScan scan(String text) {
        if (text == null || text.isEmpty()) {
            return new TemplateScan(List.of(), List.of());
        }

        List<TemplateFragment> fragments = new ArrayList<>();
        List<MvelDiagnostic> diagnostics = new ArrayList<>();

        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);

            if (current == '"' || current == '\'') {
                index = skipQuotedString(text, index);
                continue;
            }

            if (startsWith(text, index, "//")) {
                index = skipLineComment(text, index);
                continue;
            }

            if (startsWith(text, index, "/*")) {
                index = skipBlockComment(text, index);
                continue;
            }

            if (current == '@') {
                TagPattern tag = matchTag(text, index);
                if (tag != null) {
                    int openBraceOffset = index + tag.literal().length() - 1;
                    int closeBraceOffset = findMatchingBrace(text, openBraceOffset);

                    if (closeBraceOffset < 0) {
                        diagnostics.add(new MvelDiagnostic(
                                "Unclosed " + tag.literal() + " block",
                                MvelDiagnostic.Severity.ERROR,
                                MvelDiagnostic.SourceKind.TEMPLATE,
                                index,
                                Math.min(text.length(), openBraceOffset + 1)
                        ));
                        index += tag.literal().length();
                        continue;
                    }

                    fragments.add(new TemplateFragment(
                            tag.kind(),
                            index,
                            openBraceOffset,
                            openBraceOffset + 1,
                            closeBraceOffset,
                            closeBraceOffset + 1,
                            tag.literal()
                    ));

                    index += tag.literal().length();
                    continue;
                }
            }

            index++;
        }

        fragments.sort(Comparator.comparingInt(TemplateFragment::tagStartOffset));
        diagnostics.addAll(validateTemplateBlocks(fragments));

        return new TemplateScan(List.copyOf(fragments), List.copyOf(diagnostics));
    }

    private static List<MvelDiagnostic> validateTemplateBlocks(List<TemplateFragment> fragments) {
        List<MvelDiagnostic> diagnostics = new ArrayList<>();
        Deque<TemplateBlockState> blockStack = new ArrayDeque<>();

        for (TemplateFragment fragment : fragments) {
            switch (fragment.kind()) {
                case IF -> blockStack.push(new TemplateBlockState(fragment));
                case FOREACH -> blockStack.push(new TemplateBlockState(fragment));
                case ELSE -> {
                    TemplateBlockState state = blockStack.peek();
                    if (state == null || state.fragment().kind() != MvelDiagnostic.SourceKind.IF) {
                        diagnostics.add(new MvelDiagnostic(
                                "Unexpected @else{} block",
                                MvelDiagnostic.Severity.ERROR,
                                MvelDiagnostic.SourceKind.TEMPLATE,
                                fragment.tagStartOffset(),
                                fragment.contentEndOffset()
                        ));
                    } else if (state.elseSeen()) {
                        diagnostics.add(new MvelDiagnostic(
                                "Duplicate @else{} block for the same @if{}",
                                MvelDiagnostic.Severity.ERROR,
                                MvelDiagnostic.SourceKind.TEMPLATE,
                                fragment.tagStartOffset(),
                                fragment.contentEndOffset()
                        ));
                    } else {
                        state.markElseSeen();
                    }
                }
                case END -> {
                    if (blockStack.isEmpty()) {
                        diagnostics.add(new MvelDiagnostic(
                                "Unexpected @end{} block",
                                MvelDiagnostic.Severity.ERROR,
                                MvelDiagnostic.SourceKind.TEMPLATE,
                                fragment.tagStartOffset(),
                                fragment.contentEndOffset()
                        ));
                    } else {
                        blockStack.pop();
                    }
                }
                default -> {
                    // No block balancing required for other template tags.
                }
            }
        }

        while (!blockStack.isEmpty()) {
            TemplateBlockState state = blockStack.removeLast();
            String blockName = state.fragment().kind() == MvelDiagnostic.SourceKind.FOREACH
                    ? "@foreach{}"
                    : "@if{}";
            diagnostics.add(new MvelDiagnostic(
                    "Unclosed " + blockName + " block. expected @end{}",
                    MvelDiagnostic.Severity.ERROR,
                    MvelDiagnostic.SourceKind.TEMPLATE,
                    state.fragment().tagStartOffset(),
                    state.fragment().contentEndOffset()
            ));
        }

        return diagnostics;
    }

    private static TagPattern matchTag(String text, int offset) {
        for (TagPattern pattern : TAG_PATTERNS) {
            if (startsWith(text, offset, pattern.literal())) {
                return pattern;
            }
        }
        return null;
    }

    private static boolean startsWith(String text, int offset, String prefix) {
        return offset >= 0
                && offset + prefix.length() <= text.length()
                && text.startsWith(prefix, offset);
    }

    private static int findMatchingBrace(String text, int openBraceOffset) {
        int depth = 1;
        int index = openBraceOffset + 1;

        while (index < text.length()) {
            char current = text.charAt(index);

            if (current == '"' || current == '\'') {
                index = skipQuotedString(text, index);
                continue;
            }

            if (startsWith(text, index, "//")) {
                index = skipLineComment(text, index);
                continue;
            }

            if (startsWith(text, index, "/*")) {
                index = skipBlockComment(text, index);
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }

            index++;
        }

        return -1;
    }

    private static int skipQuotedString(String text, int offset) {
        char quote = text.charAt(offset);
        int index = offset + 1;

        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == quote) {
                return index + 1;
            }
            index++;
        }

        return text.length();
    }

    private static int skipLineComment(String text, int offset) {
        int index = offset + 2;
        while (index < text.length() && text.charAt(index) != '\n') {
            index++;
        }
        return index;
    }

    private static int skipBlockComment(String text, int offset) {
        int index = offset + 2;
        while (index + 1 < text.length()) {
            if (text.charAt(index) == '*' && text.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return text.length();
    }

    record TemplateScan(List<TemplateFragment> fragments, List<MvelDiagnostic> diagnostics) {
    }

    record TemplateFragment(
            MvelDiagnostic.SourceKind kind,
            int tagStartOffset,
            int openBraceOffset,
            int contentStartOffset,
            int contentEndOffset,
            int closeBraceOffset,
            String tagLiteral
    ) {
        String content(String text) {
            if (contentStartOffset >= contentEndOffset || text == null || text.isEmpty()) {
                return "";
            }
            return text.substring(contentStartOffset, contentEndOffset);
        }

        boolean containsOffset(int offset) {
            return offset >= tagStartOffset && offset <= closeBraceOffset;
        }
    }

    private record TagPattern(String literal, MvelDiagnostic.SourceKind kind) {
    }

    private static final class TemplateBlockState {
        private final TemplateFragment fragment;
        private boolean elseSeen;

        private TemplateBlockState(TemplateFragment fragment) {
            this.fragment = fragment;
        }

        public TemplateFragment fragment() {
            return fragment;
        }

        public boolean elseSeen() {
            return elseSeen;
        }

        public void markElseSeen() {
            elseSeen = true;
        }
    }
}
