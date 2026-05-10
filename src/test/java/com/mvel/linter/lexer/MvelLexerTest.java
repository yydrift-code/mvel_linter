package com.mvel.linter.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MvelLexerTest {
    @Test
    public void keepsLexingAfterUnknownCharacter() {
        MvelLexer lexer = new MvelLexer();
        lexer.start("foo € { bar }", 0, "foo € { bar }".length(), 0);

        List<IElementType> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            tokens.add(lexer.getTokenType());
            lexer.advance();
        }

        assertTrue(tokens.contains(TokenType.BAD_CHARACTER));
        assertTrue(tokens.contains(MvelTokenTypes.LBRACE));
        assertTrue(tokens.contains(MvelTokenTypes.RBRACE));
    }

    @Test
    public void marksSingleAmpersandAsBadCharacterInsteadOfStopping() {
        MvelLexer lexer = new MvelLexer();
        lexer.start("a & b", 0, "a & b".length(), 0);

        List<IElementType> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            tokens.add(lexer.getTokenType());
            lexer.advance();
        }

        assertEquals(TokenType.BAD_CHARACTER, tokens.get(2));
        assertEquals(MvelTokenTypes.IDENTIFIER, tokens.get(tokens.size() - 1));
    }

    @Test
    public void keepsTemplateKeywordAndOpeningBraceAsSeparateTokens() {
        MvelLexer lexer = new MvelLexer();
        lexer.start("@if{foo}", 0, "@if{foo}".length(), 0);

        List<IElementType> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            tokens.add(lexer.getTokenType());
            lexer.advance();
        }

        assertEquals(MvelTokenTypes.TEMPLATE_IF, tokens.get(0));
        assertEquals(MvelTokenTypes.LBRACE, tokens.get(1));
        assertEquals(MvelTokenTypes.IDENTIFIER, tokens.get(2));
        assertEquals(MvelTokenTypes.RBRACE, tokens.get(3));
    }
}
