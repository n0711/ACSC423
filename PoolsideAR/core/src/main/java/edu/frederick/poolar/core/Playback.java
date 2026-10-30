package edu.frederick.poolar.core;

public final class Playback {
  private double positionMs;
  private final Replay replay;
  private long startedNs;
  private double speed = 1.0d;
  private boolean playing = true;

  public Playback(Replay replay, long nowNs) {
    if (replay == null) {
      throw new IllegalArgumentException("Replay is required");
    }
    this.replay = replay;
    this.startedNs = nowNs;
  }

  public synchronized long position(long nowNs) {
    double time;
    double duration;
    time =
        this.positionMs
            + (this.playing
                ? (Math.max(0L, nowNs - this.startedNs) / 1000000.0d) * this.speed
                : 0.0d);
    duration = this.replay.durationMs();
    if (time > duration) {
      time %= duration;
    }
    return (long) Math.max(0.0d, Math.min(time, duration));
  }

  public synchronized Frame sample(long nowNs) {
    return this.replay.sample(position(nowNs));
  }

  public synchronized boolean isPlaying() {
    return this.playing;
  }

  public synchronized double speed() {
    return this.speed;
  }

  public synchronized void setPlaying(boolean value, long nowNs) {
    this.positionMs = position(nowNs);
    this.startedNs = nowNs;
    this.playing = value;
  }

  public synchronized void setSpeed(double value, long nowNs) {
    if (!Double.isFinite(value) || value < 0.25d || value > 4.0d) {
      throw new IllegalArgumentException("Playback speed must be in [0.25,4]");
    }
    this.positionMs = position(nowNs);
    this.startedNs = nowNs;
    this.speed = value;
  }

  public synchronized void seek(double fraction, long nowNs) {
    if (!Double.isFinite(fraction) || fraction < 0.0d || fraction > 1.0d) {
      throw new IllegalArgumentException("Seek fraction must be in [0,1]");
    }
    this.positionMs = this.replay.durationMs() * fraction;
    this.startedNs = nowNs;
  }
}
