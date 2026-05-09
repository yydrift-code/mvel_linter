package com.mvel.linter.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.mvel.linter.compiler.MvelCompileService;
import com.mvel.linter.compiler.MvelDiagnostic;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

public class MvelJavaCodeBlockInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (file instanceof MvelFile) {
                    checkCodeBlocks((MvelFile) file, holder);
                }
            }
        };
    }

    private void checkCodeBlocks(MvelFile file, ProblemsHolder holder) {
        MvelCompileService compileService = MvelCompileService.getInstance(file.getProject());
        for (MvelDiagnostic diagnostic : compileService.getCompileResult(file).diagnostics()) {
            if (diagnostic.sourceKind() != MvelDiagnostic.SourceKind.CODE_BLOCK) {
                continue;
            }
            registerDiagnostic(file, holder, diagnostic);
        }
    }

    private void registerDiagnostic(MvelFile file, ProblemsHolder holder, MvelDiagnostic diagnostic) {
        TextRange range = diagnostic.toTextRange(file.getTextLength());
        if (range.isEmpty()) {
            return;
        }

        holder.registerProblem(
                file,
                diagnostic.message(),
                diagnostic.severity() == MvelDiagnostic.Severity.WARNING
                        ? ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        : ProblemHighlightType.ERROR,
                range
        );
    }
}
