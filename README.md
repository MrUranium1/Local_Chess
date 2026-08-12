# Modern Android Chess App ♔♚

A clean, modern, and feature-rich Android Chess application built with **Kotlin** and **Jetpack Compose**. Supports local Wi-Fi & Bluetooth peer-to-peer multiplayer, offline AI bot play, local pass-and-play, game timer controls, custom board themes, and match history tracking.

---

## ✨ Features

- 📶 **Local Wi-Fi Network Match**:
  - Auto-discover match rooms on the same Wi-Fi network or join directly by IP.
  - Password-protected match rooms for private games.
- 🔵 **Bluetooth Local Match**:
  - Pair and connect with nearby Bluetooth devices to play chess offline without internet.
- 🎲 **Host Color Selection**:
  - The match host can choose to play as **White**, **Black**, or **Random**. The opponent who joins is automatically assigned the opposite color.
- 🤖 **Offline Chess AI**:
  - Challenge the built-in chess engine with adjustable difficulty levels.
- 👥 **Local Pass & Play**:
  - Play face-to-face on a single phone/tablet screen.
- ⏱️ **Custom Time Controls**:
  - Choose between Bullet (1|0), Blitz (3|2, 5|0, 5|3), Rapid (10|0, 15|10), or Classical (30|0) clock settings.
- 🎨 **Board & Visual Themes**:
  - Personalize your board with themes like **Slate Dark**, **Classic Wood**, **Ocean Blue**, and **Emerald Green**.
- 📜 **Match History & PGN Records**:
  - Save completed matches locally to review PGN notation, move counts, and game results.

---

## 📱 How to Download & Install

1. Go to the **[Releases](../../releases)** section of this GitHub repository.
2. Download the latest `app-debug.apk` file under **Assets**.
3. On your Android device, open the downloaded APK and tap **Install** *(Ensure "Install from unknown sources" is enabled in your Android settings if prompted)*.

---

## 🛠️ Building from Source

### Prerequisites
- **Android Studio** (Ladybug / Jellyfish or newer)
- **JDK 17** or higher
- **Android SDK** API Level 34+

### Building the APK locally
```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY_NAME.git
cd YOUR_REPOSITORY_NAME

# Build Debug APK
gradle assembleDebug

# Output APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 Releasing on GitHub

To release your own versions on GitHub:
1. **Push your code to GitHub** using the **"Export to GitHub"** button in Google AI Studio or git push.
2. Go to your GitHub repository and click on **Releases > Draft a new release**.
3. Create a release tag (e.g., `v1.0.0`), write a short description, and attach the compiled `app-debug.apk` file from `app/build/outputs/apk/debug/`.
4. Click **Publish Release** so users can download the APK directly!

---

## 🛠️ Built With

- **Language**: 100% [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Networking**: Java Sockets, Android NSD (Network Service Discovery), Bluetooth Sockets
