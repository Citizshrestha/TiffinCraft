@echo off
REM TiffinCraft Frontend Environment Setup
REM This script sets up Java and Gradle to use D: drive locations

set JAVA_HOME=D:\DevTools\Java\jdk-21.0.5.11-hotspot
set GRADLE_USER_HOME=D:\DevTools\Gradle\.gradle
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo TiffinCraft Environment Configured
echo ========================================
echo JAVA_HOME: %JAVA_HOME%
echo GRADLE_USER_HOME: %GRADLE_USER_HOME%
echo.
echo Java version:
java -version
echo.
echo Gradle version:
cd /d D:\TiffinCraft\frontend
call gradlew --version
