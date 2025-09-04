#!/bin/bash

# supporting script with functions to extract three information needed for choosing the correct config
# 1. Get major OS version
get_os_major_version() {
    if [ -f /etc/redhat-release ]; then
        os_version=$(grep -oE '[0-9]+' /etc/redhat-release | head -1)
        echo "$os_version"
    elif [ -f /etc/os-release ]; then
        source /etc/os-release
        if [[ -n "$VERSION_ID" ]]; then
            echo "${VERSION_ID%%.*}"
        else
            echo "unknown"
        fi
    else
        echo "unknown"
    fi
}

# 2. Check if FIPS crypto policy is enabled true/false
get_fips_status() {
    if [ -f /proc/sys/crypto/fips_enabled ]; then
        fips=$(cat /proc/sys/crypto/fips_enabled)
        if [ "$fips" -eq 1 ]; then
            echo "true"
        else
            echo "false"
        fi
    else
        echo "false"
    fi
}

# 3. Get installed JDK major version
get_jdk_major_version() {
    java_bin=$(command -v java 2>/dev/null)

    if [ -x "$java_bin" ]; then
        version=$("$java_bin" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    else
        jdk_home="${JDK_HOME:-$JAVA_HOME}"
        if [ -n "$jdk_home" ] && [ -x "$jdk_home/bin/java" ]; then
            version=$("$jdk_home/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
        else
            echo "not found"
            return
        fi
    fi

    # Extract major version
    if [[ $version == 1.* ]]; then
        echo "${version:2:1}"
    else
        echo "${version%%.*}"
    fi
}

echo "OS_MAJOR_VERSION=$(get_os_major_version)"
echo "FIPS_ENABLED=$(get_fips_status)"
echo "JDK_MAJOR_VERSION=$(get_jdk_major_version)"
