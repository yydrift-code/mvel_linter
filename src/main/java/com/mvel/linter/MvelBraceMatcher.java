package com.mvel.linter;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

public class MvelBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(MvelTokenTypes.LPAREN, MvelTokenTypes.RPAREN, false),
            new BracePair(MvelTokenTypes.LBRACKET, MvelTokenTypes.RBRACKET, false),
            new BracePair(MvelTokenTypes.LBRACE, MvelTokenTypes.RBRACE, true)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType leftBraceType, IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(@NotNull PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
