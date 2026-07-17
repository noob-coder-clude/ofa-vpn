#!/usr/bin/env bash
# Gradle wrapper script for OFA VPN project
# This script is generated automatically and should not be edited manually.

# Set the distribution URL
DIST_URL="https://services.gradle.org/distributions/gradle-8.9-bin.zip"

# Set the wrapper jar path
WRAPPER_JAR="${PWD}/gradle/wrapper/gradle-wrapper.jar"

# Check if the wrapper jar exists
if [ -f "${WRAPPER_JAR}" ]; then
    # Execute the wrapper jar with the provided arguments
    exec java -jar "${WRAPPER_JAR}" "$@"
else
    echo "Error: Gradle wrapper jar not found at ${WRAPPER_JAR}"
    exit 1
fi