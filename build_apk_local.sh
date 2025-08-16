#!/bin/bash

echo "🔧 Local Android APK Build Script"
echo "=================================="

# Check if we're in Replit or similar environment without full Android SDK
if [ ! -d "$ANDROID_HOME" ] || [ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "❌ Full Android SDK not available in this environment"
    echo ""
    echo "📝 This script is for local development with Android Studio installed."
    echo ""
    echo "🚀 For building APK in this environment, use GitHub Actions workflow:"
    echo "   1. Push to GitHub repository"
    echo "   2. Go to Actions tab"
    echo "   3. Run 'Build Knets Jr Android APK' workflow"
    echo "   4. Download APK from artifacts"
    echo ""
    echo "📦 The standalone Android package (knets-jr-android-standalone.tar.gz)"
    echo "   contains everything needed for GitHub Actions build."
    echo ""
    exit 1
fi

echo "✅ Android SDK found at: $ANDROID_HOME"

# Navigate to android directory
cd android || exit 1

echo "🔧 Accepting Android SDK licenses..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1

echo "📦 Installing required SDK components..."
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-33" "build-tools;33.0.1" > /dev/null 2>&1

echo "🧹 Cleaning previous builds..."
./gradlew clean

echo "🔨 Building Debug APK..."
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # Find the generated APK
    APK_PATH=$(find . -name "knets-jr-debug.apk" -type f)
    
    if [ -n "$APK_PATH" ]; then
        echo "📦 APK generated at: $APK_PATH"
        
        # Copy to root directory for easy access
        cp "$APK_PATH" ../knets-jr-local-debug.apk
        echo "📁 APK copied to: knets-jr-local-debug.apk"
        
        # Show APK info
        echo ""
        echo "🚀 LOCAL BUILD COMPLETE!"
        echo "========================"
        echo "APK: knets-jr-local-debug.apk"
        echo "Size: $(ls -lh ../knets-jr-local-debug.apk | awk '{print $5}')"
        echo ""
        echo "🛡️ FEATURES INCLUDED:"
        echo "• Auto-enable location (Uber/Ola style)"
        echo "• Device administrator protection"
        echo "• Uninstall prevention"
        echo "• Multi-method location tracking"
        echo "• Real-time parent dashboard integration"
        echo ""
        echo "📲 INSTALLATION:"
        echo "adb install knets-jr-local-debug.apk"
        
    else
        echo "❌ APK not found after build"
        exit 1
    fi
else
    echo "❌ Build failed"
    exit 1
fi