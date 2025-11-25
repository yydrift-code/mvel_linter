# Syntax Highlighting Fix

## Issue
Nothing is highlighted when opening `.mvel` files.

## Root Cause
The syntax highlighter might not be properly applied because:
1. Tokens might not be accessible as PSI elements for the annotator
2. The syntax highlighter might need both lexer-based and PSI-based highlighting

## Fixes Applied

### 1. Added Annotator
- Created `MvelAnnotator` to provide PSI-level syntax highlighting
- Registered in `plugin.xml` as `<annotator language="MVEL" ...>`
- Processes all PSI elements in MVEL files and applies highlighting based on token types

### 2. Updated Parser Definition
- Modified `MvelTypes.Factory.createElement()` to return `null` for leaf nodes (tokens)
- This allows IntelliJ to use default token PSI elements, which work better with syntax highlighting
- Non-leaf nodes (expressions, statements) still get custom PSI elements

### 3. Dual Highlighting Approach
- **SyntaxHighlighter**: Works at lexer level (token-based)
- **Annotator**: Works at PSI level (element-based)
- Both are registered to ensure highlighting works

## How It Works

1. **Lexer** tokenizes the file into tokens (keywords, identifiers, operators, etc.)
2. **Parser** builds AST from tokens
3. **SyntaxHighlighter** applies colors based on token types from lexer
4. **Annotator** applies colors based on PSI element types (backup/fallback)

## Testing

After rebuilding and reinstalling the plugin:
1. Open `cont.mvel` or `ghor.mvel`
2. Verify syntax highlighting appears for:
   - Keywords (if, for, def, etc.) - should be colored
   - Strings - should be colored
   - Numbers - should be colored
   - Identifiers - should be colored
   - Operators (==, +, -, etc.) - should be colored
   - Comments - should be colored

## If Still Not Working

1. **Check File Association**: Ensure `.mvel` files are recognized as "MVEL File" type
2. **Invalidate Caches**: In IntelliJ, go to `File` → `Invalidate Caches...` → `Invalidate and Restart`
3. **Check Plugin Installation**: Verify plugin is enabled in `Settings` → `Plugins`
4. **Check Logs**: Look for errors in `Help` → `Show Log in Finder`

## Next Steps

If highlighting still doesn't work, we may need to:
- Check if the lexer is producing tokens correctly
- Verify the syntax highlighter is being called
- Add more detailed logging
- Check IntelliJ platform version compatibility

