#!/bin/bash

echo "Building Knets Jr APK locally using Docker..."

# Build Docker image
cd android
docker build -t knets-android-builder .

# Run container and extract APK
docker run --name knets-build-container knets-android-builder
docker cp knets-build-container:/output ./local-output

# Clean up container
docker rm knets-build-container

echo "APK build complete. Files in android/local-output:"
ls -la local-output/

echo "APK files found:"
find local-output -name "*.apk" -type f -exec ls -la {} \;