#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "======================================================================="
echo "Smart Campus Complaint Prioritization and Tracking System - Launcher"
echo "======================================================================="

JAVA_CMD="java"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

JAR_FILE="target/smart-campus-complaint-system-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    if [ -f "target/smart-campus-complaint-system-1.0.0-executable.jar" ]; then
        JAR_FILE="target/smart-campus-complaint-system-1.0.0-executable.jar"
    elif [ -f "packaging/smart-campus-app.jar" ]; then
        JAR_FILE="packaging/smart-campus-app.jar"
    else
        echo "[INFO] Packaged JAR not found. Building with Maven..."
        mvn clean package -DskipTests
    fi
fi

echo "Launching application using $JAVA_CMD ..."
exec "$JAVA_CMD" --enable-native-access=ALL-UNNAMED -jar "$JAR_FILE"
