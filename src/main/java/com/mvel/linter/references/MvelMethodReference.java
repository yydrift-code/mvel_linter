package com.mvel.linter.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.mvel.linter.navigation.MvelNavigationResolver;
import org.jetbrains.annotations.NotNull;

public class MvelMethodReference extends PsiPolyVariantReferenceBase<PsiElement> {
    public MvelMethodReference(@NotNull PsiElement element) {
        super(element, TextRange.from(0, element.getTextLength()));
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        PsiElement[] candidates = MvelNavigationResolver.resolveTargets(getElement());
        if (candidates.length == 0) {
            return ResolveResult.EMPTY_ARRAY;
        }
        return PsiElementResolveResult.createResults(candidates);
    }

    @Override
    public Object @NotNull [] getVariants() {
        return ResolveResult.EMPTY_ARRAY;
    }

    static boolean isMethodCallIdentifier(@NotNull PsiElement element) {
        return MvelNavigationResolver.isReferenceCandidate(element);
    }
}
