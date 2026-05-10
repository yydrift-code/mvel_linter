package com.mvel.linter.codeblock;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.mvel.linter.psi.impl.MvelTemplateBlockImpl;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class MvelJavaCodeBlockModelService {
    private static final Key<CachedValue<MvelJavaCodeBlockModel>> MODEL_KEY =
            Key.create("com.mvel.linter.codeblock.MvelJavaCodeBlockModel");

    private final MvelJavaBlockTranspiler transpiler = new MvelJavaBlockTranspiler();

    public static @NotNull MvelJavaCodeBlockModelService getInstance(@NotNull Project project) {
        return project.getService(MvelJavaCodeBlockModelService.class);
    }

    public @NotNull MvelJavaCodeBlockModel getModel(@NotNull MvelTemplateBlockImpl host) {
        return CachedValuesManager.getManager(host.getProject()).getCachedValue(host, MODEL_KEY, () ->
                CachedValueProvider.Result.create(
                        transpiler.transpile(host.getText(), host.getContentRangeInElement()),
                        host.getContainingFile()
                ), false);
    }
}
