package com.mvel.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.lexer.MvelTokenTypes;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

public class MvelBestPracticeInspection extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull com.intellij.psi.PsiFile file) {
                if (file instanceof MvelFile) {
                    checkBestPractices((MvelFile) file, holder);
                }
            }
        };
    }

    private void checkBestPractices(MvelFile file, ProblemsHolder holder) {
        String text = file.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Check for common best practices
        
        // 1. Check for null-safe navigation (MVEL supports ?. operator)
        if (text.contains(".") && !text.contains("?.")) {
            // Warn about potential null pointer exceptions
            // This is a simplified check - in a real implementation, 
            // you'd parse the AST to find property accesses
        }

        // 2. Check for proper use of 'empty' literal vs null
        if (text.contains("== null") || text.contains("== nil")) {
            // Suggest using 'empty' for value emptiness checks
            int index = text.indexOf("== null");
            if (index == -1) {
                index = text.indexOf("== nil");
            }
            if (index != -1) {
                PsiElement element = file.findElementAt(index);
                if (element != null) {
                    holder.registerProblem(
                        element,
                        "Consider using 'empty' literal for value emptiness checks instead of null/nil",
                        ProblemHighlightType.WEAK_WARNING
                    );
                }
            }
        }

        // 3. Check for proper string literal usage
        // MVEL supports both single and double quotes, but consistency is recommended
        boolean hasSingleQuotes = text.contains("'");
        boolean hasDoubleQuotes = text.contains("\"");
        if (hasSingleQuotes && hasDoubleQuotes) {
            // Suggest consistency
            holder.registerProblem(
                file,
                "Consider using consistent string literal delimiters (either single or double quotes)",
                ProblemHighlightType.INFORMATION
            );
        }

        // 4. Check for missing semicolons in multi-statement expressions
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && 
                !line.endsWith(";") && 
                !line.endsWith("{") && 
                !line.endsWith("}") &&
                !line.startsWith("//") &&
                !line.startsWith("/*")) {
                // This is a simplified check - a real implementation would parse the AST
            }
        }
    }
}

