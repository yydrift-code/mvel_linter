package com.mvel.linter.editor;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.mvel.linter.codeblock.MvelCodeBlockSupport;
import com.mvel.linter.MvelFileType;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

public class MvelTypedHandler extends TypedHandlerDelegate {
    @Override
    public @NotNull Result beforeCharTyped(
            char c,
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull FileType fileType
    ) {
        if (fileType != MvelFileType.INSTANCE || !MvelSmartKeysSupport.isOpeningBrace(c)) {
            return Result.CONTINUE;
        }

        if (MvelCodeBlockSupport.isInsideJavaCodeBlockContent(file, editor.getCaretModel().getOffset())) {
            return Result.CONTINUE;
        }

        if (editor.getSelectionModel().hasSelection()) {
            return Result.CONTINUE;
        }

        int offset = editor.getCaretModel().getOffset();
        if (isInsideStringOrComment(file, offset)) {
            return Result.CONTINUE;
        }

        Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();
        if (!MvelSmartKeysSupport.shouldAutoInsertClosingBrace(text, offset, c)) {
            return Result.CONTINUE;
        }

        Character closingBrace = MvelSmartKeysSupport.closingBrace(c);
        if (closingBrace == null) {
            return Result.CONTINUE;
        }

        document.insertString(offset, "" + c + closingBrace);
        editor.getCaretModel().moveToOffset(offset + 1);
        return Result.STOP;
    }

    private boolean isInsideStringOrComment(PsiFile file, int offset) {
        if (offset <= 0) {
            return false;
        }

        IElementType tokenType = PsiUtilCore.getElementType(file.findElementAt(offset - 1));
        return tokenType == MvelTokenTypes.STRING_LITERAL ||
                tokenType == MvelTokenTypes.COMMENT ||
                tokenType == MvelTokenTypes.LINE_COMMENT;
    }
}
