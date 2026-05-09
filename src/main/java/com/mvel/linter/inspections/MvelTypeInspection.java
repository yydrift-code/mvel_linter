package com.mvel.linter.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.mvel.linter.compiler.MvelCompileService;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

public class MvelTypeInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull com.intellij.psi.PsiFile file) {
                if (file instanceof MvelFile) {
                    checkMvelTypes((MvelFile) file);
                }
            }
        };
    }

    private void checkMvelTypes(MvelFile file) {
        // Keep the inspection compile-only and side-effect free.
        // Rich type diagnostics need a platform-specific symbol model and are intentionally deferred.
        MvelCompileService.getInstance(file.getProject()).getCompileResult(file);
    }
}
