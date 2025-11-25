package com.mvel.linter.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.lexer.MvelLexer;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class MvelSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
        createTextAttributesKey("MVEL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey STRING =
        createTextAttributesKey("MVEL_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER =
        createTextAttributesKey("MVEL_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey OPERATOR =
        createTextAttributesKey("MVEL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey IDENTIFIER =
        createTextAttributesKey("MVEL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey COMMENT =
        createTextAttributesKey("MVEL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BRACKETS =
        createTextAttributesKey("MVEL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey BRACES =
        createTextAttributesKey("MVEL_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey PARENTHESES =
        createTextAttributesKey("MVEL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey DOT =
        createTextAttributesKey("MVEL_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey COMMA =
        createTextAttributesKey("MVEL_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey SEMICOLON =
        createTextAttributesKey("MVEL_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey TEMPLATE_TAG =
        createTextAttributesKey("MVEL_TEMPLATE_TAG", DefaultLanguageHighlighterColors.METADATA);

    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] OPERATOR_KEYS = new TextAttributesKey[]{OPERATOR};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] BRACKET_KEYS = new TextAttributesKey[]{BRACKETS};
    private static final TextAttributesKey[] BRACE_KEYS = new TextAttributesKey[]{BRACES};
    private static final TextAttributesKey[] PARENTHESIS_KEYS = new TextAttributesKey[]{PARENTHESES};
    private static final TextAttributesKey[] DOT_KEYS = new TextAttributesKey[]{DOT};
    private static final TextAttributesKey[] COMMA_KEYS = new TextAttributesKey[]{COMMA};
    private static final TextAttributesKey[] SEMICOLON_KEYS = new TextAttributesKey[]{SEMICOLON};
    private static final TextAttributesKey[] TEMPLATE_TAG_KEYS = new TextAttributesKey[]{TEMPLATE_TAG};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new MvelLexer();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType == MvelTokenTypes.IF ||
            tokenType == MvelTokenTypes.ELSE ||
            tokenType == MvelTokenTypes.FOR ||
            tokenType == MvelTokenTypes.FOREACH ||
            tokenType == MvelTokenTypes.WHILE ||
            tokenType == MvelTokenTypes.DO ||
            tokenType == MvelTokenTypes.UNTIL ||
            tokenType == MvelTokenTypes.RETURN ||
            tokenType == MvelTokenTypes.NEW ||
            tokenType == MvelTokenTypes.FUNCTION ||
            tokenType == MvelTokenTypes.DEF ||
            tokenType == MvelTokenTypes.ISDEF ||
            tokenType == MvelTokenTypes.WITH ||
            tokenType == MvelTokenTypes.ASSERT) {
            return KEYWORD_KEYS;
        }
        if (tokenType == MvelTokenTypes.STRING_LITERAL) {
            return STRING_KEYS;
        }
        if (tokenType == MvelTokenTypes.NUMBER_LITERAL) {
            return NUMBER_KEYS;
        }
        if (tokenType == MvelTokenTypes.IDENTIFIER) {
            return IDENTIFIER_KEYS;
        }
        if (tokenType == MvelTokenTypes.LINE_COMMENT ||
            tokenType == MvelTokenTypes.COMMENT) {
            return COMMENT_KEYS;
        }
        if (tokenType == MvelTokenTypes.EQ ||
            tokenType == MvelTokenTypes.NE ||
            tokenType == MvelTokenTypes.LT ||
            tokenType == MvelTokenTypes.GT ||
            tokenType == MvelTokenTypes.LE ||
            tokenType == MvelTokenTypes.GE ||
            tokenType == MvelTokenTypes.AND ||
            tokenType == MvelTokenTypes.OR ||
            tokenType == MvelTokenTypes.NOT ||
            tokenType == MvelTokenTypes.ASSIGN ||
            tokenType == MvelTokenTypes.PLUS ||
            tokenType == MvelTokenTypes.MINUS ||
            tokenType == MvelTokenTypes.MUL ||
            tokenType == MvelTokenTypes.DIV ||
            tokenType == MvelTokenTypes.MOD) {
            return OPERATOR_KEYS;
        }
        if (tokenType == MvelTokenTypes.LBRACKET ||
            tokenType == MvelTokenTypes.RBRACKET) {
            return BRACKET_KEYS;
        }
        if (tokenType == MvelTokenTypes.LBRACE ||
            tokenType == MvelTokenTypes.RBRACE) {
            return BRACE_KEYS;
        }
        if (tokenType == MvelTokenTypes.LPAREN ||
            tokenType == MvelTokenTypes.RPAREN) {
            return PARENTHESIS_KEYS;
        }
        if (tokenType == MvelTokenTypes.DOT) {
            return DOT_KEYS;
        }
        if (tokenType == MvelTokenTypes.COMMA) {
            return COMMA_KEYS;
        }
        if (tokenType == MvelTokenTypes.SEMICOLON) {
            return SEMICOLON_KEYS;
        }
        if (tokenType == MvelTokenTypes.AT ||
            tokenType == MvelTokenTypes.TEMPLATE_COMMENT ||
            tokenType == MvelTokenTypes.TEMPLATE_CODE ||
            tokenType == MvelTokenTypes.TEMPLATE_INCLUDE ||
            tokenType == MvelTokenTypes.TEMPLATE_INCLUDE_NAMED ||
            tokenType == MvelTokenTypes.TEMPLATE_FOREACH ||
            tokenType == MvelTokenTypes.TEMPLATE_IF ||
            tokenType == MvelTokenTypes.TEMPLATE_ELSE ||
            tokenType == MvelTokenTypes.TEMPLATE_END ||
            tokenType == MvelTokenTypes.TEMPLATE_DECLARE) {
            return TEMPLATE_TAG_KEYS;
        }
        return EMPTY_KEYS;
    }
}

