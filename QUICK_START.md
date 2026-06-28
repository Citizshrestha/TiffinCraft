# Quick Start Guide

**⚡ Fast commands to get TiffinCraft running on your machine.**

---

## First Time Setup (5 minutes)

### 1. Set JAVA_HOME

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
```

**Mac:**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
```

**Linux:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

### 2. Create Configuration Files

```bash
# Backend
cd backend
cp .env.example .env
# Edit .env with your values

# Frontend
cd frontend
cp local.properties.template local.properties
# Edit local.properties with your Android SDK path
```

### 3. Install & Build

```bash
# Backend
cd backend
npm install
node database/run_migration.js
node server.js  # Starts on port 5000

# Frontend (new terminal)
cd frontend
./gradlew installDebug
```

---

## Daily Development

### Start Backend
```bash
cd backend
node server.js
```

### Build & Install Android App

**Windows:**
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
cd frontend
.\gradlew installDebug
```

**Mac/Linux:**
```bash
export JAVA_HOME=/path/to/jdk-21
cd frontend
./gradlew installDebug
```

---

## Common Commands

### Check Java Version
```bash
java -version  # Should show 21.x.x
```

### Check Gradle
```bash
cd frontend
./gradlew -version
```

### Clean Build
```bash
cd frontend
./gradlew clean assembleDebug
```

### Run Tests
```bash
cd frontend
./gradlew test
```

### Backend Health Check
```bash
curl http://localhost:5000/api/health
```

---

## Troubleshooting

| Error | Solution |
|-------|----------|
| JAVA_HOME not set | Set `JAVA_HOME` before running gradle |
| SDK location not found | Create `frontend/local.properties` |
| Cannot connect to MySQL | Check database is running, verify `.env` |
| Wrong Java version | Install JDK 21, update JAVA_HOME |

---

## Files You Need to Create (Not in Git)

- `backend/.env` (from `.env.example`)
- `frontend/local.properties` (from `.local.properties.template`)

---

## More Help

- **Full Setup:** See `DEVELOPMENT_SETUP.md`
- **Android Only:** See `frontend/README.md`
- **Portability:** See `PORTABILITY_CHECKLIST.md`

---

**Tip:** Set JAVA_HOME permanently in system environment variables to avoid typing it every time!
