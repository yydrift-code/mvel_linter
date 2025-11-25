# Infinite Loop Fixes

## Problem
IntelliJ IDEA was freezing when opening `.mvel` files due to infinite loops in the parser and potential dead loops in the lexer.

## Root Causes Identified

### 1. **Parser Main Loop** (MvelParser.parse())
- **Issue**: `while (!builder.eof())` could loop infinitely if `parseExpression()` failed to advance the lexer
- **Fix**: Added check to advance lexer if parsing fails

### 2. **Parser Expression Parsing** (parseExpression())
- **Issue**: If `parseStatement()` returned false, the lexer wasn't advanced, causing infinite retry
- **Fix**: Made `parseExpression()` return boolean and handle EOF checks

### 3. **Binary Operator Parsing** (parseAssignmentOrExpression())
- **Issue**: `while (isBinaryOperator(...))` could loop infinitely if operator parsing failed
- **Fix**: Added iteration counter (max 100) to prevent infinite loops

### 4. **Property Access Parsing** (parsePrimaryExpression())
- **Issue**: `while (builder.getTokenType() == MvelTokenTypes.DOT)` could loop if identifier parsing failed
- **Fix**: Added iteration counter (max 100) to prevent infinite loops

### 5. **List/Map/Array Parsing** (parseListOrMap, parseArray, parseArgumentList)
- **Issue**: While loops could continue indefinitely if `parseExpression()` didn't advance
- **Fix**: 
  - Added iteration counters (max 1000)
  - Added check to force advance if token doesn't change after parseExpression()

### 6. **Parameter List Parsing** (parseParameterList)
- **Issue**: While loop could continue if lexer didn't advance properly
- **Fix**: Added iteration counter (max 1000) and EOF check

### 7. **Lexer Safety** (MvelLexer.advance())
- **Issue**: If tokenEnd wasn't set properly, currentOffset wouldn't advance
- **Fix**: Added safety check to ensure currentOffset always advances by at least 1

## Fixes Applied

### Parser Changes

1. **Main parse loop**: Now advances lexer if parsing fails
   ```java
   while (!builder.eof()) {
       if (!parseExpression(builder)) {
           builder.advanceLexer(); // Prevent infinite loop
       }
   }
   ```

2. **All while loops**: Added iteration counters and EOF checks
   ```java
   int iterations = 0;
   while (!builder.eof() && condition && iterations < MAX_ITERATIONS) {
       iterations++;
       // ... parsing logic
   }
   ```

3. **Recursive parsing**: Added token change detection to force advance if needed
   ```java
   IElementType tokenBefore = builder.getTokenType();
   parseExpression(builder);
   if (tokenBefore == builder.getTokenType() && !builder.eof()) {
       builder.advanceLexer(); // Force advance if stuck
   }
   ```

### Lexer Changes

1. **Safety check**: Ensures currentOffset always advances
   ```java
   if (tokenEnd <= currentOffset) {
       tokenEnd = currentOffset + 1;
       tokenType = null; // Mark as error
   }
   currentOffset = tokenEnd;
   ```

## Testing Recommendations

1. Test with malformed MVEL files
2. Test with very long files
3. Test with nested structures
4. Test with incomplete expressions
5. Monitor for any remaining freezes

## Prevention Measures

- All loops now have maximum iteration limits
- All loops check for EOF
- Parser always advances lexer, even on error
- Lexer always advances offset, even for unknown characters
- Token change detection prevents parser from getting stuck

## Status

✅ **Fixed** - All identified infinite loop issues have been resolved. The plugin should no longer freeze when opening `.mvel` files.

