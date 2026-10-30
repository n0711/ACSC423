# Poolside AR

An Android classroom app for exploring synthetic swimming and water-polo replays. Run it without a camera in 2D, or place a miniature pool on a real table with ARCore. Pause, seek, change speed, and test your memory with the Find ball challenge.

This is a reconstructed and improved source project based on the supplied Poolside AR demo APK. It does **not** infer athlete positions from video, connect to a livestream, or claim to contain real match data. See [provenance and scope](docs/PROVENANCE.md).

## Open and run

Open this **PoolsideAR** folder in Android Studio, select JDK 17 for Gradle, and install Android SDK Platform 36 and Build Tools 35.0.0 when prompted. The wrapper pins Gradle 8.13 and Android Gradle Plugin 8.11.1. Run the **app** configuration on Android 7.0 / API 24 or later. AR additionally needs a supported device and Google Play Services for AR.

On Windows, you can double-click **Open-Poolside-Android-Studio.bat** in the repository root. The root [README](../README.md) also identifies the project and download locations.

Command line, from this directory:

~~~powershell
.\gradlew.bat :core:test :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
~~~

On macOS/Linux replace the wrapper with ./gradlew. Set ANDROID_HOME to your Android SDK, or let Android Studio create the ignored local.properties file. Java 17 must be available through JAVA_HOME. First build needs internet to obtain dependencies; bundled replays then work offline. ARCore installation/device-profile checks may need internet.

The debug APK is **app/build/outputs/apk/debug/app-debug.apk**. Its application ID is **edu.frederick.poolar.dev**, allowing it to coexist with the original APK, which may use another signing key. The release application ID remains **edu.frederick.poolar**; release signing is deliberately not configured. Do not commit private signing keys.

## Use the app

1. Choose Water polo or Swimming. The labels R/B identify teams independently of color; swimmers have lane numbers.
2. Pause or restart playback, drag the timeline, or choose 0.5x, 1x, 1.5x, or 2x speed. Replays loop.
3. For water polo, choose Find ball. The replay freezes and the ball disappears. Tap its remembered location or use Enter guess; a guess within 2 metres is a success. Play, Restart, or seeking cancels the round.
4. Choose View in AR when desired. Grant camera access, scan a textured horizontal surface in good light, and tap to place. Adjust table length between 0.40 and 1.20 m. Place again resets placement. Return to 2D preview at any time.
5. Help explains data provenance, controls, and the Google AR privacy link.

Rotation restores the selected sport, timeline position, speed, paused/playing state, and table scale. Rotation returns to preview and cancels an active challenge; an AR anchor cannot be restored across a new session. Backgrounding freezes replay time and releases the active camera session until the app returns.

## Project guide

| Location | Purpose |
| --- | --- |
| core/ | Android-independent Java model, strict CSV parser, playback, challenge, geometry, unit tests |
| app/ | Native Android UI, accessible 2D preview, AR lifecycle and OpenGL renderer, instrumented tests |
| app/src/main/assets/fixtures/ | Original synthetic replay fixtures |
| docs/ARCHITECTURE.md | Design, threading, validation, failure behavior |
| docs/COMPLEXITY.md | Big-O derivations and benchmark method |
| docs/UI_EFFECTIVENESS.md | Usability decisions, assessment, and remaining user evaluation |
| docs/TESTING.md | Reproduction commands, results, and hardware limits |
| docs/PROVENANCE.md | Recovery evidence and assignment scope |

The GitHub workflow builds the APK, runs core tests/lint, and runs the UI suite in an API 35 emulator. Local evidence and hardware limitations are documented separately; an emulator pass is not proof of real-table AR tracking quality.
