# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android project called **TopScoreErrorNotebook（状元错题集）** - an intelligent error question organization and learning management tool for Chinese K12 students. The repository contains both design documentation (at root level) and the actual Android application code.

**Key URLs:**
- Repository root: `D:\Work\AIEnhance\claude\Error-NoteBook`
- Android project: `TopScoreErrorNotebook/`

## Android Project Structure

```
TopScoreErrorNotebook/
├── app/
│   └── src/main/
│       ├── java/com/topscore/errornotebook/
│       │   ├── core/          # Shared infrastructure
│       │   │   ├── database/   # Room (dao, entity)
│       │   │   ├── network/    # Retrofit + OkHttp
│       │   │   ├── ocr/        # Aliyun OCR integration
│       │   │   ├── oss/        # Aliyun OSS
│       │   │   ├── storage/    # DataStore
│       │   │   └── image/      # Image processing, CropSelectionView
│       │   ├── data/           # Repository implementations
│       │   ├── domain/         # Domain models
│       │   ├── feature/       # Feature modules (auth, home, question, export, profile)
│       │   ├── ui/             # Activities, Fragments, ViewModels
│       │   └── di/             # Hilt modules
│       └── res/                # Resources (layout, drawable, values, navigation)
├── build.gradle.kts            # Root build config
├── gradle.properties           # Gradle settings (parallel, caching)
└── settings.gradle.kts        # Module settings
```

## Architecture

**Pattern**: MVVM + Clean Architecture (simplified)

**Layers**:
1. **UI Layer** (`ui/`) - Activities, Fragments, ViewModels with StateFlow
2. **Domain Layer** (`domain/`) - Domain models
3. **Data Layer** (`data/`) - Repository implementations
4. **Core Layer** (`core/`) - Infrastructure (network, database, OCR, OSS)

**Key Dependencies**:
- **DI**: Hilt 2.50
- **Database**: Room 2.6.1
- **Network**: Retrofit 2.9.0 + OkHttp 4.12.0 + Moshi 1.15.0
- **Image**: Coil 2.6.0, CameraX 1.3.1
- **OCR**: Aliyun OCR SDK (`com.aliyun:ocr_api20210707:3.1.3`)
- **Navigation**: Navigation Component 2.7.7

**Build Config**:
- `compileSdk` / `targetSdk`: 34
- `minSdk`: 24
- JVM target: 17

## Build Commands

```bash
cd TopScoreErrorNotebook

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Run a specific test class
./gradlew test --tests "com.topscore.errornotebook.*"

# Check dependencies
./gradlew dependencies

# Run lint
./gradlew lint
```

## Key Technical Notes

- **OCR Integration**: Uses `recognizeEduQuestionOcrWithOptions` API from Aliyun OCR SDK. Cropped images for OCR use PNG format (no compression) to preserve accuracy.
- **CropSelectionView**: Custom View for selecting question regions during capture. Supports drag and pinch-to-zoom gestures.
- **State Management**: ViewModels expose UI state via `StateFlow`. Follows unidirectional data flow.
- **Navigation**: Single Activity with multiple Fragments using Navigation Component. Uses Safe Args for type-safe argument passing.
- **Image Compression**: Camera captures compressed to 1080p max dimension, 85% quality. OCR region uses lossless PNG.

## Documentation

- [PRD](docs/错题集%20PRD_V1.0.md) - Product Requirements
- [Technical Design](docs/状元错题集_Android技术设计_v1.0.md) - Architecture and technical decisions
- [Test Design](docs/状元错题集_Android测试设计说明书_v1.0.md) - Test cases and strategy
- [OCR Debug Summary](TopScoreErrorNotebook/docs/OCR_DEBUG_SUMMARY.md) - OCR integration notes

## Git Workflow

- Branch protection: main is protected
- Commit messages follow conventional format: `feat:`, `fix:`, `docs:`, etc.
- Design documents in `docs/` are gitignored except for explicit exceptions in `.gitignore`

## Memory

Claude Code uses a persistent memory system at `C:\Users\liwei\.claude\projects\D--Work-AIEnhance-claude-Error-NoteBook\memory\`. Key facts and project context can be stored there for cross-session recall.
