package com.mvel.linter.compiler;

import java.util.List;

public record MvelCompileResult(boolean templateFile, List<MvelDiagnostic> diagnostics) {
    public static MvelCompileResult empty(boolean templateFile) {
        return new MvelCompileResult(templateFile, List.of());
    }
}
