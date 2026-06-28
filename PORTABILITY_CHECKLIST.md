# Project Portability Checklist

This document ensures the TiffinCraft project can be set up on any machine without hardcoded paths.

## ✅ Completed Actions

### 1. Removed Hardcoded JDK Path
- **File:** `frontend/gradle.properties`
- **Removed:** `org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.11.10-hotspot`
- **Solution:** Gradle now uses system `JAVA_HOME` environment variable

### 2. Created Template Files
- ✅ `frontend/local.properties.template` - Template for Android SDK path
- ✅ `backend/.env.example` - Template for environment variables (already exists)

### 3. Documentation Created
- ✅ `frontend/README.md` - Frontend-specific setup guide
- ✅ `DEVELOPMENT_SETUP.md` - Complete project setup guide
- ✅ `PORTABILITY_CHECKLIST.md` - This file

### 4. Verified .gitignore
- ✅ `frontend/local.properties` - Excluded ✓
- ✅ `backend/.env` - Should be excluded
- ✅ `backend/node_modules/` - Should be excluded
- ✅ Build outputs - Excluded ✓

---

## 📋 Setup Instructions for New Machines

### Prerequisites
1. Install JDK 21
2. Install Node.js 18+
3. Install MySQL 8.0+
4. Install Android SDK

### Quick Setup (5 minutes)

```bash
# 1. Clone repository
git clone <repo-url>
cd TiffinCraft

# 2. Set JAVA_HOME (required for Android build)
# Windows:
$env:JAVA_HOME="C:\path\to\your\jdk-21"

# Linux/Mac:
export JAVA_HOME=/path/to/your/jdk-21

# 3. Configure backend
cd backend
cp .env.example .env
# Edit .env with your values
npm install

# 4. Setup database
mysql -u root -p
CREATE DATABASE tiffincraft;
exit
node database/run_migration.js

# 5. Configure frontend
cd ../frontend
cp local.properties.template local.properties
# Edit local.properties with your Android SDK path

# 6. Build frontend
./gradlew clean assembleDebug

# Done!
```

---

## 🔍 Verification Checklist

Before committing code, ensure:

- [ ] No absolute paths in `gradle.properties`
- [ ] No `org.gradle.java.home` in `gradle.properties`
- [ ] `local.properties` is in `.gitignore`
- [ ] `.env` is in `.gitignore`
- [ ] No machine-specific paths in `.gradle.kts` files
- [ ] No hardcoded IP addresses (use environment variables)
- [ ] Template files exist for all machine-specific configs

---

## 📁 File Categories

### ✅ Version Controlled (Commit These)
```
✓ gradle.properties (no hardcoded paths!)
✓ build.gradle.kts
✓ settings.gradle.kts
✓ *.template files
✓ .gitignore files
✓ Source code (*.java, *.kt, *.xml)
✓ Documentation (*.md)
```

### ❌ NOT Version Controlled (Do Not Commit)
```
✗ local.properties (machine-specific)
✗ .env (contains secrets)
✗ node_modules/
✗ build/
✗ .idea/ (except .gitignore allows some)
✗ *.iml
```

### 📝 Developer Creates from Template
```
→ local.properties (from local.properties.template)
→ .env (from .env.example)
```

---

## 🚀 Running on Different Machines

### Windows
```powershell
# Set JAVA_HOME for current session
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"

# Build
cd frontend
.\gradlew installDebug
```

### Mac
```bash
# Set JAVA_HOME for current session
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# Build
cd frontend
./gradlew installDebug
```

### Linux
```bash
# Set JAVA_HOME for current session
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Build
cd frontend
./gradlew installDebug
```

---

## 🔧 Permanent JAVA_HOME Setup

### Windows (System-wide)
1. Open System Properties → Advanced → Environment Variables
2. Add new System Variable:
   - Name: `JAVA_HOME`
   - Value: `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`
3. Restart terminals/IDE

### Mac (Permanent)
Add to `~/.zshrc` or `~/.bash_profile`:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

Then: `source ~/.zshrc`

### Linux (Permanent)
Add to `~/.bashrc`:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

Then: `source ~/.bashrc`

---

## 🐛 Troubleshooting

### Error: "JAVA_HOME is not set"
**Solution:** Set JAVA_HOME before running gradle:
```bash
$env:JAVA_HOME="C:\path\to\jdk-21"  # Windows
export JAVA_HOME=/path/to/jdk-21    # Linux/Mac
```

### Error: "SDK location not found"
**Solution:** Create `frontend/local.properties`:
```properties
sdk.dir=C\:\\Android\\Sdk  # Windows
# or
sdk.dir=/Users/you/Library/Android/sdk  # Mac
```

### Error: "Could not determine java version"
**Solution:** Verify JDK 21 is installed and JAVA_HOME points to it:
```bash
echo $env:JAVA_HOME  # Windows
echo $JAVA_HOME      # Linux/Mac
java -version        # Should show version 21
```

---

## ✨ Best Practices

1. **Never hardcode paths** - Use environment variables or relative paths
2. **Use template files** - Provide `.template` or `.example` files for configs
3. **Document setup** - Clear README with step-by-step instructions
4. **Version control gitignore** - Ensure machine-specific files are excluded
5. **Test on clean machine** - Verify setup works from scratch
6. **Update documentation** - Keep setup guides current with changes

---

## 📞 Support

If you encounter issues during setup:
1. Check this checklist
2. Read `DEVELOPMENT_SETUP.md`
3. Verify all prerequisites are installed
4. Contact the development team

---

**Last Updated:** January 2025
**Portable:** ✅ Yes - No hardcoded paths remain
