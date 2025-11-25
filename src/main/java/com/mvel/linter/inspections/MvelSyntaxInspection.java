package com.mvel.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElementVisitor;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;
import org.mvel2.MVEL;

import java.util.HashMap;
import java.util.Map;

public class MvelSyntaxInspection extends LocalInspectionTool {
    // Reuse empty context to avoid creating new HashMap for each inspection
    // This is safe because MVEL.eval() doesn't modify the context
    private static final Map<String, Object> EMPTY_CONTEXT = new HashMap<>();
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull com.intellij.psi.PsiFile file) {
                if (file instanceof MvelFile) {
                    checkMvelSyntax((MvelFile) file, holder);
                }
            }
        };
    }

    private void checkMvelSyntax(MvelFile file, ProblemsHolder holder) {
        String text = file.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        try {
            // Use MVEL's interpreter mode instead of compiler mode
            // This doesn't require Java's Compiler API which isn't available in IntelliJ plugins
            // Reuse EMPTY_CONTEXT to avoid creating new HashMap instances
            MVEL.eval(text, EMPTY_CONTEXT);
            
            // If evaluation succeeds, there are no syntax errors
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            // MVEL compiler classes not available - skip inspection
            // This happens when Java's Compiler API is not available
            return;
        } catch (org.mvel2.CompileException e) {
            // Syntax error detected
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = "MVEL syntax error: " + e.getClass().getSimpleName();
            }
            
            holder.registerProblem(
                file,
                message,
                ProblemHighlightType.ERROR
            );
        } catch (Exception e) {
            // Other errors - might be runtime errors, not syntax errors
            // Only report if it's clearly a syntax issue
            String message = e.getMessage();
            if (message != null && (message.contains("syntax") || message.contains("parse") || message.contains("unexpected"))) {
                holder.registerProblem(
                    file,
                    "MVEL syntax error: " + message,
                    ProblemHighlightType.ERROR
                );
            }
        }
    }
}

