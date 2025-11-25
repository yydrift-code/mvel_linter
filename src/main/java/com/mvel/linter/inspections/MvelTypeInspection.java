package com.mvel.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;
import org.mvel2.MVEL;

import java.util.HashMap;
import java.util.Map;

public class MvelTypeInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (file instanceof MvelFile) {
                    checkMvelTypes((MvelFile) file, holder);
                }
            }
        };
    }

    private void checkMvelTypes(MvelFile file, ProblemsHolder holder) {
        String text = file.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        try {
            // Use MVEL's interpreter mode instead of compiler mode
            // Create a minimal test context
            Map<String, Object> testContext = new HashMap<>();
            
            // Try to evaluate with the test context
            // This will catch type errors if the expression references variables
            try {
                MVEL.eval(text, testContext);
            } catch (org.mvel2.PropertyAccessException e) {
                // Type error detected - variable or property not found
                String message = e.getMessage();
                if (message != null && (message.contains("could not resolve") || 
                                       message.contains("not found") ||
                                       message.contains("cannot be resolved"))) {
                    holder.registerProblem(
                        file,
                        "MVEL type error: " + message,
                        ProblemHighlightType.WARNING
                    );
                }
            } catch (org.mvel2.CompileException e) {
                // Syntax errors are handled by MvelSyntaxInspection
                // Skip compilation errors here
            } catch (RuntimeException e) {
                // Check if it's a resolution error
                String message = e.getMessage();
                if (message != null && (message.contains("could not resolve") || 
                                       message.contains("not found") ||
                                       message.contains("cannot be resolved"))) {
                    holder.registerProblem(
                        file,
                        "MVEL type error: " + message,
                        ProblemHighlightType.WARNING
                    );
                }
            }
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            // MVEL compiler classes not available - skip inspection
            // This happens when Java's Compiler API is not available
            return;
        } catch (Exception e) {
            // Other errors - might be runtime errors, not type errors
            // Only report if it's clearly a type/resolution issue
            String message = e.getMessage();
            if (message != null && (message.contains("could not resolve") || 
                                   message.contains("not found") ||
                                   message.contains("cannot be resolved"))) {
                holder.registerProblem(
                    file,
                    "MVEL type error: " + message,
                    ProblemHighlightType.WARNING
                );
            }
        }
    }
}

