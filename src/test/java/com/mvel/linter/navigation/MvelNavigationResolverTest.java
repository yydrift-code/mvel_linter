package com.mvel.linter.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MvelNavigationResolverTest {
    @Test
    public void findsLatestLocalFunctionDeclarationBeforeUsage() {
        String text = """
                def helper(value) { value }
                helper(foo)
                def helper(value, extra) { value + extra }
                helper(foo, bar)
                """;

        int usageOffset = text.lastIndexOf("helper");
        int declarationOffset = text.indexOf("helper(value, extra)");

        assertEquals(Integer.valueOf(declarationOffset),
                MvelNavigationResolver.findLocalFunctionDeclarationOffset(text, "helper", usageOffset));
    }

    @Test
    public void findsLatestAssignmentDeclarationBeforeUsage() {
        String text = """
                value = 1
                other = value + 1
                value = 2
                result = value
                """;

        int usageOffset = text.lastIndexOf("value");
        int declarationOffset = text.lastIndexOf("value = 2");

        assertEquals(Integer.valueOf(declarationOffset),
                MvelNavigationResolver.findLocalVariableDeclarationOffset(text, "value", usageOffset));
    }

    @Test
    public void findsForeachVariableDeclaration() {
        String text = """
                @foreach{item : items}
                item.name
                @end{}
                """;

        int usageOffset = text.indexOf("item.name");
        int declarationOffset = text.indexOf("item : items");

        assertEquals(Integer.valueOf(declarationOffset),
                MvelNavigationResolver.findLocalVariableDeclarationOffset(text, "item", usageOffset));
    }

    @Test
    public void ignoresAssignmentsAfterUsage() {
        String text = """
                result = value
                value = 2
                """;

        int usageOffset = text.indexOf("value");
        assertNull(MvelNavigationResolver.findLocalVariableDeclarationOffset(text, "value", usageOffset));
    }
}
