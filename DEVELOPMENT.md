# Development Guide

## Project Structure

```
mvel_linter/
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts        # Gradle settings
├── gradle.properties          # Gradle properties
├── README.md                  # User documentation
├── DEVELOPMENT.md             # This file
├── test.mvel                  # Sample MVEL file for testing
├── src/
│   └── main/
│       ├── java/
│       │   └── com/mvel/linter/
│       │       ├── MvelLanguage.java          # Language definition
│       │       ├── MvelFileType.java          # File type definition
│       │       ├── lexer/
│       │       │   ├── MvelLexer.java         # Main lexer interface
│       │       │   ├── _MvelLexer.java        # Lexer implementation
│       │       │   └── MvelTokenTypes.java   # Token type definitions
│       │       ├── parser/
│       │       │   ├── MvelParser.java        # Parser implementation
│       │       │   ├── MvelParserDefinition.java  # Parser definition
│       │       │   └── MvelTypes.java          # AST node types
│       │       ├── psi/
│       │       │   ├── MvelFile.java          # PSI file implementation
│       │       │   ├── MvelElement.java       # Base PSI element
│       │       │   └── impl/
│       │       │       ├── MvelElementImpl.java
│       │       │       └── MvelExpressionImpl.java
│       │       ├── inspections/
│       │       │   ├── MvelSyntaxInspection.java      # Syntax error detection
│       │       │   ├── MvelTypeInspection.java        # Type error detection
│       │       │   └── MvelBestPracticeInspection.java # Best practices
│       │       └── highlighter/
│       │           ├── MvelSyntaxHighlighter.java    # Syntax highlighting
│       │           └── MvelColorSettingsPage.java     # Color settings
│       └── resources/
│           └── META-INF/
│               └── plugin.xml                 # Plugin manifest
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties
```

## Building the Plugin

### Prerequisites
- Java 17 or later
- IntelliJ IDEA 2023.2 or later (for development)

### Build Commands

```bash
# Build the plugin
./gradlew buildPlugin

# Run plugin in sandbox IntelliJ instance
./gradlew runIde

# Clean build
./gradlew clean buildPlugin
```

The plugin JAR will be generated in `build/distributions/`.

## Testing

1. Run `./gradlew runIde` to launch a sandbox IntelliJ instance
2. Create a new `.mvel` file
3. Test syntax highlighting and inspections

## Key Components

### Lexer (`_MvelLexer.java`)
- Tokenizes MVEL source code
- Recognizes keywords, operators, literals, identifiers
- Handles comments and whitespace

### Parser (`MvelParser.java`)
- Builds Abstract Syntax Tree (AST) from tokens
- Handles expressions, statements, control flow
- Supports MVEL 2.0 syntax

### Inspections
- **MvelSyntaxInspection**: Uses MVEL compiler to detect syntax errors
- **MvelTypeInspection**: Detects type errors and unresolved variables
- **MvelBestPracticeInspection**: Suggests code improvements

### Syntax Highlighter
- Provides color coding for different token types
- Configurable through IntelliJ color settings

## Extending the Plugin

### Adding New Inspections

1. Create a new class extending `LocalInspectionTool`
2. Implement `buildVisitor()` method
3. Register in `plugin.xml`:

```xml
<localInspection language="MVEL" 
                shortName="MyInspection" 
                displayName="My Inspection" 
                groupName="MVEL" 
                enabledByDefault="true"
                implementationClass="com.mvel.linter.inspections.MyInspection"/>
```

### Adding New Token Types

1. Add token type to `MvelTokenTypes.java`
2. Update `_MvelLexer.java` to recognize the token
3. Add highlighting in `MvelSyntaxHighlighter.java`

### Extending the Parser

1. Add new AST node type to `MvelTypes.java`
2. Add parsing logic in `MvelParser.java`
3. Create corresponding PSI element if needed

## Debugging

- Use IntelliJ's built-in debugger
- Set breakpoints in inspection classes
- Check IntelliJ logs: `Help` → `Show Log in Finder` (macOS)

## Known Limitations

1. The lexer uses regex-based tokenization which may not handle all edge cases
2. Type checking is basic and may not catch all type errors
3. Some MVEL 2.0 advanced features may not be fully supported

## Future Enhancements

- [ ] Code completion
- [ ] Refactoring support
- [ ] Better type inference
- [ ] Template support (MVEL templating)
- [ ] Integration with MVEL runtime for better error messages

