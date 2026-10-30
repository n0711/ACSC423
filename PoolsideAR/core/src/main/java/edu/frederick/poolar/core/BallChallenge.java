package edu.frederick.poolar.core;


public final class BallChallenge {
  private boolean answered;
  public final double targetX;
  public final double targetZ;

  public BallChallenge(Frame frame) {
    Entity ball = null;
    for (Entity entity : frame.entities) {
      if (entity.kind == Entity.Kind.BALL) {
        ball = entity;
      }
    }
    if (ball == null) {
      throw new IllegalArgumentException("No visible ball in this frame");
    }
    this.targetX = ball.x;
    this.targetZ = ball.z;
  }

  public synchronized double guess(double x, double z) {
    if (this.answered) {
      throw new IllegalStateException("This round already has a guess");
    }
    if (!Double.isFinite(x) || !Double.isFinite(z)) {
      throw new IllegalArgumentException("Guess must be finite");
    }
    this.answered = true;
    return Math.hypot(x - this.targetX, z - this.targetZ);
  }

  public synchronized boolean isAnswered() {
    return this.answered;
  }
}
