package com.mvel.linter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MvelCommenterTest {
    @Test
    public void exposesJavaScriptLikeCommentPrefixes() {
        MvelCommenter commenter = new MvelCommenter();

        assertEquals("//", commenter.getLineCommentPrefix());
        assertEquals("/*", commenter.getBlockCommentPrefix());
        assertEquals("*/", commenter.getBlockCommentSuffix());
        assertNull(commenter.getCommentedBlockCommentPrefix());
        assertNull(commenter.getCommentedBlockCommentSuffix());
    }
}
