package com.mvel.linter.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.mvel.linter.MvelFileType;
import com.mvel.linter.MvelLanguage;
import org.jetbrains.annotations.NotNull;

public class MvelFile extends PsiFileBase {
    public MvelFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MvelLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return MvelFileType.INSTANCE;
    }
}

