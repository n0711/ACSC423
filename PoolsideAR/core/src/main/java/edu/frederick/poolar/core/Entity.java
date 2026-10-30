package edu.frederick.poolar.core;

public final class Entity {
  public final double confidence;
  public final String id;
  public final Kind kind;
  public final double x;
  public final double y;
  public final double z;

  public enum Kind {
    RED,
    BLUE,
    SWIMMER,
    BALL
  }

  public Entity(String id, Kind kind, double x, double y, double z, double confidence) {
    if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_-]{0,31}") || kind == null) {
      throw new IllegalArgumentException("Invalid entity identifier or kind");
    }
    if (!Double.isFinite(x)
        || !Double.isFinite(y)
        || !Double.isFinite(z)
        || !Double.isFinite(confidence)
        || confidence < 0.0d
        || confidence > 1.0d) {
      throw new IllegalArgumentException("Coordinates must be finite; confidence must be in [0,1]");
    }
    this.id = id;
    this.kind = kind;
    this.x = x;
    this.y = y;
    this.z = z;
    this.confidence = confidence;
  }

  Entity interpolate(Entity next, double amount) {
    return new Entity(
        this.id,
        this.kind,
        this.x + ((next.x - this.x) * amount),
        this.y + ((next.y - this.y) * amount),
        this.z + ((next.z - this.z) * amount),
        Math.min(this.confidence, next.confidence));
  }
}
