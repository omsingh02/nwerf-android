# Nwerf 🎵

An open-source Android music streaming app that leverages **Telegram** for unlimited free cloud storage and **Firebase Firestore** for cross-device library syncing. Designed with cutting-edge Material 3 Expressive UI using Jetpack Compose.

## ✨ Features
* **Unlimited Cloud Storage**: Uses a personal Telegram Bot and Private Channel to store and stream high-quality audio files completely free.
* **Cross-Device Syncing**: Google Sign-In automatically authenticates you and syncs your library metadata (titles, artists, file IDs) securely via Firestore.
* **Material 3 Expressive UI**: A beautiful, dynamic interface built 100% in Jetpack Compose, featuring smooth animations and a premium dark mode.
* **Music Identification**: Built-in ACRCloud integration to instantly identify any song playing around you and add it directly to your library.
* **Background Playback**: Seamless background audio playback and media session controls powered by AndroidX Media3 (ExoPlayer).

## 🚀 Getting Started

1. **Download the App**: Grab the latest APK from the [Releases](https://github.com/omsingh02/nwerf-android/releases) page.
2. **Setup your Telegram Bot**:
   * Open Telegram and message `@BotFather` to create a new bot. Copy the **Bot Token**.
   * Create a **Private Channel** and add your bot as an admin.
   * Forward any message from your channel to `@RawDataBot` to get your **Chat ID**.
3. **Sign In**: Launch the app, sign in with Google, and paste your Telegram Bot Token and Chat ID into the setup wizard.
4. **Enjoy**: Upload MP3s directly from the app, or use the "Identify" feature to find new tracks. Your music is now safely stored in Telegram and your library is synced via Firebase!

## 🛠️ Built With
* [Jetpack Compose](https://developer.android.com/jetpack/compose)
* [AndroidX Media3 (ExoPlayer)](https://developer.android.com/media/media3)
* [Firebase Authentication & Firestore](https://firebase.google.com/)
* [Telegram Bot API](https://core.telegram.org/bots/api)
* [ACRCloud](https://www.acrcloud.com/)

## 👩‍💻 Contributing & Development
If you are interested in contributing, building from source, or understanding the automated release pipeline, please read the [Development Guide](DEVELOPMENT.md).

## 📝 License
This project is open-source and available under the MIT License.
