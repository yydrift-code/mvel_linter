package com.mvel.linter.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.mvel.linter.MvelFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class MvelColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
        new AttributesDescriptor("Keyword", MvelSyntaxHighlighter.KEYWORD),
        new AttributesDescriptor("String", MvelSyntaxHighlighter.STRING),
        new AttributesDescriptor("Number", MvelSyntaxHighlighter.NUMBER),
        new AttributesDescriptor("Identifier", MvelSyntaxHighlighter.IDENTIFIER),
        new AttributesDescriptor("Operator", MvelSyntaxHighlighter.OPERATOR),
        new AttributesDescriptor("Comment", MvelSyntaxHighlighter.COMMENT),
        new AttributesDescriptor("Brackets", MvelSyntaxHighlighter.BRACKETS),
        new AttributesDescriptor("Braces", MvelSyntaxHighlighter.BRACES),
        new AttributesDescriptor("Parentheses", MvelSyntaxHighlighter.PARENTHESES),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new MvelSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "// MVEL Example\n" +
               "user.name == 'John Doe'\n" +
               "if (age > 18) {\n" +
               "    status = 'adult';\n" +
               "} else {\n" +
               "    status = 'minor';\n" +
               "}\n" +
               "result = (x * 2) + 10;\n" +
               "items = [\"apple\", \"banana\", \"cherry\"];\n" +
               "map = [\"key1\" : \"value1\", \"key2\" : \"value2\"];";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "MVEL";
    }
}

