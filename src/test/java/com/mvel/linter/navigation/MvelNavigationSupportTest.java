package com.mvel.linter.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MvelNavigationSupportTest {
    @Test
    public void countsEmptyArgumentList() {
        assertEquals(Integer.valueOf(0), MvelNavigationSupport.inferArgumentCount("foo()", 3));
    }

    @Test
    public void countsNestedArgumentsWithoutSplittingOnInnerCommas() {
        String text = "foo({\"a\": [bar(1, 2), 3]}, baz, \"x,y\")";
        assertEquals(Integer.valueOf(3), MvelNavigationSupport.inferArgumentCount(text, 3));
    }

    @Test
    public void ignoresCommentsAndWhitespaceBetweenArguments() {
        String text = "foo(first, /* inner, comment */ second, // trailing comment\n third)";
        assertEquals(Integer.valueOf(3), MvelNavigationSupport.inferArgumentCount(text, 3));
    }

    @Test
    public void returnsNullForBrokenInvocation() {
        assertNull(MvelNavigationSupport.inferArgumentCount("foo(bar", 3));
        assertNull(MvelNavigationSupport.inferArgumentCount("foo bar)", 3));
    }

    @Test
    public void recognizesUppercaseQualifierHints() {
        assertTrue(MvelNavigationSupport.isUppercaseQualifier("FormatterUtil"));
        assertEquals(false, MvelNavigationSupport.isUppercaseQualifier("formatterUtil"));
        assertEquals(false, MvelNavigationSupport.isUppercaseQualifier(""));
    }
}
