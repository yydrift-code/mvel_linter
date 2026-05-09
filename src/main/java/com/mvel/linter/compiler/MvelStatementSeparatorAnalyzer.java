package com.mvel.linter.compiler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class MvelStatementSeparatorAnalyzer {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+[\\w.*$]+\\s*$");
    private static final Pattern TYPE_DECLARATION_PATTERN = Pattern.compile(
            "^(?:final\\s+)?(?:(?:byte|short|int|long|float|double|boolean|char|String|BigDecimal|BigInteger|"
                    + "[A-Z_$][\\w$]*|[a-z_$][\\w$]*)(?:\\.[A-Z_$][\\w$]*)*(?:<[^>]+>)?(?:\\[\\])?)\\s+"
                    + "[A-Za-z_$][\\w$]*\\b.*$"
    );
    private static final Set<String> CONTROL_FLOW_PREFIXES = Set.of(
            "if", "for", "foreach", "while", "do", "else", "def", "function", "switch", "try", "catch", "finally"
    );
    private static final Set<String> TERMINATOR_KEYWORDS = Set.of(
            "return", "break", "continue", "throw"
    );

    public @NotNull List<MvelDiagnostic> analyze(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        TemplateCodeBlockScanner.TemplateScan scan = TemplateCodeBlockScanner.scan(text);
        if (scan.fragments().isEmpty()) {
            return analyzeFragment(text, 0, MvelDiagnostic.SourceKind.SCRIPT);
        }

        List<MvelDiagnostic> diagnostics = new ArrayList<>();
        for (TemplateCodeBlockScanner.TemplateFragment fragment : scan.fragments()) {
            if (fragment.kind() != MvelDiagnostic.SourceKind.CODE_BLOCK) {
                continue;
            }
            diagnostics.addAll(analyzeFragment(fragment.content(text), fragment.contentStartOffset(), fragment.kind()));
        }
        return List.copyOf(diagnostics);
    }

    private List<MvelDiagnostic> analyzeFragment(String fragmentText, int absoluteStartOffset, MvelDiagnostic.SourceKind sourceKind) {
        List<StatementLine> lines = collectSignificantLines(fragmentText, absoluteStartOffset);
        List<MvelDiagnostic> diagnostics = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            StatementLine line = lines.get(index);
            if (isContinuationLine(line.text())) {
                continue;
            }

            if (!requiresStatementSeparator(line.text())) {
                continue;
            }

            int endIndex = findStatementEnd(lines, index);
            StatementLine statementEnd = lines.get(endIndex);
            StatementLine nextLine = endIndex + 1 < lines.size() ? lines.get(endIndex + 1) : null;

            if (nextLine == null || !startsNewStatement(nextLine, statementEnd)) {
                continue;
            }

            if (hasExplicitTerminator(statementEnd.text())) {
                continue;
            }

            diagnostics.add(new MvelDiagnostic(
                    "Missing ';' before next statement",
                    MvelDiagnostic.Severity.WARNING,
                    sourceKind,
                    statementEnd.lastNonWhitespaceOffset(),
                    statementEnd.lastNonWhitespaceOffset() + 1
            ));
            index = endIndex;
        }

        return diagnostics;
    }

    private int findStatementEnd(List<StatementLine> lines, int startIndex) {
        StatementLine start = lines.get(startIndex);
        NestingState baseState = start.startState();

        for (int index = startIndex; index < lines.size(); index++) {
            StatementLine line = lines.get(index);
            if (!line.endState().equals(baseState)) {
                continue;
            }

            if (endsWithContinuationToken(line.text())) {
                continue;
            }

            StatementLine nextLine = index + 1 < lines.size() ? lines.get(index + 1) : null;
            if (nextLine != null && isContinuationLine(nextLine.text())) {
                continue;
            }

            return index;
        }

        return startIndex;
    }

    private boolean startsNewStatement(StatementLine nextLine, StatementLine currentLine) {
        if (nextLine == null || isContinuationLine(nextLine.text())) {
            return false;
        }
        return nextLine.startState().equals(currentLine.endState());
    }

    private boolean requiresStatementSeparator(String line) {
        if (line.isEmpty() || line.startsWith("@")) {
            return false;
        }

        String firstToken = firstToken(line);
        if (CONTROL_FLOW_PREFIXES.contains(firstToken)) {
            return false;
        }

        if (TERMINATOR_KEYWORDS.contains(firstToken)) {
            return true;
        }

        if (IMPORT_PATTERN.matcher(line).matches() || TYPE_DECLARATION_PATTERN.matcher(line).matches()) {
            return true;
        }

        return looksLikeAssignment(line) || looksLikeInvocation(line);
    }

    private boolean hasExplicitTerminator(String line) {
        return line.endsWith(";") || line.endsWith("{") || line.endsWith("}") || line.endsWith(",");
    }

    private boolean endsWithContinuationToken(String line) {
        return line.endsWith(".")
                || line.endsWith(",")
                || line.endsWith(":")
                || line.endsWith("(")
                || line.endsWith("[")
                || line.endsWith("{")
                || line.endsWith("+")
                || line.endsWith("-")
                || line.endsWith("*")
                || line.endsWith("/")
                || line.endsWith("&&")
                || line.endsWith("||")
                || line.endsWith("?");
    }

    private boolean isContinuationLine(String line) {
        if (line.isEmpty()) {
            return false;
        }

        return line.startsWith(".")
                || line.startsWith(",")
                || line.startsWith(":")
                || line.startsWith(")")
                || line.startsWith("]")
                || line.startsWith("+")
                || line.startsWith("-")
                || line.startsWith("*")
                || line.startsWith("/")
                || line.startsWith("&&")
                || line.startsWith("||")
                || line.startsWith("?");
    }

    private boolean looksLikeAssignment(String line) {
        if (!line.contains("=")) {
            return false;
        }
        return !line.contains("==")
                && !line.contains("!=")
                && !line.contains(">=")
                && !line.contains("<=")
                && !line.contains("=>");
    }

    private boolean looksLikeInvocation(String line) {
        if (line.startsWith("new ")) {
            return true;
        }

        if (!startsWithIdentifierLikeToken(line)) {
            return false;
        }

        if (!line.contains("(") || !line.contains(")")) {
            return false;
        }

        String firstToken = firstToken(line);
        return !CONTROL_FLOW_PREFIXES.contains(firstToken) && !TERMINATOR_KEYWORDS.contains(firstToken);
    }

    private boolean startsWithIdentifierLikeToken(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        char firstCharacter = line.charAt(0);
        return Character.isLetter(firstCharacter) || firstCharacter == '_' || firstCharacter == '$';
    }

    private List<StatementLine> collectSignificantLines(String text, int absoluteStartOffset) {
        List<StatementLine> lines = new ArrayList<>();
        int lineStart = 0;

        NestingState lineStartState = NestingState.ZERO;
        int parenthesesDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        boolean inSingleQuotedString = false;
        boolean inDoubleQuotedString = false;
        boolean inBlockComment = false;
        boolean inLineComment = false;

        for (int index = 0; index <= text.length(); index++) {
            if (index == text.length() || text.charAt(index) == '\n') {
                String rawLine = text.substring(lineStart, index);
                String normalizedLine = stripTrailingComment(rawLine).trim();
                if (!normalizedLine.isEmpty()) {
                    int lastNonWhitespaceOffset = absoluteStartOffset + findLastNonWhitespaceIndex(text, lineStart, index);
                    lines.add(new StatementLine(
                            normalizedLine,
                            absoluteStartOffset + lineStart,
                            lastNonWhitespaceOffset,
                            lineStartState,
                            new NestingState(parenthesesDepth, bracketDepth, braceDepth)
                    ));
                }

                lineStart = index + 1;
                lineStartState = new NestingState(parenthesesDepth, bracketDepth, braceDepth);
                inLineComment = false;
                continue;
            }

            char current = text.charAt(index);
            char next = index + 1 < text.length() ? text.charAt(index + 1) : '\0';

            if (inLineComment) {
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }

            if (inSingleQuotedString) {
                if (current == '\\' && index + 1 < text.length()) {
                    index++;
                    continue;
                }
                if (current == '\'') {
                    inSingleQuotedString = false;
                }
                continue;
            }

            if (inDoubleQuotedString) {
                if (current == '\\' && index + 1 < text.length()) {
                    index++;
                    continue;
                }
                if (current == '"') {
                    inDoubleQuotedString = false;
                }
                continue;
            }

            if (current == '/' && next == '/') {
                inLineComment = true;
                index++;
                continue;
            }

            if (current == '/' && next == '*') {
                inBlockComment = true;
                index++;
                continue;
            }

            if (current == '\'') {
                inSingleQuotedString = true;
                continue;
            }

            if (current == '"') {
                inDoubleQuotedString = true;
                continue;
            }

            switch (current) {
                case '(' -> parenthesesDepth++;
                case ')' -> parenthesesDepth = Math.max(0, parenthesesDepth - 1);
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                case '{' -> braceDepth++;
                case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                default -> {
                }
            }
        }

        return lines;
    }

    private int findLastNonWhitespaceIndex(String text, int start, int endExclusive) {
        int index = endExclusive - 1;
        while (index >= start && Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return Math.max(start, index);
    }

    private String stripTrailingComment(String line) {
        boolean inSingleQuotedString = false;
        boolean inDoubleQuotedString = false;

        for (int index = 0; index < line.length() - 1; index++) {
            char current = line.charAt(index);
            char next = line.charAt(index + 1);

            if (inSingleQuotedString) {
                if (current == '\\') {
                    index++;
                    continue;
                }
                if (current == '\'') {
                    inSingleQuotedString = false;
                }
                continue;
            }

            if (inDoubleQuotedString) {
                if (current == '\\') {
                    index++;
                    continue;
                }
                if (current == '"') {
                    inDoubleQuotedString = false;
                }
                continue;
            }

            if (current == '\'') {
                inSingleQuotedString = true;
                continue;
            }

            if (current == '"') {
                inDoubleQuotedString = true;
                continue;
            }

            if (current == '/' && next == '/') {
                return line.substring(0, index);
            }
        }

        return line;
    }

    private String firstToken(String line) {
        int separator = line.indexOf(' ');
        return separator < 0 ? line : line.substring(0, separator);
    }

    private record StatementLine(
            String text,
            int absoluteStartOffset,
            int lastNonWhitespaceOffset,
            NestingState startState,
            NestingState endState
    ) {
    }

    private record NestingState(int parenthesesDepth, int bracketDepth, int braceDepth) {
        private static final NestingState ZERO = new NestingState(0, 0, 0);
    }
}
