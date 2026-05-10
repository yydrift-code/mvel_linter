package com.mvel.linter.references;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import com.mvel.linter.MvelLanguage;
import com.mvel.linter.codeblock.MvelCodeBlockSupport;
import org.jetbrains.annotations.NotNull;

public class MvelMethodReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement().withLanguage(MvelLanguage.INSTANCE),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(
                            @NotNull PsiElement element,
                            @NotNull ProcessingContext context
                    ) {
                        if (MvelCodeBlockSupport.isInsideJavaCodeBlockContent(element)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        if (!MvelMethodReference.isMethodCallIdentifier(element)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        return new PsiReference[]{new MvelMethodReference(element)};
                    }
                }
        );
    }
}
