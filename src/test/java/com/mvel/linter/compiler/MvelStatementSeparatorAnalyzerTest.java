package com.mvel.linter.compiler;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MvelStatementSeparatorAnalyzerTest {
    private final MvelStatementSeparatorAnalyzer analyzer = new MvelStatementSeparatorAnalyzer();

    @Test
    public void assignmentBeforeReturnIsReportedInCodeBlock() {
        String text = "@code{\n"
                + "    value = 1\n"
                + "    return value;\n"
                + "}\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals("Missing ';' before next statement", diagnostic.message());
        assertEquals(MvelDiagnostic.SourceKind.CODE_BLOCK, diagnostic.sourceKind());
        assertEquals(MvelDiagnostic.Severity.WARNING, diagnostic.severity());
    }

    @Test
    public void multilineMapAssignmentIsReportedOnlyAtStatementBoundary() {
        String text = "@code{\n"
                + "    data = [\n"
                + "        \"a\": 1,\n"
                + "        \"b\": 2\n"
                + "    ]\n"
                + "    return data;\n"
                + "}\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals("Missing ';' before next statement", diagnostic.message());
        assertEquals(MvelDiagnostic.SourceKind.CODE_BLOCK, diagnostic.sourceKind());
        assertTrue(diagnostic.startOffset() >= text.indexOf("]"));
        assertTrue(diagnostic.startOffset() < text.indexOf("return"));
    }

    @Test
    public void methodChainReportsOnlyFinalBoundary() {
        String text = "@code{\n"
                + "    query = ElasticQuery\n"
                + "        .builder()\n"
                + "        .data(\"x\", \"LABEL\")\n"
                + "    next = 1;\n"
                + "}\n";

        MvelDiagnostic diagnostic = singleDiagnostic(text);

        assertEquals("Missing ';' before next statement", diagnostic.message());
        assertTrue(diagnostic.startOffset() >= text.indexOf(".data"));
        assertTrue(diagnostic.startOffset() < text.indexOf("next = 1"));
    }

    @Test
    public void terminatedStatementsDoNotProduceDiagnostics() {
        String text = "@code{\n"
                + "    value = 1;\n"
                + "    return value;\n"
                + "}\n";

        assertTrue(analyzer.analyze(text).isEmpty());
    }

    private MvelDiagnostic singleDiagnostic(String text) {
        List<MvelDiagnostic> diagnostics = analyzer.analyze(text);
        assertEquals(1, diagnostics.size());
        return diagnostics.get(0);
    }
}
