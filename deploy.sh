#!/bin/bash

# Read current version from build.gradle
current_version_code=$(grep "versionCode" app/build.gradle | sed 's/.*versionCode \([0-9]*\).*/\1/')
current_version_name=$(grep "versionName" app/build.gradle | sed 's/.*versionName "\([^"]*\)".*/\1/')

echo "Current version: $current_version_name (code: $current_version_code)"
echo ""

# Ask for version name
read -p "Enter new version name (current: $current_version_name): " version_name
if [ -z "$version_name" ]; then
    echo "Error: Version name is required. Exiting."
    exit 1
fi

# Ask for version code
read -p "Enter new version code (current: $current_version_code): " version_code
if [ -z "$version_code" ]; then
    echo "Error: Version code is required. Exiting."
    exit 1
fi

# Update version in build.gradle
echo "Updating version in build.gradle..."
sed -i '' "s/versionCode [0-9]*/versionCode $version_code/" app/build.gradle
sed -i '' "s/versionName \".*\"/versionName \"$version_name\"/" app/build.gradle

# Create build directory if it doesn't exist
mkdir -p build

# Build release APK
echo "Building release APK..."
./gradlew assembleRelease

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful!"
    
    # Copy APK to build folder
    cp app/build/outputs/apk/release/app-release.apk build/CallHelper.apk
    echo "APK copied to build/CallHelper.apk"
else
    echo "Build failed! Please check the error messages above."
    exit 1
fi
