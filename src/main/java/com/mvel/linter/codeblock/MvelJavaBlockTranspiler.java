package com.mvel.linter.codeblock;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.compiler.MvelDiagnostic;
import com.mvel.linter.lexer.MvelLexer;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class MvelJavaBlockTranspiler {
    private static final String WRAPPER_CLASS_NAME = "__MvelBlock__";
    private static final String EMPTY_LITERAL_NAME = "__MVEL_EMPTY__";
    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_$][\\w$]*");
    private static final Pattern BARE_FOREACH_VARIABLE = Pattern.compile("^(?:final\\s+)?([A-Za-z_$][\\w$]*)$");
    private static final Set<String> RESERVED_WORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"
    );
    private static final Set<String> KEYWORD_LIKE_IDENTIFIERS = Set.of(
            "if", "else", "for", "foreach", "while", "do", "until", "return", "new", "function", "def",
            "isdef", "with", "assert", "empty", "null", "nil", "true", "false"
    );

    public @NotNull MvelJavaCodeBlockModel transpile(@NotNull String hostText, @NotNull TextRange contentRange) {
        if (contentRange.isEmpty() || contentRange.getEndOffset() > hostText.length()) {
            return emptyModel(hostText.length());
        }

        String content = hostText.substring(contentRange.getStartOffset(), contentRange.getEndOffset());
        List<MvelDiagnostic> diagnostics = new ArrayList<>();
        List<TopLevelSegment> segments = splitTopLevel(content, contentRange.getStartOffset(), diagnostics);
        Set<String> localMethodNames = collectLocalMethodNames(segments);
        FieldCollection fieldCollection = collectFieldCandidates(content, contentRange.getStartOffset(), segments, localMethodNames);

        MappedTextBuilder builder = new MappedTextBuilder(hostText.length(), contentRange.getStartOffset(), contentRange.getEndOffset());
        appendBodyInSourceOrder(builder, content, contentRange.getStartOffset(), segments, fieldCollection.names());

        return new MvelJavaCodeBlockModel(
                buildPrefix(segments, fieldCollection, contentRange.getStartOffset()),
                builder.text(),
                buildSuffix(contentRange.getEndOffset()),
                builder.javaToHostOffsets(),
                builder.hostToJavaOffsets(),
                List.copyOf(diagnostics)
        );
    }

    public @NotNull MvelJavaCodeBlockModel transpileContent(
            @NotNull String content,
            int absoluteStartOffset,
            int fileTextLength
    ) {
        if (content.isEmpty()) {
            return emptyModel(Math.max(1, fileTextLength));
        }

        List<MvelDiagnostic> diagnostics = new ArrayList<>();
        List<TopLevelSegment> segments = splitTopLevel(content, absoluteStartOffset, diagnostics);
        Set<String> localMethodNames = collectLocalMethodNames(segments);
        FieldCollection fieldCollection = collectFieldCandidates(content, absoluteStartOffset, segments, localMethodNames);

        MappedTextBuilder builder = new MappedTextBuilder(fileTextLength, absoluteStartOffset, absoluteStartOffset + content.length());
        appendBodyInSourceOrder(builder, content, absoluteStartOffset, segments, fieldCollection.names());

        return new MvelJavaCodeBlockModel(
                buildPrefix(segments, fieldCollection, absoluteStartOffset),
                builder.text(),
                buildSuffix(absoluteStartOffset + content.length()),
                builder.javaToHostOffsets(),
                builder.hostToJavaOffsets(),
                List.copyOf(diagnostics)
        );
    }

    private @NotNull MvelJavaCodeBlockModel emptyModel(int hostTextLength) {
        int[] javaToHost = new int[]{0};
        int[] hostToJava = new int[Math.max(1, hostTextLength + 1)];
        return new MvelJavaCodeBlockModel(buildPrefix(List.of(), new FieldCollection(Set.of(), new LinkedHashMap<>()), 0), "", buildSuffix(0), javaToHost, hostToJava, List.of());
    }

    private String buildPrefix(List<TopLevelSegment> segments, FieldCollection fieldCollection, int anchorOffset) {
        LinkedHashSet<String> imports = new LinkedHashSet<>();
        imports.add("import java.util.*;");
        for (TopLevelSegment segment : segments) {
            if (segment.kind != SegmentKind.IMPORT) {
                continue;
            }
            String importText = segment.text.trim();
            if (!importText.endsWith(";")) {
                importText = importText + ";";
            }
            imports.add(importText);
        }

        StringBuilder prefix = new StringBuilder();
        for (String importLine : imports) {
            prefix.append(importLine).append('\n');
        }
        prefix.append("final class ").append(WRAPPER_CLASS_NAME).append(" {\n");
        prefix.append("  private static final Object ").append(EMPTY_LITERAL_NAME).append(" = new Object();\n");
        for (Map.Entry<String, Integer> entry : fieldCollection.firstOffsets().entrySet()) {
            prefix.append("  Object ").append(entry.getKey()).append(";\n");
        }
        if (!fieldCollection.firstOffsets().isEmpty()) {
            prefix.append('\n');
        }
        return prefix.toString();
    }

    private String buildSuffix(int anchorOffset) {
        return "  private static Iterable __mvelIter(Object value) { return Collections.emptyList(); }\n"
                + "  private static java.util.List __mvelList(Object... values) { return Arrays.asList(values); }\n"
                + "  private static java.util.Map __mvelMap(Object... values) { return new LinkedHashMap(); }\n"
                + "}\n";
    }

    private void appendBodyInSourceOrder(
            MappedTextBuilder builder,
            String content,
            int baseOffset,
            List<TopLevelSegment> segments,
            Set<String> globalFieldNames
    ) {
        StringBuilder topLevelContent = new StringBuilder();
        int topLevelStartOffset = -1;
        int cursor = 0;
        for (TopLevelSegment segment : segments) {
            if (segment.startOffset > cursor) {
                if (topLevelStartOffset < 0) {
                    topLevelStartOffset = baseOffset + cursor;
                }
                topLevelContent.append(content, cursor, segment.startOffset);
            }
            if (segment.kind == SegmentKind.METHOD) {
                appendMethod(builder, segment, globalFieldNames);
            }
            cursor = segment.endOffset;
        }
        if (cursor < content.length()) {
            if (topLevelStartOffset < 0) {
                topLevelStartOffset = baseOffset + cursor;
            }
            topLevelContent.append(content.substring(cursor));
        }

        if (topLevelStartOffset >= 0) {
            appendMainBodyMethod(builder, topLevelContent.toString(), topLevelStartOffset, globalFieldNames);
        }
    }

    private void appendMainBodyMethod(
            MappedTextBuilder builder,
            String content,
            int absoluteStartOffset,
            Set<String> globalFieldNames
    ) {
        if (!containsMeaningfulCode(content)) {
            return;
        }
        ScopeState scopeState = new ScopeState(globalFieldNames);
        builder.appendSynthetic("  void __mvelMain__() {\n", absoluteStartOffset);
        rewriteInto(builder, content, absoluteStartOffset, scopeState);
        if (!builder.endsWithNewline()) {
            builder.appendSynthetic("\n", absoluteStartOffset + content.length());
        }
        builder.appendSynthetic("  }\n\n", absoluteStartOffset + content.length());
    }

    private void appendMethod(MappedTextBuilder builder, TopLevelSegment segment, Set<String> globalFieldNames) {
        ScopeState scopeState = new ScopeState(globalFieldNames);
        for (ParameterInfo parameter : segment.parameters) {
            scopeState.declare(parameter.name);
        }

        int anchorOffset = segment.absoluteStartOffset();
        builder.appendSynthetic("  Object ", anchorOffset);
        builder.appendSynthetic(segment.name, segment.nameOffset);
        builder.appendSynthetic("(", segment.parameters.isEmpty() ? anchorOffset : segment.parameters.get(0).offset);
        for (int index = 0; index < segment.parameters.size(); index++) {
            ParameterInfo parameter = segment.parameters.get(index);
            if (index > 0) {
                builder.appendSynthetic(", ", parameter.offset);
            }
            builder.appendSynthetic("Object ", parameter.offset);
            builder.appendSynthetic(parameter.name, parameter.offset);
        }
        builder.appendSynthetic(") {\n", anchorOffset);

        String bodyText = segment.bodyText == null ? "" : segment.bodyText;
        int bodyStartOffset = segment.bodyStartOffset >= 0 ? segment.bodyStartOffset : segment.absoluteStartOffset();
        rewriteInto(builder, bodyText, bodyStartOffset, scopeState);
        if (!containsReturnStatement(bodyText)) {
            if (!builder.endsWithNewline()) {
                builder.appendSynthetic("\n", segment.absoluteEndOffset() - 1);
            }
            builder.appendSynthetic("    return null;\n", segment.absoluteEndOffset() - 1);
        } else if (!builder.endsWithNewline()) {
            builder.appendSynthetic("\n", segment.absoluteEndOffset() - 1);
        }
        builder.appendSynthetic("  }\n\n", segment.absoluteEndOffset() - 1);
    }

    private List<TopLevelSegment> splitTopLevel(String text, int baseOffset, List<MvelDiagnostic> diagnostics) {
        List<TopLevelSegment> segments = new ArrayList<>();
        int index = 0;

        while (index < text.length()) {
            index = skipWhitespaceAndComments(text, index);
            if (index >= text.length()) {
                break;
            }

            if (looksLikeWord(text, index, "import")) {
                int statementEnd = findTopLevelStatementEnd(text, index);
                segments.add(new TopLevelSegment(
                        SegmentKind.IMPORT,
                        text.substring(index, statementEnd),
                        index,
                        statementEnd,
                        baseOffset + index,
                        baseOffset + statementEnd
                ));
                index = statementEnd;
                continue;
            }

            if (looksLikeWord(text, index, "def") || looksLikeWord(text, index, "function")) {
                TopLevelSegment method = parseMethodSegment(text, index, baseOffset, diagnostics);
                if (method != null) {
                    segments.add(method);
                    index = method.endOffset;
                    continue;
                }
            }

            index++;
        }

        segments.sort(Comparator.comparingInt(segment -> segment.startOffset));
        return segments;
    }

    private TopLevelSegment parseMethodSegment(String text, int keywordOffset, int baseOffset, List<MvelDiagnostic> diagnostics) {
        int index = keywordOffset;
        String keyword = looksLikeWord(text, keywordOffset, "def") ? "def" : "function";
        index += keyword.length();
        index = skipWhitespaceAndComments(text, index);

        int nameStart = index;
        while (index < text.length() && Character.isJavaIdentifierPart(text.charAt(index))) {
            index++;
        }
        if (nameStart == index) {
            diagnostics.add(new MvelDiagnostic(
                    "Malformed function definition in Java-first @code mode",
                    MvelDiagnostic.Severity.ERROR,
                    MvelDiagnostic.SourceKind.CODE_BLOCK,
                    baseOffset + keywordOffset,
                    Math.min(baseOffset + text.length(), baseOffset + keywordOffset + keyword.length())
            ));
            return null;
        }

        String name = text.substring(nameStart, index);
        index = skipWhitespaceAndComments(text, index);
        if (index >= text.length() || text.charAt(index) != '(') {
            diagnostics.add(new MvelDiagnostic(
                    "Malformed function definition in Java-first @code mode",
                    MvelDiagnostic.Severity.ERROR,
                    MvelDiagnostic.SourceKind.CODE_BLOCK,
                    baseOffset + keywordOffset,
                    Math.min(baseOffset + text.length(), baseOffset + nameStart + name.length())
            ));
            return null;
        }

        int paramsStart = index + 1;
        int paramsEnd = findMatchingDelimiter(text, index, '(', ')');
        if (paramsEnd < 0) {
            diagnostics.add(new MvelDiagnostic(
                    "Unclosed function parameter list in Java-first @code mode",
                    MvelDiagnostic.Severity.ERROR,
                    MvelDiagnostic.SourceKind.CODE_BLOCK,
                    baseOffset + index,
                    Math.min(baseOffset + text.length(), baseOffset + index + 1)
            ));
            return null;
        }

        List<ParameterInfo> parameters = parseParameters(text.substring(paramsStart, paramsEnd), baseOffset + paramsStart);
        index = skipWhitespaceAndComments(text, paramsEnd + 1);
        if (index < text.length() && text.charAt(index) == '{') {
            int bodyStart = index + 1;
            int bodyEnd = findMatchingDelimiter(text, index, '{', '}');
            if (bodyEnd < 0) {
                diagnostics.add(new MvelDiagnostic(
                        "Unclosed function body in Java-first @code mode",
                        MvelDiagnostic.Severity.ERROR,
                        MvelDiagnostic.SourceKind.CODE_BLOCK,
                        baseOffset + index,
                        Math.min(baseOffset + text.length(), baseOffset + index + 1)
                ));
                return null;
            }
            return TopLevelSegment.method(
                    text.substring(keywordOffset, bodyEnd + 1),
                    keywordOffset,
                    bodyEnd + 1,
                    baseOffset + keywordOffset,
                    baseOffset + bodyEnd + 1,
                    name,
                    baseOffset + nameStart,
                    parameters,
                    text.substring(bodyStart, bodyEnd),
                    baseOffset + bodyStart
            );
        }

        int statementEnd = findTopLevelStatementEnd(text, index);
        String expressionBody = text.substring(index, statementEnd).trim();
        return TopLevelSegment.method(
                text.substring(keywordOffset, statementEnd),
                keywordOffset,
                statementEnd,
                baseOffset + keywordOffset,
                baseOffset + statementEnd,
                name,
                baseOffset + nameStart,
                parameters,
                expressionBody.isEmpty() ? "" : "return " + expressionBody + ";",
                baseOffset + index
        );
    }

    private List<ParameterInfo> parseParameters(String paramsText, int baseOffset) {
        List<ParameterInfo> parameters = new ArrayList<>();
        int index = 0;
        while (index < paramsText.length()) {
            index = skipWhitespace(paramsText, index);
            int start = index;
            while (index < paramsText.length() && Character.isJavaIdentifierPart(paramsText.charAt(index))) {
                index++;
            }
            if (start == index) {
                break;
            }
            parameters.add(new ParameterInfo(paramsText.substring(start, index), baseOffset + start));
            index = skipWhitespace(paramsText, index);
            if (index < paramsText.length() && paramsText.charAt(index) == ',') {
                index++;
            }
        }
        return parameters;
    }

    private Set<String> collectLocalMethodNames(List<TopLevelSegment> segments) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (TopLevelSegment segment : segments) {
            if (segment.kind == SegmentKind.METHOD) {
                names.add(segment.name);
            }
        }
        return names;
    }

    private FieldCollection collectFieldCandidates(
            String content,
            int baseOffset,
            List<TopLevelSegment> segments,
            Set<String> localMethodNames
    ) {
        List<TokenInfo> tokens = tokenize(content);
        Set<TextRange> excludedRanges = new LinkedHashSet<>();
        for (TopLevelSegment segment : segments) {
            if (segment.kind == SegmentKind.IMPORT) {
                excludedRanges.add(new TextRange(segment.startOffset, segment.endOffset));
            }
        }

        LinkedHashMap<String, Integer> fields = new LinkedHashMap<>();
        for (int index = 0; index < tokens.size(); index++) {
            TokenInfo token = tokens.get(index);
            if (token.type != MvelTokenTypes.IDENTIFIER) {
                continue;
            }
            if (KEYWORD_LIKE_IDENTIFIERS.contains(token.text) || localMethodNames.contains(token.text)) {
                continue;
            }
            if (token.text.isEmpty() || RESERVED_WORDS.contains(token.text)) {
                continue;
            }
            if (isExcluded(token.startOffset, excludedRanges)) {
                continue;
            }

            IElementType previousType = previousSignificantType(tokens, index);
            IElementType nextType = nextSignificantType(tokens, index);
            if (previousType == MvelTokenTypes.DOT ||
                    previousType == MvelTokenTypes.DEF ||
                    previousType == MvelTokenTypes.FUNCTION ||
                    previousType == MvelTokenTypes.NEW) {
                continue;
            }
            if (Character.isUpperCase(token.text.charAt(0)) && nextType == MvelTokenTypes.DOT) {
                continue;
            }

            fields.putIfAbsent(token.text, baseOffset + token.startOffset);
        }

        return new FieldCollection(Set.copyOf(fields.keySet()), fields);
    }

    private boolean isExcluded(int offset, Set<TextRange> excludedRanges) {
        for (TextRange range : excludedRanges) {
            if (range.containsOffset(offset)) {
                return true;
            }
        }
        return false;
    }

    private List<TokenInfo> tokenize(String text) {
        MvelLexer lexer = new MvelLexer();
        lexer.start(text, 0, text.length(), 0);

        List<TokenInfo> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            int start = lexer.getTokenStart();
            int end = lexer.getTokenEnd();
            tokens.add(new TokenInfo(
                    lexer.getTokenType(),
                    text.substring(start, end),
                    start,
                    end
            ));
            lexer.advance();
        }
        return tokens;
    }

    private IElementType previousSignificantType(List<TokenInfo> tokens, int index) {
        for (int i = index - 1; i >= 0; i--) {
            IElementType type = tokens.get(i).type;
            if (type != MvelTokenTypes.WHITESPACE && type != MvelTokenTypes.COMMENT && type != MvelTokenTypes.LINE_COMMENT) {
                return type;
            }
        }
        return null;
    }

    private IElementType nextSignificantType(List<TokenInfo> tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            IElementType type = tokens.get(i).type;
            if (type != MvelTokenTypes.WHITESPACE && type != MvelTokenTypes.COMMENT && type != MvelTokenTypes.LINE_COMMENT) {
                return type;
            }
        }
        return null;
    }

    private void rewriteInto(MappedTextBuilder builder, String text, int absoluteStartOffset, ScopeState scopeState) {
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            int absoluteOffset = absoluteStartOffset + index;

            if (current == '"' || current == '\'') {
                int end = skipQuotedString(text, index);
                builder.appendOriginal(text.substring(index, end), absoluteOffset);
                index = end;
                continue;
            }

            if (startsWith(text, index, "//")) {
                int end = skipLineComment(text, index);
                builder.appendOriginal(text.substring(index, end), absoluteOffset);
                index = end;
                continue;
            }

            if (startsWith(text, index, "/*")) {
                int end = skipBlockComment(text, index);
                builder.appendOriginal(text.substring(index, end), absoluteOffset);
                index = end;
                continue;
            }

            if (looksLikeWord(text, index, "for") || looksLikeWord(text, index, "foreach")) {
                int keywordLength = looksLikeWord(text, index, "foreach") ? "foreach".length() : "for".length();
                int afterKeyword = skipWhitespaceAndComments(text, index + keywordLength);
                if (afterKeyword < text.length() && text.charAt(afterKeyword) == '(') {
                    int headerEnd = findMatchingDelimiter(text, afterKeyword, '(', ')');
                    if (headerEnd > afterKeyword) {
                        String header = text.substring(afterKeyword + 1, headerEnd);
                        ForHeaderRewrite rewritten = rewriteForHeader(header, absoluteStartOffset + afterKeyword + 1, scopeState);
                        builder.appendSynthetic("for", absoluteOffset);
                        builder.appendOriginal(text.substring(index + keywordLength, afterKeyword + 1), absoluteStartOffset + index + keywordLength);
                        builder.appendSynthetic(rewritten.text, absoluteStartOffset + afterKeyword + 1);
                        builder.appendSynthetic(")", absoluteStartOffset + headerEnd);
                        index = headerEnd + 1;
                        continue;
                    }
                }
            }

            if (current == '[' && isLiteralStart(text, index)) {
                int end = findMatchingDelimiter(text, index, '[', ']');
                if (end > index) {
                    rewriteBracketLiteral(builder, text.substring(index + 1, end), absoluteOffset, scopeState);
                    index = end + 1;
                    continue;
                }
            }

            if (current == '{' && isCurlyLiteralStart(text, index)) {
                int end = findMatchingDelimiter(text, index, '{', '}');
                if (end > index) {
                    rewriteCurlyLiteral(builder, text.substring(index + 1, end), absoluteOffset, scopeState);
                    index = end + 1;
                    continue;
                }
            }

            if (isWordStart(text, index)) {
                int end = readWordEnd(text, index);
                String word = text.substring(index, end);
                if ("empty".equals(word)) {
                    builder.appendSynthetic(EMPTY_LITERAL_NAME, absoluteOffset);
                    index = end;
                    continue;
                }
                if (shouldDeclareLocal(text, index, end, word, scopeState)) {
                    builder.appendSynthetic("Object ", absoluteOffset);
                    scopeState.declare(word);
                }
                builder.appendOriginal(word, absoluteOffset);
                index = end;
                continue;
            }

            builder.appendChar(current, absoluteOffset);
            index++;
        }
    }

    private boolean shouldDeclareLocal(String text, int start, int end, String word, ScopeState scopeState) {
        if (!SIMPLE_IDENTIFIER.matcher(word).matches() || scopeState.isDeclared(word) || scopeState.isGlobalField(word)) {
            return false;
        }
        int previous = previousSignificantCharIndex(text, start);
        if (previous >= 0) {
            char previousChar = text.charAt(previous);
            if (previousChar == '.' || previousChar == ':' || previousChar == '@') {
                return false;
            }
        }
        int next = nextSignificantCharIndex(text, end);
        if (next < 0 || text.charAt(next) != '=') {
            return false;
        }
        return next + 1 >= text.length() || text.charAt(next + 1) != '=';
    }

    private ForHeaderRewrite rewriteForHeader(String header, int absoluteHeaderStart, ScopeState scopeState) {
        if (header.indexOf(';') >= 0) {
            MappedTextBuilder builder = new MappedTextBuilder(absoluteHeaderStart + header.length() + 1, absoluteHeaderStart, absoluteHeaderStart + header.length());
            rewriteInto(builder, header, absoluteHeaderStart, scopeState);
            return new ForHeaderRewrite(builder.text());
        }

        int colonOffset = findTopLevelColon(header);
        if (colonOffset < 0) {
            MappedTextBuilder builder = new MappedTextBuilder(absoluteHeaderStart + header.length() + 1, absoluteHeaderStart, absoluteHeaderStart + header.length());
            rewriteInto(builder, header, absoluteHeaderStart, scopeState);
            return new ForHeaderRewrite(builder.text());
        }

        String left = header.substring(0, colonOffset).trim();
        String right = header.substring(colonOffset + 1).trim();
        java.util.regex.Matcher matcher = BARE_FOREACH_VARIABLE.matcher(left);
        if (!matcher.matches()) {
            MappedTextBuilder builder = new MappedTextBuilder(absoluteHeaderStart + header.length() + 1, absoluteHeaderStart, absoluteHeaderStart + header.length());
            rewriteInto(builder, header, absoluteHeaderStart, scopeState);
            return new ForHeaderRewrite(builder.text());
        }

        String variable = matcher.group(1);
        scopeState.declare(variable);
        MappedTextBuilder expressionBuilder = new MappedTextBuilder(absoluteHeaderStart + header.length() + 1, absoluteHeaderStart, absoluteHeaderStart + header.length());
        rewriteInto(expressionBuilder, right, absoluteHeaderStart + colonOffset + 1, scopeState);
        return new ForHeaderRewrite("Object " + variable + " : __mvelIter(" + expressionBuilder.text() + ")");
    }

    private void rewriteBracketLiteral(MappedTextBuilder builder, String content, int absoluteOpenOffset, ScopeState scopeState) {
        if (hasTopLevelColon(content)) {
            builder.appendSynthetic("__mvelMap(", absoluteOpenOffset);
            appendMapEntries(builder, content, absoluteOpenOffset + 1, scopeState);
            builder.appendSynthetic(")", absoluteOpenOffset);
            return;
        }

        builder.appendSynthetic("__mvelList(", absoluteOpenOffset);
        appendDelimitedExpressions(builder, content, absoluteOpenOffset + 1, scopeState);
        builder.appendSynthetic(")", absoluteOpenOffset);
    }

    private void rewriteCurlyLiteral(MappedTextBuilder builder, String content, int absoluteOpenOffset, ScopeState scopeState) {
        builder.appendSynthetic("__mvelList(", absoluteOpenOffset);
        appendDelimitedExpressions(builder, content, absoluteOpenOffset + 1, scopeState);
        builder.appendSynthetic(")", absoluteOpenOffset);
    }

    private void appendDelimitedExpressions(MappedTextBuilder builder, String content, int absoluteStartOffset, ScopeState scopeState) {
        List<TextSlice> expressions = splitTopLevel(content, absoluteStartOffset, ',');
        for (int index = 0; index < expressions.size(); index++) {
            TextSlice expression = expressions.get(index);
            if (index > 0) {
                builder.appendSynthetic(", ", expression.absoluteStartOffset);
            }
            rewriteInto(builder, expression.text.trim(), expression.trimmedAbsoluteStart(), scopeState);
        }
    }

    private void appendMapEntries(MappedTextBuilder builder, String content, int absoluteStartOffset, ScopeState scopeState) {
        List<TextSlice> entries = splitTopLevel(content, absoluteStartOffset, ',');
        boolean first = true;
        for (TextSlice entry : entries) {
            if (entry.text.isBlank()) {
                continue;
            }
            int colonOffset = findTopLevelColon(entry.text);
            if (colonOffset < 0) {
                if (!first) {
                    builder.appendSynthetic(", ", entry.absoluteStartOffset);
                }
                rewriteInto(builder, entry.text.trim(), entry.trimmedAbsoluteStart(), scopeState);
                first = false;
                continue;
            }

            TextSlice key = entry.slice(0, colonOffset);
            TextSlice value = entry.slice(colonOffset + 1, entry.text.length());
            if (!first) {
                builder.appendSynthetic(", ", entry.absoluteStartOffset);
            }
            rewriteInto(builder, key.text.trim(), key.trimmedAbsoluteStart(), scopeState);
            builder.appendSynthetic(", ", value.absoluteStartOffset);
            rewriteInto(builder, value.text.trim(), value.trimmedAbsoluteStart(), scopeState);
            first = false;
        }
    }

    private List<TextSlice> splitTopLevel(String text, int absoluteStartOffset, char delimiter) {
        List<TextSlice> slices = new ArrayList<>();
        int depthParen = 0;
        int depthBracket = 0;
        int depthBrace = 0;
        int start = 0;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"' || current == '\'') {
                index = skipQuotedString(text, index) - 1;
                continue;
            }
            if (startsWith(text, index, "//")) {
                index = skipLineComment(text, index) - 1;
                continue;
            }
            if (startsWith(text, index, "/*")) {
                index = skipBlockComment(text, index) - 1;
                continue;
            }

            switch (current) {
                case '(' -> depthParen++;
                case ')' -> depthParen = Math.max(0, depthParen - 1);
                case '[' -> depthBracket++;
                case ']' -> depthBracket = Math.max(0, depthBracket - 1);
                case '{' -> depthBrace++;
                case '}' -> depthBrace = Math.max(0, depthBrace - 1);
                default -> {
                }
            }

            if (current == delimiter && depthParen == 0 && depthBracket == 0 && depthBrace == 0) {
                slices.add(new TextSlice(text.substring(start, index), absoluteStartOffset + start));
                start = index + 1;
            }
        }

        slices.add(new TextSlice(text.substring(start), absoluteStartOffset + start));
        return slices;
    }

    private boolean hasTopLevelColon(String text) {
        return findTopLevelColon(text) >= 0;
    }

    private int findTopLevelColon(String text) {
        int depthParen = 0;
        int depthBracket = 0;
        int depthBrace = 0;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"' || current == '\'') {
                index = skipQuotedString(text, index) - 1;
                continue;
            }
            if (startsWith(text, index, "//")) {
                index = skipLineComment(text, index) - 1;
                continue;
            }
            if (startsWith(text, index, "/*")) {
                index = skipBlockComment(text, index) - 1;
                continue;
            }

            switch (current) {
                case '(' -> depthParen++;
                case ')' -> depthParen = Math.max(0, depthParen - 1);
                case '[' -> depthBracket++;
                case ']' -> depthBracket = Math.max(0, depthBracket - 1);
                case '{' -> depthBrace++;
                case '}' -> depthBrace = Math.max(0, depthBrace - 1);
                case ':' -> {
                    if (depthParen == 0 && depthBracket == 0 && depthBrace == 0) {
                        return index;
                    }
                }
                default -> {
                }
            }
        }
        return -1;
    }

    private boolean containsReturnStatement(String bodyText) {
        return bodyText.contains("return");
    }

    private boolean containsMeaningfulCode(String content) {
        int index = 0;
        while (index < content.length()) {
            char current = content.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (startsWith(content, index, "//")) {
                index = skipLineComment(content, index);
                continue;
            }
            if (startsWith(content, index, "/*")) {
                index = skipBlockComment(content, index);
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean isLiteralStart(String text, int index) {
        int previous = previousSignificantCharIndex(text, index);
        if (previous < 0) {
            return true;
        }

        char previousChar = text.charAt(previous);
        if ("=(:,?[!+-*/%&|".indexOf(previousChar) >= 0) {
            return true;
        }

        if (previousChar == '(') {
            String previousWord = previousMeaningfulWord(text, previous);
            return "return".equals(previousWord) || "new".equals(previousWord) || "if".equals(previousWord);
        }

        return false;
    }

    private boolean isCurlyLiteralStart(String text, int index) {
        int previous = previousSignificantCharIndex(text, index);
        if (previous < 0) {
            return true;
        }

        char previousChar = text.charAt(previous);
        if ("=(:,?[!+-*/%&|".indexOf(previousChar) >= 0) {
            return true;
        }

        String previousWord = previousMeaningfulWord(text, previous + 1);
        return "return".equals(previousWord);
    }

    private String previousMeaningfulWord(String text, int beforeIndex) {
        int index = beforeIndex;
        while (index >= 0 && Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        int end = index + 1;
        while (index >= 0 && Character.isJavaIdentifierPart(text.charAt(index))) {
            index--;
        }
        if (end <= index + 1) {
            return "";
        }
        return text.substring(index + 1, end).toLowerCase(Locale.ROOT);
    }

    private int previousSignificantCharIndex(String text, int beforeIndex) {
        int index = beforeIndex - 1;
        while (index >= 0 && Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index;
    }

    private int nextSignificantCharIndex(String text, int fromIndex) {
        int index = fromIndex;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index < text.length() ? index : -1;
    }

    private int findMatchingDelimiter(String text, int openOffset, char openChar, char closeChar) {
        int depth = 1;
        int index = openOffset + 1;
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
            if (current == openChar) {
                depth++;
            } else if (current == closeChar) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

    private int findTopLevelStatementEnd(String text, int startOffset) {
        int depthParen = 0;
        int depthBracket = 0;
        int depthBrace = 0;
        int index = startOffset;

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
            switch (current) {
                case '(' -> depthParen++;
                case ')' -> depthParen = Math.max(0, depthParen - 1);
                case '[' -> depthBracket++;
                case ']' -> depthBracket = Math.max(0, depthBracket - 1);
                case '{' -> depthBrace++;
                case '}' -> depthBrace = Math.max(0, depthBrace - 1);
                case ';' -> {
                    if (depthParen == 0 && depthBracket == 0 && depthBrace == 0) {
                        return index + 1;
                    }
                }
                case '\n' -> {
                    if (depthParen == 0 && depthBracket == 0 && depthBrace == 0) {
                        return index;
                    }
                }
                default -> {
                }
            }
            index++;
        }
        return text.length();
    }

    private boolean startsWith(String text, int offset, String prefix) {
        return offset >= 0 && offset + prefix.length() <= text.length() && text.startsWith(prefix, offset);
    }

    private boolean looksLikeWord(String text, int offset, String word) {
        if (!startsWith(text, offset, word)) {
            return false;
        }
        boolean leftBoundary = offset == 0 || !Character.isJavaIdentifierPart(text.charAt(offset - 1));
        int end = offset + word.length();
        boolean rightBoundary = end >= text.length() || !Character.isJavaIdentifierPart(text.charAt(end));
        return leftBoundary && rightBoundary;
    }

    private boolean isWordStart(String text, int offset) {
        return offset < text.length()
                && Character.isJavaIdentifierStart(text.charAt(offset))
                && (offset == 0 || !Character.isJavaIdentifierPart(text.charAt(offset - 1)));
    }

    private int readWordEnd(String text, int offset) {
        int index = offset + 1;
        while (index < text.length() && Character.isJavaIdentifierPart(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private int skipWhitespaceAndComments(String text, int offset) {
        int index = offset;
        while (index < text.length()) {
            if (Character.isWhitespace(text.charAt(index))) {
                index++;
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
            break;
        }
        return index;
    }

    private int skipWhitespace(String text, int offset) {
        int index = offset;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private int skipQuotedString(String text, int offset) {
        char quote = text.charAt(offset);
        int index = offset + 1;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '\\' && index + 1 < text.length()) {
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

    private int skipLineComment(String text, int offset) {
        int index = offset + 2;
        while (index < text.length() && text.charAt(index) != '\n') {
            index++;
        }
        return index;
    }

    private int skipBlockComment(String text, int offset) {
        int index = offset + 2;
        while (index + 1 < text.length()) {
            if (text.charAt(index) == '*' && text.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return text.length();
    }

    private enum SegmentKind {
        IMPORT,
        METHOD
    }

    private record TopLevelSegment(
            SegmentKind kind,
            String text,
            int startOffset,
            int endOffset,
            int absoluteStartOffset,
            int absoluteEndOffset,
            String name,
            int nameOffset,
            List<ParameterInfo> parameters,
            String bodyText,
            int bodyStartOffset
    ) {
        static TopLevelSegment method(
                String text,
                int startOffset,
                int endOffset,
                int absoluteStartOffset,
                int absoluteEndOffset,
                String name,
                int nameOffset,
                List<ParameterInfo> parameters,
                String bodyText,
                int bodyStartOffset
        ) {
            return new TopLevelSegment(
                    SegmentKind.METHOD,
                    text,
                    startOffset,
                    endOffset,
                    absoluteStartOffset,
                    absoluteEndOffset,
                    name,
                    nameOffset,
                    List.copyOf(parameters),
                    bodyText,
                    bodyStartOffset
            );
        }

        TopLevelSegment(
                SegmentKind kind,
                String text,
                int startOffset,
                int endOffset,
                int absoluteStartOffset,
                int absoluteEndOffset
        ) {
            this(kind, text, startOffset, endOffset, absoluteStartOffset, absoluteEndOffset, "", absoluteStartOffset, List.of(), "", -1);
        }
    }

    private record ParameterInfo(String name, int offset) {
    }

    private record TokenInfo(IElementType type, String text, int startOffset, int endOffset) {
    }

    private record ForHeaderRewrite(String text) {
    }

    private record FieldCollection(Set<String> names, LinkedHashMap<String, Integer> firstOffsets) {
    }

    private static final class ScopeState {
        private final Set<String> globalFieldNames;
        private final Set<String> declaredNames = new LinkedHashSet<>();

        private ScopeState(Set<String> globalFieldNames) {
            this.globalFieldNames = globalFieldNames;
        }

        boolean isDeclared(String name) {
            return declaredNames.contains(name);
        }

        boolean isGlobalField(String name) {
            return globalFieldNames.contains(name);
        }

        void declare(String name) {
            declaredNames.add(name);
        }
    }

    private static final class TextSlice {
        private final String text;
        private final int absoluteStartOffset;

        private TextSlice(String text, int absoluteStartOffset) {
            this.text = text;
            this.absoluteStartOffset = absoluteStartOffset;
        }

        private int trimmedAbsoluteStart() {
            int shift = 0;
            while (shift < text.length() && Character.isWhitespace(text.charAt(shift))) {
                shift++;
            }
            return absoluteStartOffset + shift;
        }

        private TextSlice slice(int start, int end) {
            return new TextSlice(text.substring(start, end), absoluteStartOffset + start);
        }
    }

    private static final class MappedTextBuilder {
        private final StringBuilder text = new StringBuilder();
        private final List<Integer> javaToHost = new ArrayList<>();
        private final int[] hostToJava;
        private final int fallbackHostOffset;
        private final int endHostOffset;

        private MappedTextBuilder(int hostTextLength, int fallbackHostOffset, int endHostOffset) {
            this.hostToJava = new int[Math.max(1, hostTextLength + 1)];
            for (int index = 0; index < hostToJava.length; index++) {
                hostToJava[index] = -1;
            }
            this.fallbackHostOffset = fallbackHostOffset;
            this.endHostOffset = endHostOffset;
        }

        void appendChar(char value, int hostOffset) {
            text.append(value);
            javaToHost.add(normalizeHostOffset(hostOffset));
            rememberHostOffset(hostOffset, text.length() - 1);
        }

        void appendOriginal(String value, int hostStartOffset) {
            for (int index = 0; index < value.length(); index++) {
                appendChar(value.charAt(index), hostStartOffset + index);
            }
        }

        void appendSynthetic(String value, int hostOffset) {
            for (int index = 0; index < value.length(); index++) {
                appendChar(value.charAt(index), hostOffset);
            }
        }

        boolean endsWithNewline() {
            return text.length() > 0 && text.charAt(text.length() - 1) == '\n';
        }

        String text() {
            return text.toString();
        }

        int[] javaToHostOffsets() {
            int[] result = new int[javaToHost.size() + 1];
            int last = normalizeHostOffset(fallbackHostOffset);
            for (int index = 0; index < javaToHost.size(); index++) {
                last = Math.max(last, javaToHost.get(index));
                result[index] = last;
            }
            result[result.length - 1] = Math.max(last, normalizeHostOffset(endHostOffset));
            return result;
        }

        int[] hostToJavaOffsets() {
            int[] result = hostToJava.clone();
            int lastSeen = 0;
            for (int index = 0; index < result.length; index++) {
                if (result[index] >= 0) {
                    lastSeen = result[index];
                } else {
                    result[index] = lastSeen;
                }
            }
            return result;
        }

        private void rememberHostOffset(int hostOffset, int javaOffset) {
            int normalized = normalizeHostOffset(hostOffset);
            if (normalized < hostToJava.length && hostToJava[normalized] == -1) {
                hostToJava[normalized] = javaOffset;
            }
        }

        private int normalizeHostOffset(int hostOffset) {
            if (hostOffset < 0) {
                return fallbackHostOffset;
            }
            return Math.min(hostOffset, hostToJava.length - 1);
        }
    }
}
