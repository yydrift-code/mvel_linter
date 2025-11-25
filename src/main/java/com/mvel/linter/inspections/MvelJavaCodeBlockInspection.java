package com.mvel.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.mvel.linter.lexer.MvelTokenTypes;
import com.mvel.linter.parser.MvelTypes;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MvelJavaCodeBlockInspection extends LocalInspectionTool {
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (file instanceof MvelFile) {
                    checkJavaCodeBlocks((MvelFile) file, holder);
                }
            }
        };
    }
    
    private void checkJavaCodeBlocks(MvelFile file, ProblemsHolder holder) {
        String text = file.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Find all @code{} blocks using manual parsing to handle nested braces
        findAndValidateCodeBlocks(file, text, holder);
        
        // Also check for @code{} blocks in PSI tree
        checkPsiTreeForCodeBlocks(file, holder);
    }
    
    private void findAndValidateCodeBlocks(MvelFile file, String text, ProblemsHolder holder) {
        int index = 0;
        while (index < text.length()) {
            int codeStart = text.indexOf("@code{", index);
            if (codeStart == -1) {
                break;
            }
            
            // Find the matching closing brace
            int braceStart = codeStart + 6; // After "@code{"
            int braceDepth = 1;
            int contentStart = braceStart;
            int contentEnd = -1;
            
            for (int i = braceStart; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '{') {
                    braceDepth++;
                } else if (ch == '}') {
                    braceDepth--;
                    if (braceDepth == 0) {
                        contentEnd = i;
                        break;
                    }
                }
            }
            
            if (contentEnd == -1) {
                // Unmatched braces - report error
                PsiElement element = file.findElementAt(codeStart);
                if (element != null) {
                    holder.registerProblem(
                        element,
                        "Unmatched braces in @code{} block",
                        ProblemHighlightType.ERROR
                    );
                }
                break;
            }
            
            String codeContent = text.substring(contentStart, contentEnd);
            if (codeContent != null && !codeContent.trim().isEmpty()) {
                validateJavaCode(file, codeContent, contentStart, contentEnd, holder);
            } else {
                // Even if empty, we should validate to catch structural issues
                PsiElement element = file.findElementAt(codeStart);
                if (element != null) {
                    holder.registerProblem(
                        element,
                        "Empty @code{} block",
                        ProblemHighlightType.WEAK_WARNING
                    );
                }
            }
            
            index = contentEnd + 1;
        }
    }
    
    private void checkPsiTreeForCodeBlocks(PsiFile file, ProblemsHolder holder) {
        // Recursively traverse the PSI tree to find template blocks
        checkElementForCodeBlock(file, holder);
    }
    
    private void checkElementForCodeBlock(PsiElement element, ProblemsHolder holder) {
        if (element == null) {
            return;
        }
        
        // Check if this is a template block
        if (element.getNode() != null && 
            element.getNode().getElementType() == MvelTypes.TEMPLATE_BLOCK) {
            // Check if it contains TEMPLATE_CODE token
            PsiElement[] children = element.getChildren();
            boolean hasCodeToken = false;
            for (PsiElement child : children) {
                if (child.getNode() != null && 
                    child.getNode().getElementType() == MvelTokenTypes.TEMPLATE_CODE) {
                    hasCodeToken = true;
                    break;
                }
            }
            
            if (hasCodeToken) {
                validateCodeBlockContent(element, holder);
            }
        }
        
        // Recursively check children
        for (PsiElement child : element.getChildren()) {
            checkElementForCodeBlock(child, holder);
        }
    }
    
    private void validateCodeBlockContent(PsiElement templateBlock, ProblemsHolder holder) {
        String text = templateBlock.getText();
        if (text == null) {
            return;
        }
        
        // Find @code{ in the text
        int codeStart = text.indexOf("@code{");
        if (codeStart == -1) {
            return;
        }
        
        // Extract content between @code{ and matching }
        int braceStart = codeStart + 6; // After "@code{"
        int braceDepth = 1;
        int contentStart = braceStart;
        int contentEnd = -1;
        
        for (int i = braceStart; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                braceDepth++;
            } else if (ch == '}') {
                braceDepth--;
                if (braceDepth == 0) {
                    contentEnd = i;
                    break;
                }
            }
        }
        
        if (contentEnd == -1 || contentEnd <= contentStart) {
            // Unmatched braces
            PsiElement element = templateBlock.findElementAt(templateBlock.getTextRange().getStartOffset() + codeStart);
            if (element != null) {
                holder.registerProblem(
                    element,
                    "Unmatched braces in @code{} block",
                    ProblemHighlightType.ERROR
                );
            }
            return;
        }
        
        String codeContent = text.substring(contentStart, contentEnd);
        int fileStartOffset = templateBlock.getTextRange().getStartOffset() + contentStart;
        int fileEndOffset = templateBlock.getTextRange().getStartOffset() + contentEnd;
        
        validateJavaCode((MvelFile) templateBlock.getContainingFile(), 
                        codeContent, fileStartOffset, fileEndOffset, holder);
    }
    
    private void validateJavaCode(MvelFile file, String javaCode, int startOffset, int endOffset, ProblemsHolder holder) {
        if (javaCode == null || javaCode.trim().isEmpty()) {
            return;
        }
        
        // Basic Java syntax validation
        List<JavaSyntaxError> errors = validateJavaSyntax(javaCode);
        
        for (JavaSyntaxError error : errors) {
            int errorOffset = startOffset + error.getOffset();
            
            // Clamp error offset to valid range
            if (errorOffset >= file.getTextLength()) {
                errorOffset = Math.max(0, file.getTextLength() - 1);
            }
            if (errorOffset < startOffset) {
                errorOffset = startOffset;
            }
            if (errorOffset > endOffset) {
                errorOffset = endOffset;
            }
            
            // Try to find element at error location
            PsiElement element = file.findElementAt(errorOffset);
            
            // If not found, try nearby positions
            if (element == null && errorOffset > 0) {
                element = file.findElementAt(errorOffset - 1);
            }
            if (element == null && errorOffset < file.getTextLength() - 1) {
                element = file.findElementAt(errorOffset + 1);
            }
            
            // If still not found, use element at start of code block
            if (element == null) {
                element = file.findElementAt(startOffset);
            }
            
            // Register the problem
            if (element != null) {
                holder.registerProblem(
                    element,
                    error.getMessage(),
                    ProblemHighlightType.ERROR
                );
            } else {
                // Last resort: register on file with offset info
                holder.registerProblem(
                    file,
                    "Java code error in @code{} block at offset " + errorOffset + ": " + error.getMessage(),
                    ProblemHighlightType.ERROR
                );
            }
        }
        
        // Use IntelliJ's Java PSI for detailed validation if available
        validateWithJavaPsi(file, javaCode, startOffset, holder);
    }
    
    private void validateWithJavaPsi(MvelFile file, String javaCode, int startOffset, ProblemsHolder holder) {
        try {
            Project project = file.getProject();
            PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
            
            // Try to parse as Java statements
            // Wrap in a method body context for better parsing
            String wrappedCode = "class Temp { void method() { " + javaCode + " } }";
            
            try {
                PsiClass tempClass = elementFactory.createClassFromText(wrappedCode, null);
                PsiMethod[] methods = tempClass.getMethods();
                if (methods.length > 0) {
                    PsiCodeBlock codeBlock = methods[0].getBody();
                    if (codeBlock != null) {
                        validateJavaCodeBlock(codeBlock, startOffset, holder);
                    }
                }
            } catch (Exception e) {
                // Parsing failed - this indicates a syntax error
                String errorMsg = extractErrorMessage(e);
                PsiElement element = file.findElementAt(startOffset);
                if (element != null) {
                    holder.registerProblem(
                        element,
                        "Java syntax error in @code{} block: " + errorMsg,
                        ProblemHighlightType.ERROR
                    );
                }
            }
        } catch (Exception e) {
            // Java PSI not available or other error - skip detailed validation
        }
    }
    
    private void validateJavaCodeBlock(PsiCodeBlock codeBlock, int baseOffset, ProblemsHolder holder) {
        if (codeBlock == null) {
            return;
        }
        
        PsiStatement[] statements = codeBlock.getStatements();
        
        for (PsiStatement statement : statements) {
            // Check for missing semicolons on expression statements
            if (statement instanceof PsiExpressionStatement) {
                PsiExpressionStatement exprStmt = (PsiExpressionStatement) statement;
                PsiElement lastChild = exprStmt.getLastChild();
                if (lastChild != null && !lastChild.getText().equals(";")) {
                    // Check if semicolon is missing
                    PsiElement nextSibling = statement.getNextSibling();
                    if (nextSibling == null || !nextSibling.getText().trim().equals(";")) {
                        holder.registerProblem(
                            statement,
                            "Missing semicolon",
                            ProblemHighlightType.ERROR
                        );
                    }
                }
            }
            
            // Check variable declarations
            if (statement instanceof PsiDeclarationStatement) {
                PsiDeclarationStatement declStmt = (PsiDeclarationStatement) statement;
                PsiElement[] declaredElements = declStmt.getDeclaredElements();
                for (PsiElement declared : declaredElements) {
                    if (declared instanceof PsiVariable) {
                        PsiVariable variable = (PsiVariable) declared;
                        // Check if variable has a type
                        if (variable.getTypeElement() == null && variable.getType() == null) {
                            holder.registerProblem(
                                variable,
                                "Variable declaration missing type",
                                ProblemHighlightType.ERROR
                            );
                        }
                    }
                }
            }
            
            // Recursively check nested code blocks
            PsiCodeBlock nestedBlock = PsiTreeUtil.findChildOfType(statement, PsiCodeBlock.class);
            if (nestedBlock != null) {
                validateJavaCodeBlock(nestedBlock, baseOffset, holder);
            }
        }
    }
    
    private List<JavaSyntaxError> validateJavaSyntax(String javaCode) {
        List<JavaSyntaxError> errors = new ArrayList<>();
        
        // Split into lines for line-by-line analysis
        String[] lines = javaCode.split("\n", -1); // -1 to preserve trailing empty strings
        int currentOffset = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            
            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                currentOffset += line.length() + (i < lines.length - 1 ? 1 : 0); // +1 for newline except last line
                continue;
            }
            
            // Check for statements that should end with semicolon
            if (isStatementRequiringSemicolon(trimmed) && !trimmed.endsWith(";") && !trimmed.endsWith("{") && !trimmed.endsWith("}")) {
                // Check if next line starts with something that suggests continuation
                boolean isContinued = false;
                if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    // Check if it's a continuation (starts with operator, dot, etc.)
                    if (!nextLine.isEmpty() && (
                        nextLine.startsWith(".") || nextLine.startsWith("+") || 
                        nextLine.startsWith("-") || nextLine.startsWith("*") || 
                        nextLine.startsWith("/") || nextLine.startsWith("(") ||
                        nextLine.startsWith("[") || nextLine.startsWith("@") ||
                        nextLine.startsWith("++") || nextLine.startsWith("--"))) {
                        isContinued = true;
                    }
                }
                // Check if current line ends with an operator that suggests continuation
                if (trimmed.endsWith(".") || trimmed.endsWith("+") || 
                    trimmed.endsWith("-") || trimmed.endsWith("*") || 
                    trimmed.endsWith("/") || trimmed.endsWith("=") ||
                    trimmed.endsWith(",") || trimmed.endsWith("||") ||
                    trimmed.endsWith("&&") || trimmed.endsWith("?")) {
                    isContinued = true;
                }
                
                if (!isContinued) {
                    // Calculate position at end of line (where semicolon should be)
                    int semicolonPos = currentOffset + line.length();
                    if (semicolonPos < currentOffset) semicolonPos = currentOffset;
                    // Make sure we're pointing to a valid position
                    if (semicolonPos >= 0) {
                        errors.add(new JavaSyntaxError(semicolonPos, "Missing semicolon"));
                    }
                }
            }
            
            // Check for common syntax errors
            if (trimmed.endsWith("=") && i + 1 < lines.length) {
                String nextLine = lines[i + 1].trim();
                if (nextLine.isEmpty() || nextLine.startsWith("//")) {
                    errors.add(new JavaSyntaxError(currentOffset + line.length(), 
                        "Incomplete assignment statement"));
                }
            }
            
            currentOffset += line.length() + (i < lines.length - 1 ? 1 : 0); // +1 for newline except last line
        }
        
        // Check for balanced braces
        int totalOpenBraces = countChar(javaCode, '{');
        int totalCloseBraces = countChar(javaCode, '}');
        if (totalOpenBraces != totalCloseBraces) {
            errors.add(new JavaSyntaxError(javaCode.length() - 1, 
                "Unmatched braces: " + (totalOpenBraces > totalCloseBraces ? 
                    (totalOpenBraces - totalCloseBraces) + " opening brace(s)" : 
                    (totalCloseBraces - totalOpenBraces) + " closing brace(s)")));
        }
        
        // Check for balanced parentheses
        int totalOpenParens = countChar(javaCode, '(');
        int totalCloseParens = countChar(javaCode, ')');
        if (totalOpenParens != totalCloseParens) {
            errors.add(new JavaSyntaxError(javaCode.length() - 1, 
                "Unmatched parentheses: " + (totalOpenParens > totalCloseParens ? 
                    (totalOpenParens - totalCloseParens) + " opening parenthesis/parentheses" : 
                    (totalCloseParens - totalOpenParens) + " closing parenthesis/parentheses")));
        }
        
        return errors;
    }
    
    private boolean isStatementRequiringSemicolon(String line) {
        String trimmed = line.trim();
        
        // Skip control flow statements that don't require semicolons
        if (trimmed.startsWith("if ") || trimmed.startsWith("if(") ||
            trimmed.startsWith("for ") || trimmed.startsWith("for(") ||
            trimmed.startsWith("while ") || trimmed.startsWith("while(") ||
            trimmed.startsWith("switch ") || trimmed.startsWith("switch(") ||
            trimmed.startsWith("try ") || trimmed.startsWith("try{") ||
            trimmed.startsWith("catch ") || trimmed.startsWith("catch(") ||
            trimmed.startsWith("finally ") || trimmed.startsWith("finally{") ||
            trimmed.startsWith("synchronized ") || trimmed.startsWith("synchronized(") ||
            trimmed.startsWith("return ") || trimmed.equals("return") ||
            trimmed.startsWith("break ") || trimmed.equals("break") ||
            trimmed.startsWith("continue ") || trimmed.equals("continue") ||
            trimmed.startsWith("throw ") ||
            trimmed.startsWith("}") || trimmed.startsWith("{") ||
            trimmed.startsWith("//") || trimmed.startsWith("/*") ||
            trimmed.startsWith("@") || // annotations
            trimmed.endsWith("{")) {
            return false;
        }
        
        // Variable declarations, assignments, method calls, etc. require semicolons
        return true;
    }
    
    private int countChar(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) {
                count++;
            }
        }
        return count;
    }
    
    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            // Extract meaningful error message
            if (message.contains("';' expected")) {
                return "Missing semicolon";
            } else if (message.contains("';'")) {
                return "Syntax error: " + message;
            } else if (message.contains("expected")) {
                return "Syntax error: " + message;
            }
            return message;
        }
        return "Java syntax error";
    }
    
    private static class JavaSyntaxError {
        private final int offset;
        private final String message;
        
        public JavaSyntaxError(int offset, String message) {
            this.offset = offset;
            this.message = message;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
