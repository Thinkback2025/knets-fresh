# Knets Jr - Android Companion App

## Overview
Knets Jr is the Android companion app for the Knets family device management system. It provides actual device control capabilities using Android's Device Policy Manager, allowing parents to remotely lock/unlock children's devices through scheduled enforcement.

## Features
- **Device Policy Manager Integration**: Actual device locking/unlocking capabilities
- **Real-time Schedule Monitoring**: Monitors parent-defined schedules automatically
- **Device Registration**: IMEI-based device registration with consent flow
- **Remote Control**: Responds to parent dashboard commands for device control
- **Background Service**: Continuous monitoring service for schedule enforcement

## Technical Stack
- **Language**: Kotlin
- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 24 (Android 7.0)
- **Device Admin**: Uses Device Policy Manager for device control
- **Architecture**: MVVM with background services

## Build Requirements
- Android SDK 34
- Kotlin 1.9+
- Gradle 8.0+
- Java 11+

## Project Structure
```
app/src/main/
├── java/com/knets/jr/
│   ├── MainActivity.kt               # Main activity with device registration
│   ├── admin/
│   │   └── KnetsDeviceAdminReceiver.kt  # Device admin receiver for policy enforcement
│   ├── service/
│   │   └── ScheduleMonitorService.kt    # Background service for schedule monitoring
│   └── api/
│       └── ApiService.kt             # API communication with parent dashboard
├── res/
│   ├── layout/                       # UI layouts
│   ├── values/                       # Strings, colors, styles
│   └── xml/
│       ├── device_admin.xml          # Device admin policy definitions
│       ├── data_extraction_rules.xml # Data backup rules
│       └── backup_rules.xml          # Backup configuration
└── AndroidManifest.xml               # App manifest with permissions
```

## Key Components

### Device Admin Receiver
- Handles device policy enforcement
- Responds to lock/unlock commands
- Manages device administration permissions

### Schedule Monitor Service
- Runs in background to monitor active schedules
- Communicates with parent dashboard API
- Automatically enforces schedule-based device control

### Main Activity
- Device registration interface
- IMEI and device information collection
- User consent and setup flow

## Permissions Required
- `BIND_DEVICE_ADMIN`: Device administration capabilities
- `INTERNET`: API communication with parent dashboard
- `WAKE_LOCK`: Background service operation
- `RECEIVE_BOOT_COMPLETED`: Auto-start after device reboot

## Build Instructions

### Using Bitrise.io (Recommended)
1. Upload this project to GitHub repository
2. Connect repository to Bitrise.io
3. Bitrise auto-detects Android project and configures build
4. Download APK from build artifacts

### Local Build
```bash
./gradlew clean
./gradlew assembleDebug
```

## Installation & Setup
1. Install APK on child's device
2. Enable device administrator permissions when prompted
3. Complete device registration with IMEI and basic information
4. App runs in background and responds to parent dashboard commands

## Security Features
- Device admin permissions required for operation
- IMEI-based device identification
- Secure API communication with parent dashboard
- User consent required for device registration

## Integration with Knets Dashboard
This app works in conjunction with the Knets web dashboard that provides:
- Family device management interface
- Schedule creation and management
- Real-time device monitoring
- Activity logging and location tracking

## Support
For technical support or questions about the Knets family device management system, please refer to the main project documentation.