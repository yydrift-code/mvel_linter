package com.mvel.linter;

import com.intellij.lang.Language;

public class MvelLanguage extends Language {
    public static final MvelLanguage INSTANCE = new MvelLanguage();

    private MvelLanguage() {
        super("MVEL");
    }
}

