package edu.frederick.poolar.core;

import java.util.*;

/** Immutable timestamped sample with an index for expected O(1) entity lookup. */
public final class Frame {
  public final long timeMs;
  public final List<Entity> entities;
  public final boolean interpolated;
  private final Map<String, Entity> byId;

  public Frame(long timeMs, List<Entity> entities) {
    this(timeMs, entities, false);
  }

  Frame(long timeMs, List<Entity> entities, boolean interpolated) {
    if (timeMs < 0 || entities == null) throw new IllegalArgumentException("Invalid frame");
    Map<String, Entity> index = new HashMap<>();
    for (Entity entity : entities) {
      if (entity == null || index.put(entity.id, entity) != null)
        throw new IllegalArgumentException("Duplicate or missing entity in frame");
    }
    this.timeMs = timeMs;
    this.entities = Collections.unmodifiableList(new ArrayList<>(entities));
    this.byId = Collections.unmodifiableMap(index);
    this.interpolated = interpolated;
  }

  public Entity find(String id) {
    return byId.get(id);
  }
}
