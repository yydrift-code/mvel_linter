package com.mvel.linter.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.mvel.linter.compiler.MvelDiagnostic;
import com.mvel.linter.compiler.MvelStatementSeparatorAnalyzer;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

public class MvelStatementSeparatorInspection extends LocalInspectionTool {
    private final MvelStatementSeparatorAnalyzer analyzer = new MvelStatementSeparatorAnalyzer();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (file instanceof MvelFile) {
                    checkStatementSeparators((MvelFile) file, holder);
                }
            }
        };
    }

    private void checkStatementSeparators(MvelFile file, ProblemsHolder holder) {
        for (MvelDiagnostic diagnostic : analyzer.analyze(file.getText())) {
            TextRange range = diagnostic.toTextRange(file.getTextLength());
            if (range.isEmpty()) {
                continue;
            }

            holder.registerProblem(
                    file,
                    diagnostic.message(),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    range
            );
        }
    }
}
