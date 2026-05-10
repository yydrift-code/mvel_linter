package com.mvel.linter.compiler;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.templates.TemplateCompiler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class MvelCompileService {
    private static final Key<CachedValue<MvelCompileResult>> COMPILE_RESULT_KEY =
            Key.create("com.mvel.linter.compiler.MvelCompileResult");

    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+[\\w.*$]+\\s*$");
    private static final Pattern TYPE_DECLARATION_PATTERN = Pattern.compile(
            "^(?:final\\s+)?(?:(?:byte|short|int|long|float|double|boolean|char|String|BigDecimal|BigInteger|"
                    + "[A-Z_$][\\w$]*|[a-z_$][\\w$]*)(?:\\.[A-Z_$][\\w$]*)*(?:<[^>]+>)?(?:\\[\\])?)\\s+"
                    + "[A-Za-z_$][\\w$]*\\b.*$"
    );

    private static final Set<String> CONTROL_FLOW_PREFIXES = Set.of(
            "if", "for", "foreach", "while", "do", "else", "def", "function", "switch", "try", "catch", "finally"
    );

    private static final Set<String> STRUCTURAL_TEMPLATE_TOKENS = Set.of(
            "expected @end{}", "unexpected @end{}", "unexpected @else{}", "unclosed @if{}", "unclosed @foreach{}"
    );

    public static MvelCompileService getInstance(Project project) {
        return project.getService(MvelCompileService.class);
    }

    public @NotNull MvelCompileResult getCompileResult(@NotNull PsiFile file) {
        return CachedValuesManager.getManager(file.getProject()).getCachedValue(file, COMPILE_RESULT_KEY, () ->
                CachedValueProvider.Result.create(compileText(file.getText()), file), false);
    }

    public @NotNull MvelCompileResult compileText(String text) {
        if (text == null || text.isBlank()) {
            return MvelCompileResult.empty(false);
        }

        TemplateCodeBlockScanner.TemplateScan scan = TemplateCodeBlockScanner.scan(text);
        List<MvelDiagnostic> diagnostics = new ArrayList<>(scan.diagnostics());

        if (scan.fragments().isEmpty()) {
            if (!diagnostics.isEmpty()) {
                return new MvelCompileResult(true, deduplicate(diagnostics));
            }

            MvelDiagnostic scriptDiagnostic = compileFragment(
                    text,
                    0,
                    text.length(),
                    text,
                    MvelDiagnostic.SourceKind.SCRIPT
            );
            if (scriptDiagnostic != null) {
                diagnostics.add(scriptDiagnostic);
            }
            return new MvelCompileResult(false, deduplicate(diagnostics));
        }

        for (TemplateCodeBlockScanner.TemplateFragment fragment : scan.fragments()) {
            if (fragment.kind() == MvelDiagnostic.SourceKind.COMMENT) {
                continue;
            }

            String content = fragment.content(text);
            if (content.isBlank()) {
                continue;
            }

            MvelDiagnostic fragmentDiagnostic = compileFragment(
                    text,
                    fragment.contentStartOffset(),
                    fragment.contentEndOffset(),
                    content,
                    fragment.kind()
            );
            if (fragmentDiagnostic != null) {
                diagnostics.add(fragmentDiagnostic);
            }
        }

        MvelDiagnostic templateDiagnostic = compileTemplateStructure(text);
        if (templateDiagnostic != null && !shouldSuppressTemplateDiagnostic(templateDiagnostic, scan.fragments(), diagnostics)) {
            diagnostics.add(templateDiagnostic);
        }

        return new MvelCompileResult(true, deduplicate(diagnostics));
    }

    private MvelDiagnostic compileFragment(
            String fullText,
            int absoluteStartOffset,
            int absoluteEndOffset,
            String fragmentText,
            MvelDiagnostic.SourceKind sourceKind
    ) {
        try {
            MVEL.compileExpression(fragmentText);
            return null;
        } catch (CompileException e) {
            MvelDiagnostic semicolonHint = buildSemicolonHint(
                    fullText,
                    absoluteStartOffset,
                    absoluteEndOffset,
                    fragmentText,
                    sourceKind,
                    sanitizeMessage(e.getMessage(), "MVEL compile error"),
                    e
            );
            if (semicolonHint != null) {
                return semicolonHint;
            }
            return buildFragmentDiagnostic(fullText, absoluteStartOffset, absoluteEndOffset, fragmentText, sourceKind, e);
        } catch (RuntimeException e) {
            return buildFallbackDiagnostic(
                    absoluteStartOffset,
                    absoluteEndOffset,
                    sourceKind,
                    "MVEL compile error: " + sanitizeMessage(e.getMessage(), "Unknown compile failure")
            );
        }
    }

    private MvelDiagnostic compileTemplateStructure(String text) {
        try {
            TemplateCompiler.compileTemplate(text);
            return null;
        } catch (CompileException e) {
            return buildWholeFileDiagnostic(text, e, MvelDiagnostic.SourceKind.TEMPLATE);
        } catch (RuntimeException e) {
            return buildFallbackDiagnostic(
                    0,
                    Math.min(text.length(), 1),
                    MvelDiagnostic.SourceKind.TEMPLATE,
                    "Template compile error: " + sanitizeMessage(e.getMessage(), "Unknown template failure")
            );
        }
    }

    private MvelDiagnostic buildFragmentDiagnostic(
            String fullText,
            int absoluteStartOffset,
            int absoluteEndOffset,
            String fragmentText,
            MvelDiagnostic.SourceKind sourceKind,
            CompileException exception
    ) {
        int relativeOffset = resolveRelativeOffset(fragmentText, exception);
        int absoluteOffset = clamp(absoluteStartOffset + relativeOffset, absoluteStartOffset, Math.max(absoluteStartOffset, absoluteEndOffset - 1));
        int anchorOffset = adjustToVisibleToken(fullText, absoluteOffset, absoluteStartOffset, absoluteEndOffset);
        int rangeStart = expandStart(fullText, anchorOffset, absoluteStartOffset);
        int rangeEnd = expandEnd(fullText, anchorOffset, absoluteEndOffset);
        return new MvelDiagnostic(
                sanitizeMessage(exception.getMessage(), "MVEL compile error"),
                MvelDiagnostic.Severity.ERROR,
                sourceKind,
                rangeStart,
                rangeEnd
        );
    }

    private MvelDiagnostic buildWholeFileDiagnostic(String text, CompileException exception, MvelDiagnostic.SourceKind sourceKind) {
        int absoluteOffset = clamp(resolveRelativeOffset(text, exception), 0, Math.max(0, text.length() - 1));
        int anchorOffset = adjustToVisibleToken(text, absoluteOffset, 0, text.length());
        int rangeStart = expandStart(text, anchorOffset, 0);
        int rangeEnd = expandEnd(text, anchorOffset, text.length());
        return new MvelDiagnostic(
                sanitizeMessage(exception.getMessage(), "Template compile error"),
                MvelDiagnostic.Severity.ERROR,
                sourceKind,
                rangeStart,
                rangeEnd
        );
    }

    private MvelDiagnostic buildFallbackDiagnostic(
            int startOffset,
            int endOffset,
            MvelDiagnostic.SourceKind sourceKind,
            String message
    ) {
        return new MvelDiagnostic(message, MvelDiagnostic.Severity.ERROR, sourceKind, startOffset, Math.max(startOffset + 1, endOffset));
    }

    private MvelDiagnostic buildSemicolonHint(
            String fullText,
            int absoluteStartOffset,
            int absoluteEndOffset,
            String fragmentText,
            MvelDiagnostic.SourceKind sourceKind,
            String sanitizedMessage,
            CompileException exception
    ) {
        if (!looksLikeSemicolonRelatedFailure(sanitizedMessage, exception)) {
            return null;
        }

        List<LineInfo> lines = collectSignificantLines(fragmentText, absoluteStartOffset);
        List<SemicolonCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < lines.size() - 1; index++) {
            LineInfo previous = lines.get(index);
            LineInfo next = lines.get(index + 1);

            if (!isSemicolonCandidate(previous.text()) || !startsNewStatement(next.text())) {
                continue;
            }

            candidates.add(new SemicolonCandidate(previous, next));
        }

        int relativeErrorOffset = resolveRelativeOffset(fragmentText, exception);
        candidates.sort((left, right) -> compareCandidates(left, right, relativeErrorOffset, absoluteStartOffset));

        for (SemicolonCandidate candidate : candidates) {
            if (!semicolonInsertionRepairsCompile(fragmentText, candidate.insertRelativeOffset(absoluteStartOffset))) {
                continue;
            }

            int highlightOffset = clamp(
                    candidate.previous().lastNonWhitespaceOffset(),
                    absoluteStartOffset,
                    Math.max(absoluteStartOffset, absoluteEndOffset - 1)
            );
            int rangeStart = expandStart(fullText, highlightOffset, candidate.previous().absoluteStartOffset());
            int rangeEnd = expandEnd(fullText, highlightOffset, absoluteEndOffset);

            return new MvelDiagnostic(
                    "Possible missing ';'",
                    MvelDiagnostic.Severity.ERROR,
                    sourceKind,
                    rangeStart,
                    rangeEnd
            );
        }

        return null;
    }

    private List<LineInfo> collectSignificantLines(String text, int absoluteStartOffset) {
        List<LineInfo> lines = new ArrayList<>();
        int lineStart = 0;

        for (int index = 0; index <= text.length(); index++) {
            if (index < text.length() && text.charAt(index) != '\n') {
                continue;
            }

            String rawLine = text.substring(lineStart, index);
            String trimmed = rawLine.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                int lastNonWhitespace = index - 1;
                while (lastNonWhitespace >= lineStart && Character.isWhitespace(text.charAt(lastNonWhitespace))) {
                    lastNonWhitespace--;
                }

                if (lastNonWhitespace >= lineStart) {
                    lines.add(new LineInfo(
                            trimmed,
                            absoluteStartOffset + lineStart,
                            absoluteStartOffset + lastNonWhitespace
                    ));
                }
            }

            lineStart = index + 1;
        }

        return lines;
    }

    private boolean isSemicolonCandidate(String line) {
        if (line.endsWith(";") || line.endsWith("{") || line.endsWith("}") || line.startsWith("@")) {
            return false;
        }

        String firstToken = firstToken(line);
        if (CONTROL_FLOW_PREFIXES.contains(firstToken)) {
            return false;
        }

        if (IMPORT_PATTERN.matcher(line).matches() || TYPE_DECLARATION_PATTERN.matcher(line).matches()) {
            return true;
        }

        return looksLikeAssignment(line);
    }

    private boolean startsNewStatement(String line) {
        if (line.isEmpty() || line.startsWith("@")) {
            return false;
        }

        String firstToken = firstToken(line);
        if (CONTROL_FLOW_PREFIXES.contains(firstToken) || "import".equals(firstToken)) {
            return true;
        }

        return TYPE_DECLARATION_PATTERN.matcher(line).matches() || looksLikeAssignment(line);
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

    private int compareCandidates(
            SemicolonCandidate left,
            SemicolonCandidate right,
            int relativeErrorOffset,
            int absoluteStartOffset
    ) {
        if (relativeErrorOffset > 0) {
            int leftDistance = left.distanceTo(relativeErrorOffset, absoluteStartOffset);
            int rightDistance = right.distanceTo(relativeErrorOffset, absoluteStartOffset);
            if (leftDistance != rightDistance) {
                return Integer.compare(leftDistance, rightDistance);
            }
        }

        return Integer.compare(
                right.previous().absoluteStartOffset(),
                left.previous().absoluteStartOffset()
        );
    }

    private boolean semicolonInsertionRepairsCompile(String fragmentText, int insertRelativeOffset) {
        if (insertRelativeOffset <= 0 || insertRelativeOffset > fragmentText.length()) {
            return false;
        }

        String patchedFragment = fragmentText.substring(0, insertRelativeOffset) + ';' + fragmentText.substring(insertRelativeOffset);
        try {
            MVEL.compileExpression(patchedFragment);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean looksLikeSemicolonRelatedFailure(String sanitizedMessage, CompileException exception) {
        int cursor = exception.getCursor();
        if (cursor <= 0) {
            return true;
        }

        String lowerCaseMessage = sanitizedMessage.toLowerCase(Locale.ROOT);
        return lowerCaseMessage.contains("unexpected token")
                || lowerCaseMessage.contains("unknown class or illegal statement")
                || lowerCaseMessage.contains("unterminated")
                || lowerCaseMessage.contains("incomplete statement");
    }

    private boolean shouldSuppressTemplateDiagnostic(
            MvelDiagnostic templateDiagnostic,
            List<TemplateCodeBlockScanner.TemplateFragment> fragments,
            List<MvelDiagnostic> diagnostics
    ) {
        if (templateDiagnostic == null) {
            return true;
        }

        if (diagnostics.stream().anyMatch(existing -> sameDiagnostic(existing, templateDiagnostic))) {
            return true;
        }

        String lowerCaseMessage = templateDiagnostic.message().toLowerCase(Locale.ROOT);
        boolean structural = STRUCTURAL_TEMPLATE_TOKENS.stream().anyMatch(token -> lowerCaseMessage.contains(token.toLowerCase(Locale.ROOT)))
                || lowerCaseMessage.contains("unbalanced braces");

        TemplateCodeBlockScanner.TemplateFragment owner = findOwner(fragments, templateDiagnostic.startOffset());
        if (owner != null && owner.kind() == MvelDiagnostic.SourceKind.CODE_BLOCK) {
            return true;
        }

        if (!structural && owner != null) {
            return diagnostics.stream().anyMatch(existing ->
                    existing.sourceKind() == owner.kind()
                            && overlaps(existing, owner.contentStartOffset(), owner.contentEndOffset()));
        }

        return structural && diagnostics.stream().anyMatch(existing ->
                existing.sourceKind() == MvelDiagnostic.SourceKind.TEMPLATE);
    }

    private TemplateCodeBlockScanner.TemplateFragment findOwner(
            List<TemplateCodeBlockScanner.TemplateFragment> fragments,
            int offset
    ) {
        for (TemplateCodeBlockScanner.TemplateFragment fragment : fragments) {
            if (fragment.containsOffset(offset)) {
                return fragment;
            }
        }
        return null;
    }

    private boolean overlaps(MvelDiagnostic diagnostic, int startOffset, int endOffset) {
        return diagnostic.startOffset() < endOffset && diagnostic.endOffset() > startOffset;
    }

    private boolean sameDiagnostic(MvelDiagnostic left, MvelDiagnostic right) {
        return left.startOffset() == right.startOffset()
                && left.endOffset() == right.endOffset()
                && Objects.equals(left.message(), right.message());
    }

    private List<MvelDiagnostic> deduplicate(List<MvelDiagnostic> diagnostics) {
        return List.copyOf(new LinkedHashSet<>(diagnostics));
    }

    private int resolveRelativeOffset(String text, CompileException exception) {
        int cursor = exception.getCursor();
        if (cursor >= 0 && cursor < text.length()) {
            return cursor;
        }

        int lineNumber = exception.getLineNumber();
        int column = exception.getColumn();
        if (lineNumber <= 0) {
            return 0;
        }

        int lineStart = 0;
        int currentLine = 1;
        while (currentLine < lineNumber && lineStart < text.length()) {
            int nextLineBreak = text.indexOf('\n', lineStart);
            if (nextLineBreak < 0) {
                return Math.max(0, text.length() - 1);
            }
            lineStart = nextLineBreak + 1;
            currentLine++;
        }

        int normalizedColumn = Math.max(0, column - 1);
        return clamp(lineStart + normalizedColumn, 0, Math.max(0, text.length() - 1));
    }

    private int adjustToVisibleToken(String text, int offset, int minOffset, int maxOffset) {
        if (text.isEmpty()) {
            return 0;
        }

        int clampedMax = Math.max(minOffset + 1, maxOffset);
        int anchor = clamp(offset, minOffset, clampedMax - 1);
        if (!Character.isWhitespace(text.charAt(anchor))) {
            return anchor;
        }

        for (int index = anchor; index >= minOffset; index--) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }

        for (int index = anchor + 1; index < clampedMax; index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }

        return anchor;
    }

    private int expandStart(String text, int offset, int minOffset) {
        int start = offset;
        while (start > minOffset && isIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    private int expandEnd(String text, int offset, int maxOffset) {
        int end = Math.min(offset + 1, text.length());
        int upperBound = Math.min(maxOffset, text.length());
        while (end < upperBound && isIdentifierPart(text.charAt(end))) {
            end++;
        }
        return Math.max(offset + 1, end);
    }

    private boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '$';
    }

    private int clamp(int value, int minValue, int maxValue) {
        if (maxValue < minValue) {
            return minValue;
        }
        return Math.max(minValue, Math.min(value, maxValue));
    }

    private String sanitizeMessage(String rawMessage, String defaultMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return defaultMessage;
        }

        String message = rawMessage;
        int nearIndex = message.indexOf("\n[Near");
        if (nearIndex >= 0) {
            message = message.substring(0, nearIndex);
        }

        message = message.replace('\n', ' ').trim();
        while (message.startsWith("[Error:")) {
            message = message.substring("[Error:".length()).trim();
        }
        while (message.startsWith("[")) {
            message = message.substring(1).trim();
        }
        while (message.endsWith("]")) {
            message = message.substring(0, message.length() - 1).trim();
        }

        return message.isBlank() ? defaultMessage : message;
    }

    private String firstToken(String line) {
        int separator = line.indexOf(' ');
        return separator < 0 ? line : line.substring(0, separator);
    }

    private record LineInfo(String text, int absoluteStartOffset, int lastNonWhitespaceOffset) {
    }

    private record SemicolonCandidate(LineInfo previous, LineInfo next) {
        int insertRelativeOffset(int absoluteStartOffset) {
            return previous.lastNonWhitespaceOffset() - absoluteStartOffset + 1;
        }

        int distanceTo(int relativeErrorOffset, int absoluteStartOffset) {
            int candidateStart = previous.absoluteStartOffset() - absoluteStartOffset;
            int candidateEnd = next.lastNonWhitespaceOffset() - absoluteStartOffset;
            if (relativeErrorOffset < candidateStart) {
                return candidateStart - relativeErrorOffset;
            }
            if (relativeErrorOffset > candidateEnd) {
                return relativeErrorOffset - candidateEnd;
            }
            return 0;
        }
    }
}
