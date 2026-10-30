package edu.frederick.poolar.core;

public final class ViewerState {
  private BallChallenge challenge;
  private Playback playback;
  private Replay replay;

  public static final class Snapshot {
    public final long durationMs;
    public final Frame frame;
    public final boolean hideBall;
    public final boolean playing;
    public final double speed;
    public final Sport sport;

    Snapshot(Replay replay, Playback playback, BallChallenge challenge, long nowNs) {
      this.sport = replay.sport;
      this.frame = playback.sample(nowNs);
      this.durationMs = replay.durationMs();
      this.playing = playback.isPlaying();
      this.speed = playback.speed();
      this.hideBall = (challenge == null || challenge.isAnswered()) ? false : true;
    }
  }

  public ViewerState(Replay replay, long nowNs) {
    select(replay, nowNs);
  }

  public synchronized void select(Replay value, long nowNs) {
    this.replay = value;
    this.playback = new Playback(value, nowNs);
    this.challenge = null;
  }

  public synchronized Snapshot snapshot(long nowNs) {
    return new Snapshot(this.replay, this.playback, this.challenge, nowNs);
  }

  public synchronized void setPlaying(boolean playing, long nowNs) {
    playback.setPlaying(playing, nowNs);
  }

  public synchronized void toggle(long nowNs) {
    this.challenge = null;
    this.playback.setPlaying(!this.playback.isPlaying(), nowNs);
  }

  public synchronized void restart(long nowNs) {
    this.challenge = null;
    this.playback.seek(0.0d, nowNs);
    this.playback.setPlaying(true, nowNs);
  }

  public synchronized void speed(double value, long nowNs) {
    this.playback.setSpeed(value, nowNs);
  }

  public synchronized void seek(double fraction, long nowNs) {
    this.challenge = null;
    this.playback.seek(fraction, nowNs);
  }

  public synchronized void beginChallenge(long nowNs) {
    if (this.replay.sport != Sport.WATER_POLO) {
      throw new IllegalStateException("The ball game is for water polo");
    }
    this.challenge = new BallChallenge(this.playback.sample(nowNs));
    this.playback.setPlaying(false, nowNs);
  }

  public synchronized double guess(double x, double z) {
    if (this.challenge == null) {
      throw new IllegalStateException("Start a round first");
    }
    if (!Double.isFinite(x)
        || !Double.isFinite(z)
        || x < 0.0d
        || x > this.replay.sport.length
        || z < 0.0d
        || z > this.replay.sport.width) {
      throw new IllegalArgumentException("Tap inside the pool");
    }
    return this.challenge.guess(x, z);
  }
}
