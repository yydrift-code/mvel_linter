package com.mvel.linter.lexer;

import com.intellij.psi.tree.IElementType;
import com.mvel.linter.MvelLanguage;

public class MvelTokenTypes {
    public static final IElementType IDENTIFIER = new MvelElementType("IDENTIFIER");
    public static final IElementType STRING_LITERAL = new MvelElementType("STRING_LITERAL");
    public static final IElementType NUMBER_LITERAL = new MvelElementType("NUMBER_LITERAL");
    public static final IElementType BOOLEAN_LITERAL = new MvelElementType("BOOLEAN_LITERAL");
    public static final IElementType NULL_LITERAL = new MvelElementType("NULL_LITERAL");
    public static final IElementType EMPTY_LITERAL = new MvelElementType("EMPTY_LITERAL");
    
    // Operators
    public static final IElementType EQ = new MvelElementType("EQ");
    public static final IElementType NE = new MvelElementType("NE");
    public static final IElementType LT = new MvelElementType("LT");
    public static final IElementType GT = new MvelElementType("GT");
    public static final IElementType LE = new MvelElementType("LE");
    public static final IElementType GE = new MvelElementType("GE");
    public static final IElementType AND = new MvelElementType("AND");
    public static final IElementType OR = new MvelElementType("OR");
    public static final IElementType NOT = new MvelElementType("NOT");
    public static final IElementType ASSIGN = new MvelElementType("ASSIGN");
    public static final IElementType PLUS = new MvelElementType("PLUS");
    public static final IElementType MINUS = new MvelElementType("MINUS");
    public static final IElementType MUL = new MvelElementType("MUL");
    public static final IElementType DIV = new MvelElementType("DIV");
    public static final IElementType MOD = new MvelElementType("MOD");
    
    // Delimiters
    public static final IElementType DOT = new MvelElementType("DOT");
    public static final IElementType COMMA = new MvelElementType("COMMA");
    public static final IElementType SEMICOLON = new MvelElementType("SEMICOLON");
    public static final IElementType LPAREN = new MvelElementType("LPAREN");
    public static final IElementType RPAREN = new MvelElementType("RPAREN");
    public static final IElementType LBRACKET = new MvelElementType("LBRACKET");
    public static final IElementType RBRACKET = new MvelElementType("RBRACKET");
    public static final IElementType LBRACE = new MvelElementType("LBRACE");
    public static final IElementType RBRACE = new MvelElementType("RBRACE");
    public static final IElementType COLON = new MvelElementType("COLON");
    public static final IElementType QUESTION = new MvelElementType("QUESTION");
    public static final IElementType AT = new MvelElementType("AT");
    
    // Template keywords
    public static final IElementType TEMPLATE_COMMENT = new MvelElementType("TEMPLATE_COMMENT");
    public static final IElementType TEMPLATE_CODE = new MvelElementType("TEMPLATE_CODE");
    public static final IElementType TEMPLATE_INCLUDE = new MvelElementType("TEMPLATE_INCLUDE");
    public static final IElementType TEMPLATE_INCLUDE_NAMED = new MvelElementType("TEMPLATE_INCLUDE_NAMED");
    public static final IElementType TEMPLATE_FOREACH = new MvelElementType("TEMPLATE_FOREACH");
    public static final IElementType TEMPLATE_IF = new MvelElementType("TEMPLATE_IF");
    public static final IElementType TEMPLATE_ELSE = new MvelElementType("TEMPLATE_ELSE");
    public static final IElementType TEMPLATE_END = new MvelElementType("TEMPLATE_END");
    public static final IElementType TEMPLATE_DECLARE = new MvelElementType("TEMPLATE_DECLARE");
    
    // Keywords
    public static final IElementType IF = new MvelElementType("IF");
    public static final IElementType ELSE = new MvelElementType("ELSE");
    public static final IElementType FOR = new MvelElementType("FOR");
    public static final IElementType FOREACH = new MvelElementType("FOREACH");
    public static final IElementType WHILE = new MvelElementType("WHILE");
    public static final IElementType DO = new MvelElementType("DO");
    public static final IElementType UNTIL = new MvelElementType("UNTIL");
    public static final IElementType RETURN = new MvelElementType("RETURN");
    public static final IElementType NEW = new MvelElementType("NEW");
    public static final IElementType FUNCTION = new MvelElementType("FUNCTION");
    public static final IElementType DEF = new MvelElementType("DEF");
    public static final IElementType ISDEF = new MvelElementType("ISDEF");
    public static final IElementType WITH = new MvelElementType("WITH");
    public static final IElementType ASSERT = new MvelElementType("ASSERT");
    
    // Special
    public static final IElementType WHITESPACE = new MvelElementType("WHITESPACE");
    public static final IElementType COMMENT = new MvelElementType("COMMENT");
    public static final IElementType LINE_COMMENT = new MvelElementType("LINE_COMMENT");
    
    private static class MvelElementType extends IElementType {
        public MvelElementType(String debugName) {
            super(debugName, MvelLanguage.INSTANCE);
        }
    }
}

