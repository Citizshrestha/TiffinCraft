#!/bin/bash
# Environment setup for TiffinCraft frontend
# This script sets up Java and Gradle to use D: drive locations

export JAVA_HOME="D:\DevTools\Java\jdk-21.0.5.11-hotspot"
export GRADLE_USER_HOME="D:\DevTools\Gradle\.gradle"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Environment configured:"
echo "JAVA_HOME: $JAVA_HOME"
echo "GRADLE_USER_HOME: $GRADLE_USER_HOME"
echo ""
echo "Java version:"
java -version
