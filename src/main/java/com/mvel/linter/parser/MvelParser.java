package com.mvel.linter.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.mvel.linter.lexer.MvelTokenTypes;
import org.jetbrains.annotations.NotNull;

public class MvelParser implements PsiParser {
    // Maximum recursion depth to prevent stack overflow
    private static final int MAX_RECURSION_DEPTH = 1000;
    
    @NotNull
    @Override
    public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();
        
        int parseCount = 0;
        int maxParseIterations = 100000; // Safety limit for very long files
        
        while (!builder.eof() && parseCount < maxParseIterations) {
            parseCount++;
            if (!parseExpression(builder, 0)) {
                // If we can't parse, advance lexer to prevent infinite loop
                builder.advanceLexer();
            }
        }
        
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    private boolean parseExpression(PsiBuilder builder, int depth) {
        // Prevent stack overflow from deep recursion
        if (depth > MAX_RECURSION_DEPTH) {
            // Force advance and return false to break recursion
            if (!builder.eof()) {
                builder.advanceLexer();
            }
            return false;
        }
        
        if (builder.eof()) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        
        if (parseStatement(builder, depth + 1)) {
            marker.done(MvelTypes.EXPRESSION);
            return true;
        } else {
            marker.drop();
            return false;
        }
    }

    private boolean parseStatement(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            if (!builder.eof()) {
                builder.advanceLexer();
            }
            return false;
        }
        
        if (builder.eof()) {
            return false;
        }
        
        IElementType token = builder.getTokenType();
        
        if (token == null) {
            return false;
        }
        
        // Handle template syntax first
        if (token == MvelTokenTypes.TEMPLATE_CODE ||
            token == MvelTokenTypes.TEMPLATE_IF ||
            token == MvelTokenTypes.TEMPLATE_ELSE ||
            token == MvelTokenTypes.TEMPLATE_FOREACH ||
            token == MvelTokenTypes.TEMPLATE_INCLUDE ||
            token == MvelTokenTypes.TEMPLATE_INCLUDE_NAMED ||
            token == MvelTokenTypes.TEMPLATE_COMMENT ||
            token == MvelTokenTypes.TEMPLATE_DECLARE ||
            token == MvelTokenTypes.TEMPLATE_END ||
            token == MvelTokenTypes.AT) {
            return parseTemplateBlock(builder, depth + 1);
        }
        
        // Handle different statement types
        if (token == MvelTokenTypes.IF) {
            return parseIfStatement(builder, depth + 1);
        } else if (token == MvelTokenTypes.FOR || token == MvelTokenTypes.FOREACH) {
            return parseForStatement(builder, depth + 1);
        } else if (token == MvelTokenTypes.WHILE || token == MvelTokenTypes.DO) {
            return parseWhileStatement(builder, depth + 1);
        } else if (token == MvelTokenTypes.RETURN) {
            return parseReturnStatement(builder, depth + 1);
        } else if (token == MvelTokenTypes.DEF || token == MvelTokenTypes.FUNCTION) {
            return parseFunctionDefinition(builder, depth + 1);
        } else {
            return parseAssignmentOrExpression(builder, depth + 1);
        }
    }

    private boolean parseIfStatement(PsiBuilder builder, int depth) {
        if (builder.getTokenType() != MvelTokenTypes.IF) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // consume 'if'
        
        if (builder.getTokenType() == MvelTokenTypes.LPAREN) {
            builder.advanceLexer(); // consume '('
            parseExpression(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RPAREN) {
                builder.advanceLexer(); // consume ')'
            }
        }
        
        parseExpression(builder, depth + 1);
        
        if (builder.getTokenType() == MvelTokenTypes.ELSE) {
            builder.advanceLexer(); // consume 'else'
            parseExpression(builder, depth + 1);
        }
        
        marker.done(MvelTypes.IF_STATEMENT);
        return true;
    }

    private boolean parseForStatement(PsiBuilder builder, int depth) {
        IElementType token = builder.getTokenType();
        if (token != MvelTokenTypes.FOR && token != MvelTokenTypes.FOREACH) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        
        if (builder.getTokenType() == MvelTokenTypes.LPAREN) {
            builder.advanceLexer();
            parseExpression(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RPAREN) {
                builder.advanceLexer();
            }
        }
        
        parseExpression(builder, depth + 1);
        marker.done(MvelTypes.FOR_STATEMENT);
        return true;
    }

    private boolean parseWhileStatement(PsiBuilder builder, int depth) {
        IElementType token = builder.getTokenType();
        if (token != MvelTokenTypes.WHILE && token != MvelTokenTypes.DO) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        
        if (builder.getTokenType() == MvelTokenTypes.LPAREN) {
            builder.advanceLexer();
            parseExpression(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RPAREN) {
                builder.advanceLexer();
            }
        }
        
        parseExpression(builder, depth + 1);
        marker.done(MvelTypes.WHILE_STATEMENT);
        return true;
    }

    private boolean parseReturnStatement(PsiBuilder builder, int depth) {
        if (builder.getTokenType() != MvelTokenTypes.RETURN) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        parseExpression(builder, depth + 1);
        marker.done(MvelTypes.RETURN_STATEMENT);
        return true;
    }

    private boolean parseFunctionDefinition(PsiBuilder builder, int depth) {
        IElementType token = builder.getTokenType();
        if (token != MvelTokenTypes.DEF && token != MvelTokenTypes.FUNCTION) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        
        if (builder.getTokenType() == MvelTokenTypes.IDENTIFIER) {
            builder.advanceLexer();
        }
        
        if (builder.getTokenType() == MvelTokenTypes.LPAREN) {
            builder.advanceLexer();
            parseParameterList(builder);
            if (builder.getTokenType() == MvelTokenTypes.RPAREN) {
                builder.advanceLexer();
            }
        }
        
        parseExpression(builder, depth + 1);
        marker.done(MvelTypes.FUNCTION_DEFINITION);
        return true;
    }

    private void parseParameterList(PsiBuilder builder) {
        int iterations = 0;
        while (!builder.eof() && builder.getTokenType() == MvelTokenTypes.IDENTIFIER && iterations < 1000) {
            iterations++;
            builder.advanceLexer();
            if (builder.getTokenType() == MvelTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }
    }

    private boolean parseAssignmentOrExpression(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            if (!builder.eof()) {
                builder.advanceLexer();
            }
            return false;
        }
        
        if (builder.eof()) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        boolean parsed = parsePrimaryExpression(builder, depth + 1);
        
        if (parsed) {
            // Check for assignment
            if (builder.getTokenType() == MvelTokenTypes.ASSIGN) {
                builder.advanceLexer();
                parseExpression(builder, depth + 1);
                marker.done(MvelTypes.ASSIGNMENT);
            } else {
                // Check for binary operators
                int operatorCount = 0;
                while (!builder.eof() && isBinaryOperator(builder.getTokenType()) && operatorCount < 100) {
                    operatorCount++; // Prevent infinite loop
                    builder.advanceLexer();
                    if (!parsePrimaryExpression(builder, depth + 1)) {
                        break;
                    }
                }
                marker.done(MvelTypes.EXPRESSION);
            }
        } else {
            marker.drop();
        }
        
        return parsed;
    }

    private boolean parsePrimaryExpression(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            if (!builder.eof()) {
                builder.advanceLexer();
            }
            return false;
        }
        IElementType token = builder.getTokenType();
        
        if (token == null) {
            return false;
        }
        
        if (token == MvelTokenTypes.IDENTIFIER ||
            token == MvelTokenTypes.STRING_LITERAL ||
            token == MvelTokenTypes.NUMBER_LITERAL ||
            token == MvelTokenTypes.BOOLEAN_LITERAL ||
            token == MvelTokenTypes.NULL_LITERAL ||
            token == MvelTokenTypes.EMPTY_LITERAL) {
            builder.advanceLexer();
            
            // Handle property access
            int dotCount = 0;
            while (!builder.eof() && builder.getTokenType() == MvelTokenTypes.DOT && dotCount < 100) {
                dotCount++; // Prevent infinite loop
                builder.advanceLexer();
                if (builder.getTokenType() == MvelTokenTypes.IDENTIFIER) {
                    builder.advanceLexer();
                } else {
                    break;
                }
            }
            
            // Handle method calls
            if (builder.getTokenType() == MvelTokenTypes.LPAREN) {
                builder.advanceLexer();
                parseArgumentList(builder, depth + 1);
                if (builder.getTokenType() == MvelTokenTypes.RPAREN) {
                    builder.advanceLexer();
                }
            }
            
            return true;
        } else if (token == MvelTokenTypes.LPAREN) {
            builder.advanceLexer();
            parseExpression(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RPAREN) {
                builder.advanceLexer();
            }
            return true;
        } else if (token == MvelTokenTypes.LBRACKET) {
            // Inline list or map
            builder.advanceLexer();
            parseListOrMap(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RBRACKET) {
                builder.advanceLexer();
            }
            return true;
        } else if (token == MvelTokenTypes.LBRACE) {
            // Inline array
            builder.advanceLexer();
            parseArray(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RBRACE) {
                builder.advanceLexer();
            }
            return true;
        }
        
        return false;
    }

    private void parseArgumentList(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return;
        }
        
        int iterations = 0;
        while (!builder.eof() && builder.getTokenType() != MvelTokenTypes.RPAREN && iterations < 1000) {
            iterations++;
            IElementType tokenBefore = builder.getTokenType();
            parseExpression(builder, depth + 1);
            // If parseExpression didn't advance, force advance to prevent infinite loop
            if (tokenBefore == builder.getTokenType() && !builder.eof()) {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == MvelTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }
    }

    private void parseListOrMap(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return;
        }
        
        int iterations = 0;
        while (!builder.eof() && builder.getTokenType() != MvelTokenTypes.RBRACKET && iterations < 1000) {
            iterations++;
            IElementType tokenBefore = builder.getTokenType();
            parseExpression(builder, depth + 1);
            // If parseExpression didn't advance, force advance to prevent infinite loop
            if (tokenBefore == builder.getTokenType() && !builder.eof()) {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == MvelTokenTypes.COLON) {
                builder.advanceLexer();
                parseExpression(builder, depth + 1);
            }
            if (builder.getTokenType() == MvelTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }
    }

    private void parseArray(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return;
        }
        
        int iterations = 0;
        while (!builder.eof() && builder.getTokenType() != MvelTokenTypes.RBRACE && iterations < 1000) {
            iterations++;
            IElementType tokenBefore = builder.getTokenType();
            parseExpression(builder, depth + 1);
            // If parseExpression didn't advance, force advance to prevent infinite loop
            if (tokenBefore == builder.getTokenType() && !builder.eof()) {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == MvelTokenTypes.COMMA) {
                builder.advanceLexer();
            } else {
                break;
            }
        }
    }

    private boolean parseTemplateBlock(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            if (!builder.eof()) {
                builder.advanceLexer();
            }
            return false;
        }
        
        IElementType token = builder.getTokenType();
        if (token == null) {
            return false;
        }
        
        PsiBuilder.Marker marker = builder.mark();
        
        // Consume the template token (@code{, @if{, etc.)
        if (token == MvelTokenTypes.AT) {
            builder.advanceLexer();
            // Check if it's @{...} expression orb
            if (builder.getTokenType() == MvelTokenTypes.LBRACE) {
                builder.advanceLexer();
                // Parse content until matching }
                parseTemplateContent(builder, depth + 1);
                if (builder.getTokenType() == MvelTokenTypes.RBRACE) {
                    builder.advanceLexer();
                }
            }
        } else if (token == MvelTokenTypes.TEMPLATE_CODE ||
                   token == MvelTokenTypes.TEMPLATE_IF ||
                   token == MvelTokenTypes.TEMPLATE_ELSE ||
                   token == MvelTokenTypes.TEMPLATE_FOREACH ||
                   token == MvelTokenTypes.TEMPLATE_INCLUDE ||
                   token == MvelTokenTypes.TEMPLATE_INCLUDE_NAMED ||
                   token == MvelTokenTypes.TEMPLATE_DECLARE) {
            builder.advanceLexer(); // Consume template keyword
            // The lexer already consumed @code{ or similar, now we need to parse the content
            // Parse content until matching }
            parseTemplateContent(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RBRACE) {
                builder.advanceLexer();
            }
        } else if (token == MvelTokenTypes.TEMPLATE_COMMENT) {
            // Comments don't need parsing
            builder.advanceLexer();
            parseTemplateContent(builder, depth + 1);
            if (builder.getTokenType() == MvelTokenTypes.RBRACE) {
                builder.advanceLexer();
            }
        } else if (token == MvelTokenTypes.TEMPLATE_END) {
            builder.advanceLexer();
        } else {
            marker.drop();
            return false;
        }
        
        marker.done(MvelTypes.TEMPLATE_BLOCK);
        return true;
    }
    
    private void parseTemplateContent(PsiBuilder builder, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return;
        }
        
        int braceDepth = 1; // We're already inside one brace
        int iterations = 0;
        
        while (!builder.eof() && braceDepth > 0 && iterations < 10000) {
            iterations++;
            IElementType token = builder.getTokenType();
            
            if (token == MvelTokenTypes.LBRACE) {
                braceDepth++;
                builder.advanceLexer();
            } else if (token == MvelTokenTypes.RBRACE) {
                braceDepth--;
                if (braceDepth > 0) {
                    builder.advanceLexer();
                } else {
                    // This is the closing brace for our template block
                    break;
                }
            } else {
                // Parse as expression or advance
                IElementType tokenBefore = builder.getTokenType();
                if (!parseExpression(builder, depth + 1)) {
                    // If we can't parse, just advance to prevent infinite loop
                    if (tokenBefore == builder.getTokenType() && !builder.eof()) {
                        builder.advanceLexer();
                    }
                }
            }
        }
    }

    private boolean isBinaryOperator(IElementType token) {
        return token == MvelTokenTypes.PLUS ||
               token == MvelTokenTypes.MINUS ||
               token == MvelTokenTypes.MUL ||
               token == MvelTokenTypes.DIV ||
               token == MvelTokenTypes.MOD ||
               token == MvelTokenTypes.EQ ||
               token == MvelTokenTypes.NE ||
               token == MvelTokenTypes.LT ||
               token == MvelTokenTypes.GT ||
               token == MvelTokenTypes.LE ||
               token == MvelTokenTypes.GE ||
               token == MvelTokenTypes.AND ||
               token == MvelTokenTypes.OR;
    }
}

