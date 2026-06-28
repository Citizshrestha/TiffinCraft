# TiffinCraft Development Setup Guide

Complete guide for setting up the TiffinCraft project on any machine.

## Prerequisites Checklist

- [ ] JDK 21 installed
- [ ] Node.js 18+ installed (for backend)
- [ ] MySQL 8.0+ installed
- [ ] Android SDK installed (for mobile development)
- [ ] Git installed

---

## 1. Clone the Repository

```bash
git clone <repository-url>
cd TiffinCraft
```

---

## 2. Backend Setup

### 2.1 Install Dependencies

```bash
cd backend
npm install
```

### 2.2 Configure Database

Create MySQL database:
```sql
CREATE DATABASE tiffincraft;
```

### 2.3 Configure Environment

Create `.env` file in `backend/` directory:
```env
# Server
PORT=5000
CLIENT_URL=http://localhost:3000,http://192.168.1.4:3000

# Database
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password_here
DB_NAME=tiffincraft
DB_PORT=3306

# JWT
JWT_SECRET=your_jwt_secret_here_minimum_32_characters

# Email (SMTP)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your_email@gmail.com
SMTP_PASS=your_app_password_here

# OAuth (Optional)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
FACEBOOK_APP_ID=your_facebook_app_id
FACEBOOK_APP_SECRET=your_facebook_app_secret
```

### 2.4 Run Database Migrations

```bash
cd backend
node database/run_migration.js
```

### 2.5 Start Backend Server

```bash
cd backend
node server.js
```

Server should start on http://localhost:5000

---

## 3. Frontend (Android) Setup

### 3.1 Set JAVA_HOME

**Windows (PowerShell):**
```powershell
# Check if JDK 21 is installed
java -version

# Set JAVA_HOME for current session
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"

# Verify
$env:JAVA_HOME
```

**Windows (Command Prompt):**
```cmd
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
```

**Linux/Mac:**
```bash
# Add to ~/.bashrc or ~/.zshrc for permanent setup
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# Reload
source ~/.bashrc  # or source ~/.zshrc
```

### 3.2 Configure Android SDK

1. Install Android Studio or Android SDK Command Line Tools
2. Create `frontend/local.properties`:

**Windows:**
```properties
sdk.dir=C\:\\Android\\Sdk
```

**Mac:**
```properties
sdk.dir=/Users/username/Library/Android/sdk
```

**Linux:**
```properties
sdk.dir=/home/username/Android/Sdk
```

### 3.3 Update API Base URL

Edit `frontend/app/src/main/java/com/tiffincraft/app/api/RetrofitClient.java`:

```java
public class RetrofitClient {
    // Update this to your backend server IP
    private static final String BASE_URL = "http://YOUR_IP_ADDRESS:5000/api/";
    
    // Examples:
    // Local development: "http://192.168.1.4:5000/api/"
    // Production: "https://api.tiffincraft.com/api/"
}
```

### 3.4 Build and Install

**Option A: Android Studio**
1. Open `TiffinCraft/frontend` in Android Studio
2. Let Gradle sync complete
3. Connect device or start emulator
4. Click Run (Shift+F10)

**Option B: Command Line**

```bash
cd frontend

# Windows
.\gradlew clean installDebug

# Linux/Mac
./gradlew clean installDebug
```

---

## 4. Verify Setup

### 4.1 Check Backend

```bash
curl http://localhost:5000/api/health
```

Expected response:
```json
{
  "status": "ok",
  "database": "connected",
  "timestamp": "2024-..."
}
```

### 4.2 Check Frontend Build

```bash
cd frontend
./gradlew build --dry-run
```

Should complete without errors.

---

## 5. Common Issues

### Issue: "JAVA_HOME is not set"

**Solution:**
```bash
# Set JAVA_HOME before running gradle
$env:JAVA_HOME="C:\path\to\jdk-21"  # Windows PowerShell
export JAVA_HOME=/path/to/jdk-21    # Linux/Mac
```

### Issue: "SDK location not found"

**Solution:** Create `frontend/local.properties` with your SDK path.

### Issue: "Cannot connect to MySQL"

**Solution:**
1. Verify MySQL is running: `mysql -u root -p`
2. Check database exists: `SHOW DATABASES;`
3. Verify credentials in `backend/.env`

### Issue: "Cannot connect to backend from Android"

**Solution:**
1. Make sure backend is running on `0.0.0.0` not `localhost`
2. Use your machine's IP address, not `localhost` or `127.0.0.1`
3. For emulator, use `10.0.2.2` instead of `localhost`
4. Update `RetrofitClient.java` with correct IP

### Issue: "Gradle build fails with version error"

**Solution:** Verify JDK 21 is being used:
```bash
java -version  # Should show version 21
./gradlew -version  # Should show Gradle using JVM 21
```

---

## 6. Development Workflow

### Starting Development

1. **Start Backend:**
   ```bash
   cd backend
   node server.js
   ```

2. **Start Frontend:**
   ```bash
   cd frontend
   # Set JAVA_HOME if needed
   $env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
   .\gradlew installDebug
   ```

### Making Changes

- **Backend:** Changes require server restart
- **Frontend:** Rebuild and reinstall APK

---

## 7. Project Portability Rules

### ✅ DO Commit:
- `gradle.properties` (without machine-specific paths)
- `build.gradle.kts` files
- Source code
- Resource files
- `.gitignore` files
- Template files (`.template` extension)

### ❌ DO NOT Commit:
- `local.properties` (machine-specific)
- `.env` files (contains secrets)
- `node_modules/` (dependencies)
- Build outputs (`build/`, `app/build/`)
- IDE files (`.idea/`, `*.iml`)
- Generated files

### Environment-Specific Files:
- Use `.template` files as examples
- Each developer creates their own from template
- Never hardcode absolute paths in version-controlled files

---

## 8. Team Setup

### New Developer Onboarding:

1. **Clone Repository**
   ```bash
   git clone <repo-url>
   cd TiffinCraft
   ```

2. **Copy Template Files**
   ```bash
   # Backend
   cp backend/.env.example backend/.env
   # Edit backend/.env with your values
   
   # Frontend
   cp frontend/local.properties.template frontend/local.properties
   # Edit frontend/local.properties with your SDK path
   ```

3. **Set JAVA_HOME**
   - Add to system environment variables (permanent)
   - Or set before each gradle command (temporary)

4. **Install Dependencies**
   ```bash
   cd backend
   npm install
   ```

5. **Setup Database**
   ```bash
   mysql -u root -p
   CREATE DATABASE tiffincraft;
   exit
   
   cd backend
   node database/run_migration.js
   ```

6. **Build & Run**
   ```bash
   # Terminal 1: Backend
   cd backend
   node server.js
   
   # Terminal 2: Frontend
   cd frontend
   ./gradlew installDebug
   ```

---

## 9. Production Deployment

### Backend:
- Use environment variables, not `.env` file
- Enable HTTPS
- Use production database
- Set `NODE_ENV=production`

### Frontend:
- Update BASE_URL to production API
- Build release APK: `./gradlew assembleRelease`
- Sign APK with release keystore
- Upload to Play Store

---

## Support

For issues or questions, contact the development team or create an issue in the repository.
