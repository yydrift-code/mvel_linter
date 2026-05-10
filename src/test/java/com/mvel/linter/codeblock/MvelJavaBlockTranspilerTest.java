package com.mvel.linter.codeblock;

import com.intellij.openapi.util.TextRange;
import com.mvel.linter.compiler.MvelDiagnostic;
import org.junit.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class MvelJavaBlockTranspilerTest {
    private final MvelJavaBlockTranspiler transpiler = new MvelJavaBlockTranspiler();

    @Test
    public void hoistsImportsAndRewritesCoreMvelDeltas() {
        String host = "@code{\n"
                + "import java.time.LocalDate;\n"
                + "fields = ['a': 1, 'b': [1, 2]];\n"
                + "def log(message) {\n"
                + "    for (item : items) {\n"
                + "        values = [1, 2];\n"
                + "        if (message == empty) {\n"
                + "            return values;\n"
                + "        }\n"
                + "    }\n"
                + "    return fields;\n"
                + "}\n"
                + "}";

        MvelJavaCodeBlockModel model = transpiler.transpile(host, new TextRange(6, host.length() - 1));

        assertTrue(model.prefix().contains("import java.time.LocalDate;"));
        assertFalse(model.javaText().contains("import java.time.LocalDate;"));
        assertTrue(model.javaText().contains("Object log(Object message)"));
        assertTrue(model.javaText().contains("for (Object item : __mvelIter(items))"));
        assertTrue(model.javaText().contains("__mvelMap("));
        assertTrue(model.javaText().contains("__mvelList("));
        assertTrue(model.javaText().contains("__MVEL_EMPTY__"));
        assertTrue(model.prefix().contains("Object items;"));
        assertTrue(model.prefix().contains("Object fields;"));
    }

    @Test
    public void generatedSyntheticJavaCompilesForRepresentativeCodeBlock() {
        String host = "@code{\n"
                + "import java.time.LocalDate;\n"
                + "fields = ['a': 1, 'b': [1, 2]];\n"
                + "def log(message) {\n"
                + "    for (item : items) {\n"
                + "        values = [1, 2];\n"
                + "        if (message == empty) {\n"
                + "            return values;\n"
                + "        }\n"
                + "    }\n"
                + "    return fields;\n"
                + "}\n"
                + "}";

        MvelJavaCodeBlockModel model = transpiler.transpile(host, new TextRange(6, host.length() - 1));
        String javaSource = model.prefix() + model.javaText() + model.suffix();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue("System Java compiler is not available in the current test runtime", compiler != null);
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                null,
                diagnostics,
                List.of("-proc:none"),
                null,
                List.of(new StringJavaFileObject("MvelBlockSnippet", javaSource))
        );

        assertTrue(diagnostics.getDiagnostics().toString(), task.call());
    }

    @Test
    public void mapsGeneratedMethodBackToOriginalHostOffset() {
        String host = "@code{\n"
                + "def log(message) {\n"
                + "    return message;\n"
                + "}\n"
                + "}";

        MvelJavaCodeBlockModel model = transpiler.transpile(host, new TextRange(6, host.length() - 1));
        int javaOffset = model.javaText().indexOf("log");
        int hostOffset = model.mapJavaToHostOffset(javaOffset);

        assertEquals(host.indexOf("log"), hostOffset);
    }

    @Test
    public void malformedFunctionProducesExplicitDiagnostic() {
        String host = "@code{\ndef broken(\n}";

        MvelJavaCodeBlockModel model = transpiler.transpile(host, new TextRange(6, host.length() - 1));

        assertFalse(model.diagnostics().isEmpty());
        MvelDiagnostic diagnostic = model.diagnostics().get(0);
        assertTrue(diagnostic.message().contains("parameter list"));
        assertEquals(MvelDiagnostic.SourceKind.CODE_BLOCK, diagnostic.sourceKind());
    }

    private static final class StringJavaFileObject extends SimpleJavaFileObject {
        private final String source;

        private StringJavaFileObject(String className, String source) {
            super(URI.create("string:///" + className + ".java"), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
