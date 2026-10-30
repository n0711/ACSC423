# Testing and verification

Recorded on **30 October 2026**. Machine-readable summary: [verification.json](verification.json).

| Check | Result |
| --- | --- |
| Java core regression tests | 48 passed, 0 failed |
| Android instrumented UI tests | 11 passed, 0 failed on API 35 x86_64 emulator |
| Debug APK assembly | Passed |
| Android lint | 0 errors; 7 version/target advisories |
| Core line coverage | 322 / 337 lines, 95.55% |
| Core branch coverage | 187 / 222 branches, 84.23% |
| Core instruction coverage | 2320 / 2400 instructions, 96.67% |
| APK signature verification | Passed, v2 signing scheme |
| Manual camera-denial flow | Denial returned to usable preview with explanation |
| Portrait and 150% text rendering | Visually inspected; controls readable and reachable |
| Landscape | Complete pool beside controls, with automated visibility regression |

The seven lint warnings report newer Gradle/dependency versions and retained target SDK 35. No lint baseline suppresses errors. Gradle also reports deprecated behavior from the pinned toolchain. API 24 is the declared minimum, but API 24 was not emulator-tested in this session.

## Reproduce

From PoolsideAR, with JDK 17 and Android SDK configured:

~~~powershell
.\gradlew.bat :core:test :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :core:benchmark
~~~

The connected test command requires a booted emulator/device. Use a fresh emulator or a debug install without camera permission for the camera-free-launch assertion. The test runner may remove its debug installation after completion; reinstall the app APK for manual use. The original edu.frederick.poolar package remains separate.

Generated reports:

- core/build/reports/tests/test/index.html
- core/build/reports/jacoco/test/html/index.html
- app/build/reports/lint-results-debug.html
- app/build/reports/androidTests/connected/debug/index.html

The CI workflow repeats core tests, lint, assembly, and instrumented tests on Linux with an API 35 emulator, then uploads APK/report artifacts. A workflow definition is not proof of a successful remote run; inspect the actual GitHub Actions result.

## Coverage scope

Core tests verify fractional interpolation, binary search against known trajectories, endpoints/clamping, gaps, confidence thresholds, missing entities, immutability, malformed and bounded CSV inputs, bundled fixtures, pause/resume, speed, looping, seek, challenge bounds and one-guess rules, and finite mesh geometry. Coverage refers only to core Java; it is not whole-app coverage.

UI tests cover preview launch without requesting camera, pause/resume, sport-dependent challenge availability, accessible coordinate guesses, validation recovery, recreation, seek persistence, background/manual pause, landscape visibility, Help, and restart.

## Artifact

- File: poolside-ar-improved-debug.apk (local copy beside the original course APK)
- Version: 1.0.0, version code 2
- Package: edu.frederick.poolar.dev
- Size: 1024209 bytes
- SHA-256: cb44450ad742f019376ad9e26d255b9463e58c2362b4afb829438550188966b7

Debug signing is for local/classroom testing. A rebuild or CI build may have a different byte hash because signing identity and build metadata differ. The recorded checksum identifies the actual local artifact, not all possible rebuilds.

## Required real-device follow-up

Not executed: real table plane discovery, stable world anchoring while walking, hit-test accuracy, recovery across poor lighting, AR frame-rate/thermal/battery profiling, and TalkBack testing with users. Google Play Services for AR is a runtime prerequisite for AR mode. Emulator preview/UI success does not prove these hardware behaviors. A camera-free preview is available regardless.

No original final-assignment rubric was recovered; functional tests and documentation cannot establish a mark or guarantee perfection.
