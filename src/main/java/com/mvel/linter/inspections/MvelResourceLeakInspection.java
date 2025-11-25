package com.mvel.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElementVisitor;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection to detect potential resource leaks in MVEL expressions.
 * Currently focuses on ensuring proper cleanup of evaluation contexts.
 */
public class MvelResourceLeakInspection extends LocalInspectionTool {
    
    // Note: We don't cache contexts to avoid potential memory leaks
    // Each inspection creates a fresh context that will be GC'd automatically
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull com.intellij.psi.PsiFile file) {
                if (file instanceof MvelFile) {
                    checkResourceLeaks((MvelFile) file, holder);
                }
            }
        };
    }

    private void checkResourceLeaks(MvelFile file, ProblemsHolder holder) {
        String text = file.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Check for potential resource leaks:
        // 1. Large string operations that might not be cleaned up
        // 2. Recursive expressions that could cause stack overflow
        // 3. Infinite loops that could consume resources
        
        // Check for potential infinite loops
        if (containsInfiniteLoopPattern(text)) {
            holder.registerProblem(
                file,
                "Potential infinite loop detected - may cause resource exhaustion",
                ProblemHighlightType.WARNING
            );
        }
        
        // Check for very large string concatenations
        if (hasExcessiveStringOperations(text)) {
            holder.registerProblem(
                file,
                "Excessive string operations detected - may cause memory issues",
                ProblemHighlightType.WEAK_WARNING
            );
        }
    }
    
    private boolean containsInfiniteLoopPattern(String text) {
        // Simple heuristic: check for while(true) or for(;;) patterns
        // without proper break conditions
        String lowerText = text.toLowerCase();
        return (lowerText.contains("while(true)") || lowerText.contains("for(;;)")) &&
               !lowerText.contains("break") && !lowerText.contains("return");
    }
    
    private boolean hasExcessiveStringOperations(String text) {
        // Count string concatenations
        int concatCount = 0;
        int index = 0;
        while ((index = text.indexOf("+", index)) != -1) {
            // Check if it's a string concatenation (not arithmetic)
            if (index > 0 && index < text.length() - 1) {
                char before = text.charAt(index - 1);
                char after = text.charAt(index + 1);
                if ((before == '"' || before == '\'') || 
                    (after == '"' || after == '\'')) {
                    concatCount++;
                }
            }
            index++;
        }
        // Warn if more than 10 string concatenations
        return concatCount > 10;
    }
}

