package com.mvel.linter.editor;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.mvel.linter.MvelFileType;
import com.mvel.linter.codeblock.MvelCodeBlockSupport;
import org.jetbrains.annotations.NotNull;

public class MvelEnterHandler implements EnterHandlerDelegate {
    @Override
    public @NotNull Result preprocessEnter(
            @NotNull PsiFile file,
            @NotNull Editor editor,
            @NotNull Ref<Integer> caretOffsetRef,
            @NotNull Ref<Integer> caretAdvanceRef,
            @NotNull DataContext dataContext,
            EditorActionHandler originalHandler
    ) {
        FileType fileType = file.getFileType();
        if (fileType != MvelFileType.INSTANCE) {
            return Result.Continue;
        }

        if (MvelCodeBlockSupport.isInsideJavaCodeBlockContent(file, editor.getCaretModel().getOffset())) {
            return Result.Continue;
        }

        int caretOffset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        MvelSmartKeysSupport.BraceEnterExpansion expansion =
                MvelSmartKeysSupport.buildBraceEnterExpansion(document.getCharsSequence(), caretOffset);
        if (expansion == null) {
            return Result.Continue;
        }

        document.replaceString(
                expansion.getStartOffset(),
                expansion.getEndOffset(),
                expansion.getReplacement()
        );
        editor.getCaretModel().moveToOffset(expansion.getStartOffset() + expansion.getCaretShift());
        caretOffsetRef.set(editor.getCaretModel().getOffset());
        caretAdvanceRef.set(0);
        return Result.Stop;
    }

    @Override
    public @NotNull Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        return Result.Continue;
    }
}
