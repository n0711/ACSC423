package edu.frederick.poolar.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Replay {
  public static final long MAX_INTERPOLATION_GAP_MS = 1500;
  public static final double MIN_CONFIDENCE = 0.5d;
  public final List<Frame> frames;
  public final Sport sport;

  public Replay(Sport sport, List<Frame> frames) {
    if (sport == null
        || frames == null
        || frames.size() < 2
        || frames.get(0) == null
        || frames.get(0).timeMs != 0) {
      throw new IllegalArgumentException(
          "Replay requires a sport and at least two frames starting at 0");
    }
    long previous = -1;
    for (Frame frame : frames) {
      if (frame == null || frame.timeMs <= previous) {
        throw new IllegalArgumentException("Frames must increase in time");
      }
      previous = frame.timeMs;
    }
    this.sport = sport;
    this.frames = Collections.unmodifiableList(new ArrayList<>(frames));
  }

  public long durationMs() {
    return this.frames.get(this.frames.size() - 1).timeMs;
  }

  public Frame sample(long timeMs) {
    long time = Math.max(0L, Math.min(timeMs, durationMs()));
    int low = 0;
    int high = this.frames.size() - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      long timestamp = this.frames.get(middle).timeMs;
      if (timestamp == time) {
        return visible(this.frames.get(middle), time);
      }
      if (timestamp < time) {
        low = middle + 1;
      } else {
        high = middle - 1;
      }
    }
    Frame before = this.frames.get(high);
    Frame after = this.frames.get(low);
    List<Entity> entities = new ArrayList<>();
    if (after.timeMs - before.timeMs <= MAX_INTERPOLATION_GAP_MS) {
      double amount = (double) (time - before.timeMs) / (after.timeMs - before.timeMs);
      for (Entity entity : before.entities) {
        Entity next = after.find(entity.id);
        if (next != null
            && next.kind == entity.kind
            && entity.confidence >= 0.5d
            && next.confidence >= 0.5d) {
          entities.add(entity.interpolate(next, amount));
        }
      }
    }
    return new Frame(time, entities, true);
  }

  private Frame visible(Frame frame, long time) {
    List<Entity> entities = new ArrayList<>();
    for (Entity entity : frame.entities) {
      if (entity.confidence >= 0.5d) {
        entities.add(entity);
      }
    }
    return new Frame(time, entities);
  }
}
