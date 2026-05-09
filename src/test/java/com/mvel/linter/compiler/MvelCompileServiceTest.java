package com.mvel.linter.compiler;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class MvelCompileServiceTest {
    private final MvelCompileService compileService = new MvelCompileService();

    @Test
    public void plainScriptMissingSemicolonGetsPreciseHint() {
        String text = "int x = 10\nString name = \"demo\";\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals("Possible missing ';'", diagnostic.message());
        assertTrue(diagnostic.startOffset() >= text.indexOf("x"));
        assertTrue(diagnostic.startOffset() < text.indexOf('\n'));
        assertEquals(MvelDiagnostic.SourceKind.SCRIPT, diagnostic.sourceKind());
    }

    @Test
    public void plainScriptCompileErrorIsNotPinnedToFileStart() {
        String text = "value = 1;\nfoo = ;\nother = 2;";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertTrue(diagnostic.startOffset() >= text.indexOf("foo"));
        assertTrue(diagnostic.startOffset() < text.indexOf("other"));
        assertEquals(MvelDiagnostic.SourceKind.SCRIPT, diagnostic.sourceKind());
    }

    @Test
    public void realWorldPlainScriptPassesCompileValidation() throws IOException {
        assertTrue(compileService.compileText(readProjectFile("cont.mvel")).diagnostics().isEmpty());
    }

    @Test
    public void realWorldTemplatePassesCompileValidation() throws IOException {
        assertTrue(compileService.compileText(readProjectFile("ghor.mvel")).diagnostics().isEmpty());
    }

    @Test
    public void invalidCodeBlockReportsErrorInsideBlock() {
        String text = "@code{\n    int x = 10\n    String name = \"demo\";\n}\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals("Possible missing ';'", diagnostic.message());
        assertEquals(MvelDiagnostic.SourceKind.CODE_BLOCK, diagnostic.sourceKind());
        assertTrue(diagnostic.startOffset() > text.indexOf("@code{"));
    }

    @Test
    public void semicolonHintDoesNotMaskOtherErrorsInLargeCodeBlock() {
        String text = "@code{\n"
                + "    positionId = foo\n"
                + "    if (x) { return y; }\n"
                + "    bar = )\n"
                + "}\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertNotEquals("Possible missing ';'", diagnostic.message());
        assertEquals(MvelDiagnostic.SourceKind.CODE_BLOCK, diagnostic.sourceKind());
        assertTrue(diagnostic.startOffset() >= text.indexOf("bar"));
    }

    @Test
    public void unclosedTemplateBlockIsReportedAtOpeningTag() {
        String text = "@if{x > 0}hello";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertTrue(diagnostic.message().contains("expected @end{}"));
        assertEquals(text.indexOf("@if{"), diagnostic.startOffset());
        assertEquals(MvelDiagnostic.SourceKind.TEMPLATE, diagnostic.sourceKind());
    }

    @Test
    public void unclosedCodeBlockIsReported() {
        String text = "@code{\n    int x = 10;\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertTrue(diagnostic.message().contains("Unclosed @code{"));
        assertEquals(text.indexOf("@code{"), diagnostic.startOffset());
        assertEquals(MvelDiagnostic.SourceKind.TEMPLATE, diagnostic.sourceKind());
    }

    @Test
    public void orbExpressionErrorIsLocalizedInsideOrb() {
        String text = "prefix @{foo = } suffix";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals(MvelDiagnostic.SourceKind.ORB, diagnostic.sourceKind());
        assertTrue(diagnostic.startOffset() > text.indexOf("@{"));
        assertTrue(diagnostic.startOffset() < text.indexOf("suffix"));
    }

    @Test
    public void foreachHeaderErrorIsLocalizedInsideHeader() {
        String text = "@foreach{item: foo = }@{item}@end{}";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals(MvelDiagnostic.SourceKind.FOREACH, diagnostic.sourceKind());
        assertTrue(diagnostic.startOffset() > text.indexOf("@foreach{"));
        assertTrue(diagnostic.startOffset() < text.indexOf("@{item}"));
    }

    private MvelDiagnostic singleDiagnostic(String text) {
        List<MvelDiagnostic> diagnostics = compileService.compileText(text).diagnostics();
        assertFalse(diagnostics.isEmpty());
        assertEquals(1, diagnostics.size());
        return diagnostics.get(0);
    }

    private String readProjectFile(String fileName) throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        return Files.readString(projectRoot.resolve(fileName));
    }
}
