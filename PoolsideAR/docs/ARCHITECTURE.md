# Implementation and architecture

## Data flow

~~~mermaid
flowchart LR
  CSV[Bundled synthetic CSV] --> Parser[ReplayCsv validation]
  Parser --> Replay[Immutable indexed frames]
  Replay --> State[ViewerState and Playback]
  State --> Preview[Android Canvas preview]
  State --> Mesh[SceneMesh]
  Mesh --> Renderer[OpenGL renderer]
  ARCore[ARCore session and anchor] --> Renderer
  Controls[Playback and challenge controls] --> State
~~~

## Separation of concerns

The core module has no Android imports. Entity and Frame represent immutable samples; Frame maintains an ID index. Replay chooses bracketing frames through binary search and interpolates matching entities with floating-point arithmetic. Confidence below 0.5 hides an entity. A gap greater than 1500 ms is deliberately not interpolated: inventing a trajectory across missing data would misrepresent the sample.

ReplayCsv accepts a documented, unquoted seven-column format. It validates identifiers, finite coordinates, confidence, pool bounds, sport-compatible kinds, duplicate IDs, a single ball per frame, stable kinds, monotonic timestamps, and at least two frames starting at zero. Bounds are 100,000 rows, 64 entities per frame, 512 characters per line, and one day of timestamps. The caller retains ownership of the Reader. Read errors have actionable messages and line numbers when a row can be identified.

Playback uses monotonic nanoseconds and converts elapsed time to milliseconds. Pause, seek, and speed changes retain the current position. A position beyond the duration loops; the exact endpoint can still be inspected. ViewerState synchronizes mutations and snapshots so the UI and GL threads see consistent model state. BallChallenge captures the target once and allows one finite in-bounds guess; an invalid guess does not consume a round.

## Android lifecycle and failure recovery

The activity loads only small bundled fixtures, then presents preview without requesting camera access. Camera permission is requested only after View in AR. AR support and installation are checked before creating/resuming a Session. Denial, unavailable ARCore, and session errors leave preview available. Permanent denial offers a Settings action.

Rendering pauses before Session.pause, and the anchor/session are released after the renderer stops during destruction. Support callbacks check foreground/destroyed state before activating AR. A latest-tap atomic slot bounds input buffering; stale taps are discarded while tracking is paused. One anchor is retained at a time. Reset detaches it on the GL thread. UI messages from rendering are posted to the main thread and identical tracking messages are deduplicated.

The preview maps x/z pool metres to an aspect-correct rectangle; ball guesses invert that transform. AR guesses transform a plane hit into anchor-local metres and apply the table-to-pool scale. ARCore manages camera and tracking; the app draws its own simple mesh. A paused frame's mesh is cached, and its direct GPU upload buffer is reused when capacity permits. Active animated frames still allocate mesh geometry; see performance limits in COMPLEXITY.md.

State survives activity recreation through Bundle primitives. For reliable recovery, recreation returns to preview and discards the old anchor/challenge. Backgrounding freezes playback while remembering whether it should resume. Window insets keep controls clear of system bars. Portrait controls scroll. Standard landscape shows the complete pool next to an independently scrolling control pane; small-height or enlarged-text landscape uses a single scrollable column.

## Privacy and dependencies

No network permission, backend endpoint, camera recording, or upload code is present in the app. Google Play Services for AR has its own behavior and privacy terms; the in-app Help links to its privacy information. Backups of app preferences are disabled. Debug and original packages coexist. Development dependency versions are pinned; version-update lint advisories remain visible rather than suppressed.

Official references consulted: [Enable AR](https://developers.google.com/ar/develop/java/enable-arcore), [ARCore Session](https://developers.google.com/ar/reference/java/com/google/ar/core/Session), [AR privacy requirements](https://developers.google.com/ar/develop/privacy-requirements).
