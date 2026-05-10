package com.mvel.linter;

import com.intellij.lang.BracePair;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MvelBraceMatcherTest {
    @Test
    public void exposesStandardAndTemplateBracePairs() {
        MvelBraceMatcher matcher = new MvelBraceMatcher();
        List<BracePair> pairs = Arrays.asList(matcher.getPairs());

        assertTrue(containsPair(pairs, MvelTokenTypes.LPAREN, MvelTokenTypes.RPAREN));
        assertTrue(containsPair(pairs, MvelTokenTypes.LBRACKET, MvelTokenTypes.RBRACKET));
        assertTrue(containsPair(pairs, MvelTokenTypes.LBRACE, MvelTokenTypes.RBRACE));
        assertEquals(3, pairs.size());
    }

    private boolean containsPair(List<BracePair> pairs, Object left, Object right) {
        return pairs.stream().anyMatch(pair -> pair.getLeftBraceType() == left && pair.getRightBraceType() == right);
    }
}
