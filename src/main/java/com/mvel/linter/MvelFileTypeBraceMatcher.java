package com.mvel.linter;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;

public class MvelFileTypeBraceMatcher extends PairedBraceMatcherAdapter {
    public MvelFileTypeBraceMatcher() {
        super(new MvelBraceMatcher(), MvelLanguage.INSTANCE);
    }
}
