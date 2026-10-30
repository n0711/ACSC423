# Provenance and assignment scope

The user identified **Poolside AR** as the ACSC423 assignment and authorized rebuilding it locally and publishing the completed result to n0711/ACSC423. No matching final-assignment rubric was found in the searched Documents, Desktop, or Downloads files. A Lab 4 exercise is present, but it is a separate exercise and was not treated as the Poolside specification. This project therefore preserves the supplied APK's observable scope and records improvements; it does not claim verified compliance with an unavailable marking rubric.

## Recovery source

- Input: poolside-ar-debug.apk
- SHA-256: 15163148ec1ad3e1577a2fd6ccd1aae4a90558e893ba9b18d0bd4c42810a7d1e
- Original package: edu.frederick.poolar
- Original version: 0.1.0-demo (version code 1), minimum API 24, target API 35.
- A second Downloads archive contained the same-named APK, not editable source.
- Source structure and behavior were inspected with Android SDK aapt/dexdump and JADX 1.5.3. The original APK and recovery tools are preserved outside this repository.

The repository and build configuration were created anew. Native UI and CSV parsing were rewritten. The model and OpenGL geometry/rendering were recovered, corrected, cleaned up, and integrated with tests. Decompiled code was reviewed; no decompiler failure stubs are shipped. This is **reconstruction**, not a claim that the original source tree or its comments were recovered exactly.

The original swimming.csv and water-polo.csv fixtures are retained byte-for-byte. They describe synthetic positions in metre-based model pools. Pool dimensions are model parameters, not assertions about regulation venues. Existing Lab3 and Lab4 repository files are preserved.

## Delivered scope

- Swimming and water-polo replays; 2D preview and optional anchored AR.
- Play/pause, restart, seek, adjustable speed, and table scale.
- Ball-memory challenge with touch and keyboard-accessible coordinate input.
- Strict input validation, indexed sampling, lifecycle handling, tests, documentation, and CI.

## Explicit limits

No live feed, video reconstruction, object detection, account system, analytics, or backend is implemented. No grading criteria beyond the user's stated goals were supplied. Real-device AR placement and walking around an anchor require a supported physical device and are not inferred from emulator results. Google Play release signing and distribution are outside this classroom debug deliverable.
