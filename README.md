# Mercante in Fiera

![Screenshot](/mercanteinfiera/screenshots/Screenshot.jpg)


An Android video game for the classic "Mercante in Fiera" board game.

## Description
Implementation of the "Mercante in Fiera" game (auction, elimination, prizes) using Kotlin and Jetpack Compose.

## Technologies Used
- Kotlin
- Jetpack Compose
- Architecture Components (ViewModel, StateFlow)
- Coroutines
- Firebase (Realtime Database, Authentication)
- Coil (Image loading)

## Project Structure
- `models/` - Contains data models like `CardModel`
- `data/` - Contains data and deck management logic
- `ui/` - Contains user interface composables
- `viewmodel/` - Contains ViewModels for state management
- `utils/` - Utility classes and helper functions

## Features
- Two decks of 40 cards each (Merchant and Player)
- Graphic assets for cards and back-of-card
- Flip animations to show/hide cards
- MVVM Architecture
- Multiplayer support via Firebase

## Dependencies
- Jetpack Compose
- ViewModel & StateFlow
- Coroutines
- Firebase
- Coil

## How to build the project

### Prerequisites
- Android SDK with API level 34
- Android build-tools version 34.0.0 or higher
- Java Development Kit (JDK) 17 or higher (Required by AGP 8.1.0)
- Gradle 9.0 or higher

### Installing dependencies
Ensure all necessary components are installed via Android SDK Manager:
```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Building and Running for Testing
To install and run the debug version on a connected device:

```bash
cd mercanteinfiera
./gradlew installDebug
```

### Building for distribution
To create an APK for distribution:

```bash
./gradlew assembleRelease
```

The final APK will be available in `app/build/outputs/apk/release/`.

### Recommended Development Environment
- Android Studio Flamingo or later
- Or any text editor with Kotlin and Gradle support
