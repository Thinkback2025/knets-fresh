#!/bin/bash

echo "🔨 Building Protected Knets Jr Android App"
echo "=========================================="

# Navigate to Android directory
cd android

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build the APK with uninstall protection
echo "📱 Building protected APK..."
./gradlew assembleDebug

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # Find the generated APK
    APK_PATH=$(find . -name "knets-jr-debug.apk" -type f)
    
    if [ -n "$APK_PATH" ]; then
        echo "📦 APK generated at: $APK_PATH"
        
        # Copy to root directory for easy access
        cp "$APK_PATH" ../knets-jr-protected.apk
        echo "📁 APK copied to: knets-jr-protected.apk"
        
        # Show APK info
        echo ""
        echo "🛡️ PROTECTED KNETS JR APK FEATURES:"
        echo "• Device Administrator Privileges"
        echo "• Uninstall Protection with Parent Secret Code"
        echo "• Multi-Method Location Tracking:"
        echo "  - GPS (±5-50m accuracy)"
        echo "  - Network/WiFi (±50-500m accuracy)"
        echo "  - Cell Tower (±500-5000m accuracy)"
        echo "  - IP Geolocation (±10km fallback)"
        echo "• Real-time Parent Location Request Monitoring"
        echo "• Safety-focused Permission Education"
        echo "• Network Control & WiFi Management"
        echo "• Boot-time Protection Restoration"
        echo "• Parent Dashboard API Integration"
        echo "• Emergency Location Sharing with Smart Fallbacks"
        echo "• Offline Location Storage & Sync"
        echo ""
        echo "📋 INSTALLATION INSTRUCTIONS:"
        echo "1. Enable 'Unknown Sources' in Android Settings"
        echo "2. Install: adb install knets-jr-protected.apk"
        echo "3. Grant Device Administrator permissions when prompted"
        echo "4. Enter parent code to connect to dashboard"
        echo "5. Set 4-digit secret code for uninstall protection"
        echo ""
        echo "🚨 SECURITY FEATURES:"
        echo "• App cannot be uninstalled without parent secret code"
        echo "• Disabling device admin triggers parent alerts"
        echo "• Protection survives device reboots"
        echo "• Unauthorized uninstall attempts are logged"
        
    else
        echo "❌ APK not found after build"
        exit 1
    fi
else
    echo "❌ Build failed"
    exit 1
fi

echo ""
echo "🎉 Protected Knets Jr build complete!"