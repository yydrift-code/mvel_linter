package com.mvel.linter.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.mvel.linter.MvelLanguage;
import com.mvel.linter.lexer.MvelLexer;
import com.mvel.linter.lexer.MvelTokenTypes;
import com.mvel.linter.psi.MvelFile;
import org.jetbrains.annotations.NotNull;

public class MvelParserDefinition implements ParserDefinition {
    public static final TokenSet WHITE_SPACES = TokenSet.create(MvelTokenTypes.WHITESPACE);
    public static final TokenSet COMMENTS = TokenSet.create(
            MvelTokenTypes.LINE_COMMENT,
            MvelTokenTypes.COMMENT
    );
    public static final TokenSet STRING_LITERALS = TokenSet.create(MvelTokenTypes.STRING_LITERAL);
    public static final IFileElementType FILE = new IFileElementType(MvelLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new MvelLexer();
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRING_LITERALS;
    }

    @NotNull
    @Override
    public PsiParser createParser(Project project) {
        return new MvelParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MvelFile(viewProvider);
    }

    @NotNull
    @Override
    public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        // Use the factory for all nodes - it will handle both tokens and expressions
        return MvelTypes.Factory.createElement(node);
    }
}

