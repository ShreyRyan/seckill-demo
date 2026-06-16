#!/bin/sh
# Maven Wrapper for Unix/Linux/macOS

MVNW_REPOURL="https://repo.maven.apache.org/maven2"
WRAPPER_VERSION="3.3.2"
MVN_VERSION="3.9.8"

DIRNAME="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIRNAME/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPS="$DIRNAME/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper $WRAPPER_VERSION ..."
    if command -v curl > /dev/null; then
        curl -L -o "$WRAPPER_JAR" "$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/$WRAPPER_VERSION/maven-wrapper-$WRAPPER_VERSION.jar"
    elif command -v wget > /dev/null; then
        wget -O "$WRAPPER_JAR" "$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/$WRAPPER_VERSION/maven-wrapper-$WRAPPER_VERSION.jar"
    else
        echo "ERROR: Neither curl nor wget found. Install one and try again."
        exit 1
    fi
fi

MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" \
    -classpath "$WRAPPER_JAR" \
    -Dmaven.multiModuleProjectDirectory="$DIRNAME" \
    -Dmaven.home= \
    -Dwrapper.properties="$WRAPPER_PROPS" \
    -Dmaven.user.home="$MAVEN_USER_HOME" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
