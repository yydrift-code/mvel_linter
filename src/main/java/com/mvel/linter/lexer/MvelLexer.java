package com.mvel.linter.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.MvelLanguage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MvelLexer extends LexerBase {
    private CharSequence buffer;
    private int startOffset;
    private int endOffset;
    private int currentOffset;
    private IElementType tokenType;
    private int tokenStart;
    private int tokenEnd;

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?[fFdDlL]?");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\"(?:[^\"\\\\]|\\\\.)*\"|^'(?:[^'\\\\]|\\\\.)*'");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("^\\s+");
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("^//.*");
    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("^/\\*.*?\\*/", Pattern.DOTALL);

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.currentOffset = startOffset;
        advance();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        if (currentOffset >= endOffset) {
            tokenType = null;
            tokenStart = tokenEnd = currentOffset;
            return;
        }

        tokenStart = currentOffset;
        CharSequence remaining = buffer.subSequence(currentOffset, endOffset);

        // Skip whitespace
        Matcher wsMatcher = WHITESPACE_PATTERN.matcher(remaining);
        if (wsMatcher.find()) {
            tokenType = MvelTokenTypes.WHITESPACE;
            tokenEnd = currentOffset + wsMatcher.end();
            currentOffset = tokenEnd;
            return;
        }

        // Comments
        Matcher lineCommentMatcher = LINE_COMMENT_PATTERN.matcher(remaining);
        if (lineCommentMatcher.find()) {
            tokenType = MvelTokenTypes.LINE_COMMENT;
            tokenEnd = currentOffset + lineCommentMatcher.end();
            currentOffset = tokenEnd;
            return;
        }

        Matcher blockCommentMatcher = BLOCK_COMMENT_PATTERN.matcher(remaining);
        if (blockCommentMatcher.find()) {
            tokenType = MvelTokenTypes.COMMENT;
            tokenEnd = currentOffset + blockCommentMatcher.end();
            currentOffset = tokenEnd;
            return;
        }

        // String literals
        Matcher stringMatcher = STRING_PATTERN.matcher(remaining);
        if (stringMatcher.find()) {
            tokenType = MvelTokenTypes.STRING_LITERAL;
            tokenEnd = currentOffset + stringMatcher.end();
            currentOffset = tokenEnd;
            return;
        }

        // Number literals
        Matcher numberMatcher = NUMBER_PATTERN.matcher(remaining);
        if (numberMatcher.find()) {
            tokenType = MvelTokenTypes.NUMBER_LITERAL;
            tokenEnd = currentOffset + numberMatcher.end();
            currentOffset = tokenEnd;
            return;
        }

        // Operators and delimiters
        char ch = remaining.charAt(0);
        switch (ch) {
            case '=':
                if (remaining.length() > 1 && remaining.charAt(1) == '=') {
                    tokenType = MvelTokenTypes.EQ;
                    tokenEnd = currentOffset + 2;
                } else {
                    tokenType = MvelTokenTypes.ASSIGN;
                    tokenEnd = currentOffset + 1;
                }
                break;
            case '!':
                if (remaining.length() > 1 && remaining.charAt(1) == '=') {
                    tokenType = MvelTokenTypes.NE;
                    tokenEnd = currentOffset + 2;
                } else {
                    tokenType = MvelTokenTypes.NOT;
                    tokenEnd = currentOffset + 1;
                }
                break;
            case '<':
                if (remaining.length() > 1 && remaining.charAt(1) == '=') {
                    tokenType = MvelTokenTypes.LE;
                    tokenEnd = currentOffset + 2;
                } else {
                    tokenType = MvelTokenTypes.LT;
                    tokenEnd = currentOffset + 1;
                }
                break;
            case '>':
                if (remaining.length() > 1 && remaining.charAt(1) == '=') {
                    tokenType = MvelTokenTypes.GE;
                    tokenEnd = currentOffset + 2;
                } else {
                    tokenType = MvelTokenTypes.GT;
                    tokenEnd = currentOffset + 1;
                }
                break;
            case '&':
                if (remaining.length() > 1 && remaining.charAt(1) == '&') {
                    tokenType = MvelTokenTypes.AND;
                    tokenEnd = currentOffset + 2;
                } else {
                    tokenType = null; // Error
                    tokenEnd = currentOffset + 1;
                }
                break;
            case '|':
                if (remaining.length() > 1 && remaining.charAt(1) == '|') {
                    tokenType = MvelTokenTypes.OR;
                    tokenEnd = currentOffset + 2;
                } else {
                    tokenType = null; // Error
                    tokenEnd = currentOffset + 1;
                }
                break;
            case '+':
                tokenType = MvelTokenTypes.PLUS;
                tokenEnd = currentOffset + 1;
                break;
            case '-':
                tokenType = MvelTokenTypes.MINUS;
                tokenEnd = currentOffset + 1;
                break;
            case '*':
                tokenType = MvelTokenTypes.MUL;
                tokenEnd = currentOffset + 1;
                break;
            case '/':
                tokenType = MvelTokenTypes.DIV;
                tokenEnd = currentOffset + 1;
                break;
            case '%':
                tokenType = MvelTokenTypes.MOD;
                tokenEnd = currentOffset + 1;
                break;
            case '.':
                tokenType = MvelTokenTypes.DOT;
                tokenEnd = currentOffset + 1;
                break;
            case ',':
                tokenType = MvelTokenTypes.COMMA;
                tokenEnd = currentOffset + 1;
                break;
            case ';':
                tokenType = MvelTokenTypes.SEMICOLON;
                tokenEnd = currentOffset + 1;
                break;
            case '(':
                tokenType = MvelTokenTypes.LPAREN;
                tokenEnd = currentOffset + 1;
                break;
            case ')':
                tokenType = MvelTokenTypes.RPAREN;
                tokenEnd = currentOffset + 1;
                break;
            case '[':
                tokenType = MvelTokenTypes.LBRACKET;
                tokenEnd = currentOffset + 1;
                break;
            case ']':
                tokenType = MvelTokenTypes.RBRACKET;
                tokenEnd = currentOffset + 1;
                break;
            case '{':
                tokenType = MvelTokenTypes.LBRACE;
                tokenEnd = currentOffset + 1;
                break;
            case '}':
                tokenType = MvelTokenTypes.RBRACE;
                tokenEnd = currentOffset + 1;
                break;
            case ':':
                tokenType = MvelTokenTypes.COLON;
                tokenEnd = currentOffset + 1;
                break;
            case '?':
                tokenType = MvelTokenTypes.QUESTION;
                tokenEnd = currentOffset + 1;
                break;
            case '@':
                // Handle MVEL template syntax: @{}, @code{}, @comment{}, etc.
                if (remaining.length() > 1 && remaining.charAt(1) == '{') {
                    // Check for template keywords
                    String remainingStr = remaining.toString();
                    if (remainingStr.startsWith("@comment{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_COMMENT;
                        tokenEnd = currentOffset + 9; // "@comment{".length()
                    } else if (remainingStr.startsWith("@code{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_CODE;
                        tokenEnd = currentOffset + 6; // "@code{".length()
                    } else if (remainingStr.startsWith("@includeNamed{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_INCLUDE_NAMED;
                        tokenEnd = currentOffset + 15; // "@includeNamed{".length()
                    } else if (remainingStr.startsWith("@include{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_INCLUDE;
                        tokenEnd = currentOffset + 9; // "@include{".length()
                    } else if (remainingStr.startsWith("@foreach{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_FOREACH;
                        tokenEnd = currentOffset + 10; // "@foreach{".length()
                    } else if (remainingStr.startsWith("@if{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_IF;
                        tokenEnd = currentOffset + 4; // "@if{".length()
                    } else if (remainingStr.startsWith("@else{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_ELSE;
                        tokenEnd = currentOffset + 6; // "@else{".length()
                    } else if (remainingStr.startsWith("@end{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_END;
                        tokenEnd = currentOffset + 5; // "@end{".length()
                    } else if (remainingStr.startsWith("@declare{")) {
                        tokenType = MvelTokenTypes.TEMPLATE_DECLARE;
                        tokenEnd = currentOffset + 9; // "@declare{".length()
                    } else {
                        // Simple expression orb: @{...}
                        tokenType = MvelTokenTypes.AT;
                        tokenEnd = currentOffset + 1;
                    }
                } else {
                    // Just @ symbol
                    tokenType = MvelTokenTypes.AT;
                    tokenEnd = currentOffset + 1;
                }
                break;
            default:
                // Try identifier or keyword
                Matcher idMatcher = IDENTIFIER_PATTERN.matcher(remaining);
                if (idMatcher.find()) {
                    String identifier = idMatcher.group();
                    tokenType = getKeywordType(identifier);
                    if (tokenType == null) {
                        tokenType = MvelTokenTypes.IDENTIFIER;
                    }
                    tokenEnd = currentOffset + idMatcher.end();
                } else {
                    // Unknown character
                    tokenType = null;
                    tokenEnd = currentOffset + 1;
                }
                break;
        }

        // Safety check: ensure we always advance to prevent infinite loops
        if (tokenEnd <= currentOffset) {
            tokenEnd = currentOffset + 1;
            tokenType = null; // Mark as error token
        }
        
        currentOffset = tokenEnd;
    }

    private IElementType getKeywordType(String identifier) {
        switch (identifier) {
            case "if": return MvelTokenTypes.IF;
            case "else": return MvelTokenTypes.ELSE;
            case "for": return MvelTokenTypes.FOR;
            case "foreach": return MvelTokenTypes.FOREACH;
            case "while": return MvelTokenTypes.WHILE;
            case "do": return MvelTokenTypes.DO;
            case "until": return MvelTokenTypes.UNTIL;
            case "return": return MvelTokenTypes.RETURN;
            case "new": return MvelTokenTypes.NEW;
            case "function": return MvelTokenTypes.FUNCTION;
            case "def": return MvelTokenTypes.DEF;
            case "isdef": return MvelTokenTypes.ISDEF;
            case "with": return MvelTokenTypes.WITH;
            case "assert": return MvelTokenTypes.ASSERT;
            case "true":
            case "false": return MvelTokenTypes.BOOLEAN_LITERAL;
            case "null":
            case "nil": return MvelTokenTypes.NULL_LITERAL;
            case "empty": return MvelTokenTypes.EMPTY_LITERAL;
            default: return null;
        }
    }

    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}

