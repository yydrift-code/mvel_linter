# MVEL Linter Plugin for IntelliJ IDEA

An IntelliJ IDEA plugin that provides syntax highlighting, code inspections, and linting for Apache MVEL expression language files.

## Features

- **Syntax Highlighting**: Full syntax highlighting for MVEL expressions, keywords, operators, and literals
- **Syntax Validation**: Real-time syntax error detection using MVEL compiler
- **Type Checking**: Basic type error detection and validation
- **Best Practice Inspections**: Suggestions for improving MVEL code quality
- **File Type Recognition**: Automatic recognition of `.mvel` files

## Installation

### Building from Source

1. Clone this repository:
```bash
git clone <repository-url>
cd mvel_linter
```

2. Build the plugin using Gradle:
```bash
./gradlew buildPlugin
```

3. Install the plugin in IntelliJ IDEA:
   - Open IntelliJ IDEA
   - Go to `File` → `Settings` → `Plugins`
   - Click the gear icon → `Install Plugin from Disk...`
   - Select the generated plugin JAR file from `build/distributions/`

### Development Setup

1. Open the project in IntelliJ IDEA
2. Import the Gradle project
3. Run the `runIde` Gradle task to launch a sandbox IntelliJ instance with the plugin installed

## Usage

1. Create or open a `.mvel` file in IntelliJ IDEA
2. The plugin will automatically:
   - Apply syntax highlighting
   - Show syntax errors in real-time
   - Display inspection warnings and suggestions

## Supported MVEL Features

The plugin supports MVEL 2.0 syntax including:

- Property expressions (`user.name`)
- Boolean expressions (`user.name == 'John'`)
- Method invocations
- Variable assignments
- Function definitions
- Control flow (if/else, for, foreach, while, do-while)
- Inline collections (lists, maps, arrays)
- Type coercion
- Null-safe navigation
- Lambda expressions

## Inspections

The plugin includes three types of inspections:

1. **Syntax Errors**: Detects compilation errors in MVEL expressions
2. **Type Errors**: Warns about potential type mismatches and unresolved variables
3. **Best Practices**: Suggests improvements like:
   - Using `empty` literal instead of `null` for emptiness checks
   - Consistent string literal delimiters
   - Proper null-safe navigation

## Configuration

You can configure the inspections in:
`File` → `Settings` → `Editor` → `Inspections` → `MVEL`

## Requirements

- IntelliJ IDEA 2023.2 or later
- Java 17 or later

## License

This plugin is provided as-is for use with Apache MVEL.

## References

- [MVEL Documentation](http://mvel.documentnode.com/)
- [IntelliJ Platform Plugin Development](https://plugins.jetbrains.com/docs/intellij/welcome.html)

