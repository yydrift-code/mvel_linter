package com.mvel.linter.editor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MvelSmartKeysSupportTest {
    @Test
    public void autoInsertClosingBraceAtEndOfLine() {
        assertTrue(MvelSmartKeysSupport.shouldAutoInsertClosingBrace("def log(message) ", 17, '{'));
        assertTrue(MvelSmartKeysSupport.shouldAutoInsertClosingBrace("call", 4, '('));
    }

    @Test
    public void doesNotAutoInsertBeforeIdentifier() {
        assertEquals(false, MvelSmartKeysSupport.shouldAutoInsertClosingBrace("foobar", 1, '{'));
    }

    @Test
    public void buildsEnterExpansionBetweenBraces() {
        MvelSmartKeysSupport.BraceEnterExpansion expansion =
                MvelSmartKeysSupport.buildBraceEnterExpansion("def log() {}", 11);

        assertNotNull(expansion);
        assertEquals("{\n    \n}", expansion.getReplacement());
        assertEquals(6, expansion.getCaretShift());
    }

    @Test
    public void buildsEnterExpansionPreservingBaseIndent() {
        String text = "    if (ready) {}";
        int caretOffset = text.indexOf('}') ;
        MvelSmartKeysSupport.BraceEnterExpansion expansion =
                MvelSmartKeysSupport.buildBraceEnterExpansion(text, caretOffset);

        assertNotNull(expansion);
        assertEquals("{\n        \n    }", expansion.getReplacement());
    }

    @Test
    public void returnsNullWhenNotBetweenCurlyBraces() {
        assertNull(MvelSmartKeysSupport.buildBraceEnterExpansion("call()", 5));
    }
}
