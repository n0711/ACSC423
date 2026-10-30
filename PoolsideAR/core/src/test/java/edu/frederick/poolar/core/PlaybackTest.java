package edu.frederick.poolar.core;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class PlaybackTest {
  @Test
  public void pauseFreezesTime() {
    Playback p = new Playback(ReplayTest.replay(), 0);
    p.setPlaying(false, 250_000_000);
    assertEquals(250, p.position(900_000_000));
  }

  @Test
  public void resumeContinuesAtSamePosition() {
    Playback p = new Playback(ReplayTest.replay(), 0);
    p.setPlaying(false, 250_000_000);
    p.setPlaying(true, 1_000_000_000);
    assertEquals(500, p.position(1_250_000_000));
  }

  @Test
  public void speedChangeDoesNotJump() {
    Playback p = new Playback(ReplayTest.replay(), 0);
    p.setSpeed(2, 250_000_000);
    assertEquals(250, p.position(250_000_000));
    assertEquals(750, p.position(500_000_000));
  }

  @Test
  public void loopsAfterEnd() {
    Playback p = new Playback(ReplayTest.replay(), 0);
    assertEquals(250, p.position(2_250_000_000L));
  }

  @Test
  public void exactEndRemainsVisible() {
    assertEquals(1000, new Playback(ReplayTest.replay(), 0).position(1_000_000_000));
  }

  @Test
  public void seekWhilePaused() {
    Playback p = new Playback(ReplayTest.replay(), 0);
    p.setPlaying(false, 0);
    p.seek(.75, 0);
    assertEquals(750, p.position(8_000_000_000L));
  }

  @Test
  public void negativeElapsedDoesNotRewind() {
    assertEquals(0, new Playback(ReplayTest.replay(), 100).position(0));
  }

  @Test
  public void rejectsInvalidSpeedAndSeek() {
    Playback p = new Playback(ReplayTest.replay(), 0);
    for (double n : new double[] {0, 5, Double.NaN, Double.POSITIVE_INFINITY})
      assertThrows(IllegalArgumentException.class, () -> p.setSpeed(n, 0));
    for (double n : new double[] {-.1, 1.1, Double.NaN})
      assertThrows(IllegalArgumentException.class, () -> p.seek(n, 0));
  }

  static Replay polo() {
    Entity ball = new Entity("ball", Entity.Kind.BALL, 10, 0, 5, 1);
    return new Replay(
        Sport.WATER_POLO, Arrays.asList(ReplayTest.frame(0, ball), ReplayTest.frame(1000, ball)));
  }

  @Test
  public void challengePausesAndHidesBall() {
    ViewerState s = new ViewerState(polo(), 0);
    s.beginChallenge(100);
    assertTrue(s.snapshot(200).hideBall);
    assertFalse(s.snapshot(200).playing);
    assertEquals(5, s.guess(13, 9), 0);
    assertFalse(s.snapshot(200).hideBall);
  }

  @Test
  public void onlyOneGuessPerRound() {
    ViewerState s = new ViewerState(polo(), 0);
    s.beginChallenge(0);
    s.guess(10, 5);
    assertThrows(IllegalStateException.class, () -> s.guess(10, 5));
  }

  @Test
  public void invalidGuessDoesNotConsumeRound() {
    ViewerState s = new ViewerState(polo(), 0);
    s.beginChallenge(0);
    assertThrows(IllegalArgumentException.class, () -> s.guess(-1, 0));
    assertThrows(IllegalArgumentException.class, () -> s.guess(Double.NaN, 0));
    assertEquals(0, s.guess(10, 5), 0);
  }

  @Test
  public void seekCancelsChallenge() {
    ViewerState s = new ViewerState(polo(), 0);
    s.beginChallenge(0);
    s.seek(.5, 0);
    assertFalse(s.snapshot(0).hideBall);
    assertThrows(IllegalStateException.class, () -> s.guess(0, 0));
  }

  @Test
  public void swimmingCannotStartChallenge() {
    ViewerState s =
        new ViewerState(
            new Replay(Sport.SWIMMING, Arrays.asList(ReplayTest.frame(0), ReplayTest.frame(1000))),
            0);
    assertThrows(IllegalStateException.class, () -> s.beginChallenge(0));
  }

  @Test
  public void noBallCannotStartChallenge() {
    assertThrows(IllegalArgumentException.class, () -> new BallChallenge(ReplayTest.frame(0)));
  }
}
