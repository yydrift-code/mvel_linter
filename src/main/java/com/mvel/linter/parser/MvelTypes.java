package com.mvel.linter.parser;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.mvel.linter.MvelLanguage;

public class MvelTypes {
    public static final IElementType EXPRESSION = new MvelElementType("EXPRESSION");
    public static final IElementType STATEMENT = new MvelElementType("STATEMENT");
    public static final IElementType IF_STATEMENT = new MvelElementType("IF_STATEMENT");
    public static final IElementType FOR_STATEMENT = new MvelElementType("FOR_STATEMENT");
    public static final IElementType WHILE_STATEMENT = new MvelElementType("WHILE_STATEMENT");
    public static final IElementType RETURN_STATEMENT = new MvelElementType("RETURN_STATEMENT");
    public static final IElementType FUNCTION_DEFINITION = new MvelElementType("FUNCTION_DEFINITION");
    public static final IElementType ASSIGNMENT = new MvelElementType("ASSIGNMENT");
    public static final IElementType TEMPLATE_BLOCK = new MvelElementType("TEMPLATE_BLOCK");

    private static class MvelElementType extends IElementType {
        public MvelElementType(String debugName) {
            super(debugName, MvelLanguage.INSTANCE);
        }
    }

    public static class Factory {
        public static com.intellij.psi.PsiElement createElement(com.intellij.lang.ASTNode node) {
            IElementType type = node.getElementType();
            
            // For leaf nodes (tokens), return null to use default token PSI
            // This allows the syntax highlighter to work directly with tokens
            if (node.getFirstChildNode() == null) {
                return null; // Use default token PSI element
            }
            
            // For non-leaf nodes (expressions, statements), create custom PSI elements
            if (type == EXPRESSION || type == STATEMENT || type == IF_STATEMENT ||
                type == FOR_STATEMENT || type == WHILE_STATEMENT || type == RETURN_STATEMENT ||
                type == FUNCTION_DEFINITION || type == ASSIGNMENT || type == TEMPLATE_BLOCK) {
                return new com.mvel.linter.psi.impl.MvelExpressionImpl(node);
            }
            return new com.mvel.linter.psi.impl.MvelElementImpl(node);
        }
    }
}

