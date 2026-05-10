package com.mvel.linter.navigation;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.mvel.linter.lexer.MvelLexer;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MvelNavigationResolver {
    private MvelNavigationResolver() {
    }

    public static PsiElement @NotNull [] resolveTargets(PsiElement element) {
        if (!isReferenceCandidate(element)) {
            return PsiElement.EMPTY_ARRAY;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return PsiElement.EMPTY_ARRAY;
        }

        Set<PsiElement> targets = new LinkedHashSet<>();
        String symbolName = element.getText();
        int usageOffset = element.getTextRange().getStartOffset();
        CharSequence fileText = file.getViewProvider().getContents();
        List<TokenInfo> tokens = tokenize(fileText, usageOffset);

        Integer localFunctionOffset = findLocalFunctionDeclarationOffset(tokens, symbolName);
        if (localFunctionOffset != null) {
            addTargetAtOffset(file, localFunctionOffset, targets);
        }

        Integer localVariableOffset = findLocalVariableDeclarationOffset(tokens, symbolName);
        if (localVariableOffset != null) {
            addTargetAtOffset(file, localVariableOffset, targets);
        }

        if (isMethodCallIdentifier(element)) {
            addJavaMethods(element, symbolName, targets);
        } else {
            addStaticFields(element, symbolName, targets);
        }

        return targets.toArray(PsiElement.EMPTY_ARRAY);
    }

    public static boolean isReferenceCandidate(@NotNull PsiElement element) {
        if (PsiUtilCore.getElementType(element) != MvelTokenTypes.IDENTIFIER) {
            return false;
        }

        if (isFunctionDeclarationName(element)) {
            return false;
        }

        PsiElement previousVisibleLeaf = PsiTreeUtil.prevVisibleLeaf(element);
        return PsiUtilCore.getElementType(previousVisibleLeaf) != MvelTokenTypes.NEW;
    }

    public static boolean isMethodCallIdentifier(@NotNull PsiElement element) {
        if (PsiUtilCore.getElementType(element) != MvelTokenTypes.IDENTIFIER) {
            return false;
        }

        PsiElement nextVisibleLeaf = PsiTreeUtil.nextVisibleLeaf(element);
        if (PsiUtilCore.getElementType(nextVisibleLeaf) != MvelTokenTypes.LPAREN) {
            return false;
        }

        IElementType previousType = PsiUtilCore.getElementType(PsiTreeUtil.prevVisibleLeaf(element));
        return previousType != MvelTokenTypes.DEF &&
                previousType != MvelTokenTypes.FUNCTION &&
                previousType != MvelTokenTypes.NEW;
    }

    private static boolean isFunctionDeclarationName(PsiElement element) {
        IElementType previousType = PsiUtilCore.getElementType(PsiTreeUtil.prevVisibleLeaf(element));
        IElementType nextType = PsiUtilCore.getElementType(PsiTreeUtil.nextVisibleLeaf(element));
        return (previousType == MvelTokenTypes.DEF || previousType == MvelTokenTypes.FUNCTION) &&
                nextType == MvelTokenTypes.LPAREN;
    }

    private static void addJavaMethods(PsiElement element, String symbolName, Set<PsiElement> targets) {
        GlobalSearchScope scope = element.getResolveScope();
        PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(element.getProject());

        String classHint = extractUppercaseQualifier(element);
        List<PsiMethod> methods = new ArrayList<>();
        if (classHint != null) {
            for (PsiClass psiClass : shortNamesCache.getClassesByName(classHint, scope)) {
                for (PsiMethod method : psiClass.findMethodsByName(symbolName, true)) {
                    methods.add(method);
                }
            }
        }

        if (methods.isEmpty()) {
            for (PsiMethod method : shortNamesCache.getMethodsByName(symbolName, scope)) {
                methods.add(method);
            }
        }

        Integer argumentCount = MvelNavigationSupport.inferArgumentCount(
                element.getContainingFile().getViewProvider().getContents(),
                element.getTextRange().getEndOffset()
        );

        for (PsiMethod method : filterMethodsByArgumentCount(methods, argumentCount)) {
            targets.add(method);
        }
    }

    private static void addStaticFields(PsiElement element, String symbolName, Set<PsiElement> targets) {
        String classHint = extractUppercaseQualifier(element);
        if (classHint == null) {
            return;
        }

        GlobalSearchScope scope = element.getResolveScope();
        PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(element.getProject());
        for (PsiClass psiClass : shortNamesCache.getClassesByName(classHint, scope)) {
            PsiField field = psiClass.findFieldByName(symbolName, true);
            if (field != null) {
                targets.add(field);
            }
        }
    }

    private static List<PsiMethod> filterMethodsByArgumentCount(List<PsiMethod> methods, Integer argumentCount) {
        if (argumentCount == null) {
            return deduplicate(methods);
        }

        List<PsiMethod> filtered = new ArrayList<>();
        for (PsiMethod method : methods) {
            int parameterCount = method.getParameterList().getParametersCount();
            if (method.isVarArgs()) {
                if (argumentCount >= parameterCount - 1) {
                    filtered.add(method);
                }
            } else if (argumentCount == parameterCount) {
                filtered.add(method);
            }
        }

        return filtered.isEmpty() ? deduplicate(methods) : deduplicate(filtered);
    }

    private static List<PsiMethod> deduplicate(List<PsiMethod> methods) {
        return new ArrayList<>(new LinkedHashSet<>(methods));
    }

    private static String extractUppercaseQualifier(PsiElement element) {
        PsiElement separator = PsiTreeUtil.prevVisibleLeaf(element);
        if (PsiUtilCore.getElementType(separator) != MvelTokenTypes.DOT) {
            return null;
        }

        PsiElement qualifierLeaf = PsiTreeUtil.prevVisibleLeaf(separator);
        if (PsiUtilCore.getElementType(qualifierLeaf) != MvelTokenTypes.IDENTIFIER) {
            return null;
        }

        String qualifier = qualifierLeaf.getText();
        return MvelNavigationSupport.isUppercaseQualifier(qualifier) ? qualifier : null;
    }

    private static Integer findLocalFunctionDeclarationOffset(List<TokenInfo> tokens, String symbolName) {
        Integer result = null;
        for (int i = 0; i < tokens.size(); i++) {
            TokenInfo token = tokens.get(i);
            if (token.type != MvelTokenTypes.IDENTIFIER || !symbolName.equals(token.text)) {
                continue;
            }

            IElementType previousType = previousSignificantType(tokens, i);
            IElementType nextType = nextSignificantType(tokens, i);
            if ((previousType == MvelTokenTypes.DEF || previousType == MvelTokenTypes.FUNCTION) &&
                    nextType == MvelTokenTypes.LPAREN) {
                result = token.startOffset;
            }
        }
        return result;
    }

    private static Integer findLocalVariableDeclarationOffset(List<TokenInfo> tokens, String symbolName) {
        Integer result = null;
        for (int i = 0; i < tokens.size(); i++) {
            TokenInfo token = tokens.get(i);
            if (token.type != MvelTokenTypes.IDENTIFIER || !symbolName.equals(token.text)) {
                continue;
            }

            IElementType previousType = previousSignificantType(tokens, i);
            IElementType nextType = nextSignificantType(tokens, i);

            if (nextType == MvelTokenTypes.ASSIGN && previousType != MvelTokenTypes.DOT) {
                result = token.startOffset;
                continue;
            }

            if (nextType == MvelTokenTypes.COLON && isForeachVariable(tokens, i)) {
                result = token.startOffset;
            }
        }
        return result;
    }

    static Integer findLocalFunctionDeclarationOffset(CharSequence text, String symbolName, int usageOffset) {
        return findLocalFunctionDeclarationOffset(tokenize(text, usageOffset), symbolName);
    }

    static Integer findLocalVariableDeclarationOffset(CharSequence text, String symbolName, int usageOffset) {
        return findLocalVariableDeclarationOffset(tokenize(text, usageOffset), symbolName);
    }

    private static boolean isForeachVariable(List<TokenInfo> tokens, int index) {
        int nesting = 0;
        for (int i = index - 1; i >= 0; i--) {
            IElementType type = tokens.get(i).type;
            if (type == MvelTokenTypes.WHITESPACE ||
                    type == MvelTokenTypes.COMMENT ||
                    type == MvelTokenTypes.LINE_COMMENT) {
                continue;
            }

            if (type == MvelTokenTypes.RPAREN || type == MvelTokenTypes.RBRACE || type == MvelTokenTypes.RBRACKET) {
                nesting++;
                continue;
            }

            if (type == MvelTokenTypes.LPAREN || type == MvelTokenTypes.LBRACE || type == MvelTokenTypes.LBRACKET) {
                if (nesting == 0) {
                    IElementType significantBefore = previousSignificantType(tokens, i);
                    return significantBefore == MvelTokenTypes.FOR ||
                            significantBefore == MvelTokenTypes.FOREACH ||
                            significantBefore == MvelTokenTypes.TEMPLATE_FOREACH;
                }
                nesting--;
                continue;
            }

            if (nesting == 0 &&
                    (type == MvelTokenTypes.FOR || type == MvelTokenTypes.FOREACH || type == MvelTokenTypes.TEMPLATE_FOREACH)) {
                return true;
            }
        }
        return false;
    }

    private static IElementType previousSignificantType(List<TokenInfo> tokens, int index) {
        for (int i = index - 1; i >= 0; i--) {
            IElementType type = tokens.get(i).type;
            if (type != MvelTokenTypes.WHITESPACE &&
                    type != MvelTokenTypes.COMMENT &&
                    type != MvelTokenTypes.LINE_COMMENT) {
                return type;
            }
        }
        return null;
    }

    private static IElementType nextSignificantType(List<TokenInfo> tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            IElementType type = tokens.get(i).type;
            if (type != MvelTokenTypes.WHITESPACE &&
                    type != MvelTokenTypes.COMMENT &&
                    type != MvelTokenTypes.LINE_COMMENT) {
                return type;
            }
        }
        return null;
    }

    private static List<TokenInfo> tokenize(CharSequence text, int endOffsetExclusive) {
        MvelLexer lexer = new MvelLexer();
        lexer.start(text, 0, Math.min(endOffsetExclusive, text.length()), 0);

        List<TokenInfo> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            int startOffset = lexer.getTokenStart();
            int endOffset = lexer.getTokenEnd();
            tokens.add(new TokenInfo(
                    lexer.getTokenType(),
                    text.subSequence(startOffset, endOffset).toString(),
                    startOffset,
                    endOffset
            ));
            lexer.advance();
        }
        return tokens;
    }

    private static void addTargetAtOffset(PsiFile file, int offset, Set<PsiElement> targets) {
        PsiElement target = file.findElementAt(offset);
        if (target != null) {
            targets.add(target);
        }
    }

    private static final class TokenInfo {
        private final IElementType type;
        private final String text;
        private final int startOffset;
        @SuppressWarnings("unused")
        private final int endOffset;

        private TokenInfo(IElementType type, String text, int startOffset, int endOffset) {
            this.type = type;
            this.text = text;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
