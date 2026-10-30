# Big-O analysis

Let F be the number of frames, E the maximum entities per frame, R the number of CSV rows, C the total input characters, V the vertices in a rendered mesh, and P the planes returned by ARCore. Pool decorations and sphere tessellation use fixed counts. With that fixed tessellation, V = O(E + 1).

| Operation | Time | Extra working space | Reason |
| --- | --- | --- | --- |
| Parse CSV | Expected O(C + R) | O(R) retained model | Each character and row is read once; hash indexes validate IDs. Line/entity counts are bounded. |
| Construct Frame | Expected O(E) | O(E) | Defensive list copy and ID hash index. |
| Entity lookup | Expected O(1), conservative O(E) worst case | O(1) | HashMap lookup; expected bounds assume well-distributed hashes. |
| Sample interpolated replay | Expected O(log F + E) | O(E) | Binary search plus one indexed lookup per entity, plus the output frame/index. |
| Sample exact timestamp | Expected O(log F + E) | O(E) | Binary search and confidence filtering. |
| Playback position, pause, speed, seek | O(1) | O(1) | Monotonic arithmetic and scalar updates. |
| Start challenge | Expected O(log F + E) | O(E) temporary, O(1) challenge | Sample once, scan for the ball, store its target. |
| Submit a guess | O(1) | O(1) | Bounds, one distance calculation, one answered flag. |
| Build scene geometry | O(V) = O(E + 1) amortized | O(V) | Fixed-tessellation entities and geometric buffer growth. |
| 2D draw | Expected O(log F + E) | O(E) sampled frame | One sample and one pass over visible entities. |
| Unplaced AR frame | Expected O(log F + E + P), excluding SDK internals | O(E + P) including SDK collection | Scan detected planes until placement. |
| Placed AR frame | Expected O(log F + E + V), excluding SDK internals | O(E + V) | Sample, transform, mesh generation/upload/draw. |

## Why indexing matters

The recovered implementation searched the next frame's entity list for every current entity. That required O(E squared) matching per interpolated sample. Frame now builds an ID index once, changing expected matching time to O(E) at the cost of O(E) additional stored references per frame. Binary search remains O(log F). Hash collision worst cases must not be described as guaranteed constant-time lookup.

The entire replay retains O(R) entities plus indexes and O(F) frame metadata. A sampled frame also stores an index, so its memory is O(E), not O(1). Mesh caching avoids rebuilding geometry while paused, but camera processing and GPU uploads still occur. The renderer keeps only one cached mesh and one anchor.

## Performance evidence and limits

The deterministic test binarySearchMatchesLinearReference checks 1,000 randomly selected times across 300 frames against an analytical linear trajectory. This tests correctness; it is not a benchmark or an empirical proof of asymptotic complexity.

Run the optional **:core:benchmark** task for local timings at increasing entity counts. It reports sampling and mesh timing separately, warms up the JVM, and uses a checksum so results are consumed. Timings depend on the JVM, CPU, GC, and power state. Do not compare desktop JVM numbers to AR frame rates.

ARCore camera acquisition, tracking, hit testing, and driver/GPU costs are external to this analysis and cannot honestly be assigned O(1) here. Animated mesh generation still allocates arrays; a next step for large scenes would be persistent static/dynamic GPU meshes. Current CSV input is explicitly bounded and the bundled scenes are small. Device frame-time, thermal, and battery measurements remain physical-device validation tasks.

## Recorded desktop run (2026-10-30)

Windows, OpenJDK 17, 1,000 frames; times below are microseconds per operation from one warmed run. They are informational measurements, not pass/fail thresholds.

| Entities | Sample, microseconds | Mesh build, microseconds |
| ---: | ---: | ---: |
| 8 | 7.364 | 833.744 |
| 16 | 11.274 | 1388.086 |
| 32 | 20.799 | 2984.873 |
| 64 | 40.982 | 6273.482 |

The observed trend is consistent with increasing entity work; Big-O claims above come from inspecting loops/data structures, not fitting these four measurements.
