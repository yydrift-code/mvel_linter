package com.mvel.linter.codeblock;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.PsiElement;
import com.mvel.linter.psi.impl.MvelTemplateBlockImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MvelJavaCodeBlockInjector implements MultiHostInjector {
    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof MvelTemplateBlockImpl host) || !host.isValidHost() || !host.isCodeBlock()) {
            return;
        }

        MvelJavaCodeBlockModel model = MvelJavaCodeBlockModelService.getInstance(host.getProject()).getModel(host);
        registrar.startInjecting(JavaLanguage.INSTANCE);
        registrar.addPlace(model.prefix(), model.suffix(), host, host.getContentRangeInElement());
        registrar.doneInjecting();
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(MvelTemplateBlockImpl.class);
    }
}
