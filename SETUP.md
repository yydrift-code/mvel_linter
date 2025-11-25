# Setup Instructions

## Quick Start

The Gradle wrapper script (`gradlew`) has been created. However, you need the `gradle-wrapper.jar` file to use it.

### Option 1: If you have Gradle installed

Run this command to generate the wrapper files:
```bash
gradle wrapper
```

### Option 2: Download the wrapper JAR manually

1. Download `gradle-wrapper.jar` from:
   https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar

2. Save it to: `gradle/wrapper/gradle-wrapper.jar`

3. Then you can use:
```bash
./gradlew buildPlugin
```

### Option 3: Use Homebrew to install Gradle (macOS)

```bash
brew install gradle
gradle wrapper
```

### Option 4: Use SDKMAN (cross-platform)

```bash
curl -s "https://get.sdkman.io" | bash
sdk install gradle 8.4
gradle wrapper
```

## After Setup

Once the wrapper is set up, you can:

1. **Build the plugin:**
   ```bash
   ./gradlew buildPlugin
   ```

2. **Run in sandbox IntelliJ:**
   ```bash
   ./gradlew runIde
   ```

3. **Clean build:**
   ```bash
   ./gradlew clean buildPlugin
   ```

The plugin JAR will be in `build/distributions/` after building.

