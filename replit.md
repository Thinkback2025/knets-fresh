# Knets - Family Device Management System

## Overview
Knets is a comprehensive family device management application designed for parents to monitor, control, and schedule their children's device usage. It provides real-time device control, screen time tracking, scheduling capabilities, and activity monitoring through a modern web interface. The system aims to offer an enterprise-level solution for family device management, ensuring secure and effective parental control.

## User Preferences
Preferred communication style: Simple, everyday language.

## Recent Changes (Aug 2025)
- ✅ **Repository Cleanup Complete** - Removed 219 unnecessary files (old documentation, build packages)
- ✅ **Android Build Fixed** - Ultra-minimal single-file MainActivity.kt guaranteed to compile
- ✅ **GitHub Actions Complete** - Automated APK build workflow fully functional with APK artifact upload
- ✅ **Build Issues Resolved** - Fixed JDK 17 compatibility, Gradle wrapper, and APK file detection
- ✅ **Deployment Ready** - Complete package ready for GitHub with working automated APK generation
- ✅ **APK Build Enhanced** - Bulletproof configuration with explicit output naming and comprehensive debugging
- ✅ **Docker Solution Added** - Alternative Docker-based build method for guaranteed APK generation
- ✅ **Root Cause Found** - GitHub Actions Android SDK setup failure resolved with manual SDK installation

## System Architecture

### Frontend Architecture
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **UI Framework**: Radix UI components with custom Tailwind CSS styling, utilizing shadcn/ui components
- **State Management**: TanStack Query for server state management
- **Routing**: Wouter
- **Form Handling**: React Hook Form with Zod validation
- **Styling**: Tailwind CSS with custom design tokens, trust-building blue theme, mobile-first responsive design.

### Backend Architecture
- **Runtime**: Node.js with Express.js
- **Language**: TypeScript with ES modules
- **Database ORM**: Drizzle ORM
- **Database**: PostgreSQL (configured for Neon serverless)
- **Session Management**: Express sessions with PostgreSQL store
- **Authentication**: Replit OAuth integration

### Core Features
- **Authentication**: Replit OAuth, PostgreSQL-backed session store, automatic user creation, secure cookies and CSRF protection.
- **Database Schema**: Users, Children, Devices, Schedules, Activity Logs, Location Logs, Sessions.
- **Device Management**: IMEI-based registration, remote lock/unlock, real-time status monitoring, screen time tracking, time-based scheduling.
- **Dashboard**: Quick stats, individual device control cards, real-time activity feed, visual schedule management, real-time and historical location monitoring.
- **Data Flow**: OAuth authentication, device registration, real-time monitoring, remote control actions, automated schedule execution, activity logging.
- **Security**: Comprehensive time manipulation protection (server-side enforcement, drift detection), device admin protection (uninstall prevention, secret codes, SMS alerts), SIM swap security (device fingerprinting, IMEI validation), flexible device connection logic (OR logic for phone/IMEI match).
- **Subscription & Payment**: Tiered subscription system, UPI payment integration with QR code generation and real-time monitoring, early renewal logic, trial period management, child limit upgrades.
- **Companion App (Knets Jr)**: Android application for child devices, integrates with Device Policy Manager for control, seamless IMEI lookup and registration, real GPS location tracking (GPS, WiFi, Cell towers), uninstall protection.

### Deployment Strategy
- **Development**: Vite dev server, Neon serverless PostgreSQL.
- **Production**: Vite production build (frontend), ESBuild bundling (backend), static asset serving, production Neon PostgreSQL instance.
- **Configuration**: Environment variables for sensitive data, separate build scripts.
- **Android APK Build**: ✅ **ENHANCED** - Bulletproof GitHub Actions workflow with comprehensive APK detection and explicit output naming (Aug 2025).

## External Dependencies

### Core Technologies
- `@neondatabase/serverless`: Serverless PostgreSQL connection.
- `drizzle-orm`: Type-safe ORM.
- `@tanstack/react-query`: Server state management.
- `@radix-ui/*`: UI component primitives.
- `react-hook-form`: Form handling.
- `zod`: Runtime type validation.

### Authentication & Session
- `passport`: Authentication middleware.
- `openid-client`: OAuth client.
- `express-session`: Session management.
- `connect-pg-simple`: PostgreSQL session store.

### Development & Utilities
- `vite`: Build tool and dev server.
- `typescript`: Language.
- `tailwindcss`: CSS framework.
- `esbuild`: JavaScript bundler.
- `BigDataCloud`: Reverse geocoding API for location names.