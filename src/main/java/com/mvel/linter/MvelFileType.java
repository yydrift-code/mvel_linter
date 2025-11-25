package com.mvel.linter;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MvelFileType extends LanguageFileType {
    public static final MvelFileType INSTANCE = new MvelFileType();

    private MvelFileType() {
        super(MvelLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "MVEL File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Apache MVEL expression language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "mvel";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }
}

