# TiffinCraft Android Frontend

Android application for the TiffinCraft platform connecting home cooks with customers.

## Prerequisites

- **JDK 21** (Eclipse Adoptium, Oracle, or OpenJDK)
- **Android Studio** (latest version recommended) OR Android SDK Command Line Tools
- **Gradle** (wrapper included in project)

## Setup Instructions

### 1. Install JDK 21

Download and install JDK 21 from:
- Eclipse Adoptium: https://adoptium.net/
- Oracle: https://www.oracle.com/java/technologies/downloads/#java21
- OpenJDK: https://jdk.java.net/21/

### 2. Set JAVA_HOME Environment Variable

**Windows:**
```powershell
# Temporary (current session only)
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"

# Permanent (System Properties -> Environment Variables)
# Add JAVA_HOME pointing to your JDK installation
# Example: C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
```

**Linux/Mac:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

### 3. Configure Android SDK

Create a `local.properties` file in the frontend directory (if not exists):
```properties
sdk.dir=C\:\\Android\\Sdk  # Windows
# sdk.dir=/Users/username/Library/Android/sdk  # Mac
# sdk.dir=/home/username/Android/Sdk  # Linux
```

**Note:** This file is gitignored and machine-specific.

### 4. Verify Setup

```bash
# Check Java version
java -version  # Should show version 21

# Check Gradle can find Java
./gradlew -version
```

## Building the Project

### Option 1: Using Android Studio (Recommended)

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `frontend` directory
4. Wait for Gradle sync to complete
5. Click "Run" or press Shift+F10

### Option 2: Command Line

**Windows (PowerShell):**
```powershell
# Make sure JAVA_HOME is set (see step 2 above)
cd frontend
.\gradlew clean assembleDebug
.\gradlew installDebug
```

**Linux/Mac:**
```bash
cd frontend
./gradlew clean assembleDebug
./gradlew installDebug
```

### Temporary JAVA_HOME Override (if needed)

If you need to use a different JDK for this project only:

**Windows:**
```powershell
$env:JAVA_HOME="C:\path\to\jdk-21" ; .\gradlew installDebug
```

**Linux/Mac:**
```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew installDebug
```

## Project Structure

```
frontend/
├── app/                    # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/      # Java source files
│   │   │   ├── res/       # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/   # Instrumented tests
│   └── build.gradle.kts
├── gradle/                 # Gradle wrapper
├── build.gradle.kts        # Root build file
├── gradle.properties       # Gradle properties (no hardcoded paths!)
├── local.properties        # Local SDK path (gitignored)
└── settings.gradle.kts
```

## Important Configuration Files

### gradle.properties
- **Contains:** Gradle JVM arguments, project-wide settings
- **Should NOT contain:** Machine-specific paths like `org.gradle.java.home`
- **Version controlled:** Yes

### local.properties
- **Contains:** Android SDK path (machine-specific)
- **Should NOT be committed:** This file is in .gitignore
- **Each developer must create their own**

## Troubleshooting

### "JAVA_HOME is not set"
```bash
# Set JAVA_HOME before running gradle
$env:JAVA_HOME="C:\path\to\jdk-21"  # Windows
export JAVA_HOME=/path/to/jdk-21    # Linux/Mac
```

### "SDK location not found"
Create `local.properties` with your Android SDK path:
```properties
sdk.dir=C\:\\Android\\Sdk
```

### Gradle fails with "Unsupported class file major version"
Your JDK version is incorrect. This project requires JDK 21.

### "Could not determine java version"
Gradle cannot find your JDK. Set JAVA_HOME correctly.

## API Configuration

The app connects to a backend API. Update the base URL in:
```
app/src/main/java/com/tiffincraft/app/api/RetrofitClient.java
```

Default: `http://192.168.1.4:5000/api/`

## Building Release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## License

Copyright © 2024 TiffinCraft. All rights reserved.
