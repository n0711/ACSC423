package edu.frederick.poolar.core;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class ReplayTest {
  static Entity entity(String id, double x, double confidence) {
    return new Entity(id, Entity.Kind.RED, x, 0, 2, confidence);
  }

  static Frame frame(long t, Entity... entities) {
    return new Frame(t, Arrays.asList(entities));
  }

  static Replay replay() {
    return new Replay(
        Sport.WATER_POLO,
        Arrays.asList(frame(0, entity("red1", 0, 1)), frame(1000, entity("red1", 10, 1))));
  }

  @Test
  public void interpolatesFractionalTime() {
    Frame sample = replay().sample(250);
    assertEquals(2.5, sample.find("red1").x, 1e-9);
    assertTrue(sample.interpolated);
  }

  @Test
  public void clampsBeforeStart() {
    assertEquals(0, replay().sample(-100).timeMs);
  }

  @Test
  public void clampsAfterEnd() {
    assertEquals(1000, replay().sample(Long.MAX_VALUE).timeMs);
    assertEquals(10, replay().sample(1001).find("red1").x, 0);
  }

  @Test
  public void exactFrameIsNotInterpolated() {
    assertFalse(replay().sample(1000).interpolated);
  }

  @Test
  public void filtersLowConfidence() {
    Replay r =
        new Replay(
            Sport.WATER_POLO,
            Arrays.asList(frame(0, entity("red1", 0, .49)), frame(1000, entity("red1", 10, 1))));
    assertTrue(r.sample(0).entities.isEmpty());
    assertTrue(r.sample(500).entities.isEmpty());
  }

  @Test
  public void confidenceBoundaryVisible() {
    Replay r =
        new Replay(
            Sport.WATER_POLO,
            Arrays.asList(frame(0, entity("red1", 0, .5)), frame(1000, entity("red1", 10, .5))));
    assertEquals(1, r.sample(500).entities.size());
  }

  @Test
  public void missingEntityIsNotInvented() {
    Replay r =
        new Replay(
            Sport.WATER_POLO,
            Arrays.asList(frame(0, entity("red1", 0, 1)), frame(1000, entity("red2", 10, 1))));
    assertTrue(r.sample(500).entities.isEmpty());
  }

  @Test
  public void longGapsAreNotInterpolated() {
    Replay r =
        new Replay(
            Sport.WATER_POLO,
            Arrays.asList(frame(0, entity("red1", 0, 1)), frame(2000, entity("red1", 10, 1))));
    assertTrue(r.sample(1000).entities.isEmpty());
    assertEquals(1, r.sample(2000).entities.size());
  }

  @Test
  public void exactMaximumGapInterpolates() {
    Replay r =
        new Replay(
            Sport.WATER_POLO,
            Arrays.asList(frame(0, entity("red1", 0, 1)), frame(1500, entity("red1", 9, 1))));
    assertEquals(3, r.sample(500).find("red1").x, 0);
  }

  @Test
  public void framesDefensivelyCopy() {
    List<Entity> source = new ArrayList<>();
    source.add(entity("red1", 0, 1));
    Frame f = new Frame(0, source);
    source.clear();
    assertEquals(1, f.entities.size());
    assertThrows(UnsupportedOperationException.class, () -> f.entities.clear());
  }

  @Test
  public void replayDefensivelyCopies() {
    List<Frame> source = new ArrayList<>(replay().frames);
    Replay r = new Replay(Sport.WATER_POLO, source);
    source.clear();
    assertEquals(2, r.frames.size());
  }

  @Test
  public void rejectsDuplicateIds() {
    assertThrows(
        IllegalArgumentException.class, () -> frame(0, entity("a", 0, 1), entity("a", 1, 1)));
  }

  @Test
  public void rejectsNonFinite() {
    assertThrows(IllegalArgumentException.class, () -> entity("a", Double.NaN, 1));
    assertThrows(IllegalArgumentException.class, () -> entity("a", 1, Double.POSITIVE_INFINITY));
  }

  @Test
  public void rejectsBadIdentifiers() {
    assertThrows(IllegalArgumentException.class, () -> entity("../a", 0, 1));
  }

  @Test
  public void rejectsUnorderedFrames() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Replay(Sport.WATER_POLO, Arrays.asList(frame(0), frame(0))));
  }

  @Test
  public void rejectsNullFrame() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Replay(Sport.WATER_POLO, Arrays.asList(frame(0), null)));
  }

  @Test
  public void binarySearchMatchesLinearReference() {
    List<Frame> frames = new ArrayList<>();
    for (int i = 0; i < 300; i++) frames.add(frame(i * 100, entity("red1", i * .1, 1)));
    Replay r = new Replay(Sport.WATER_POLO, frames);
    Random random = new Random(423);
    for (int i = 0; i < 1000; i++) {
      long t = random.nextInt(29901);
      assertEquals(t / 1000.0, r.sample(t).find("red1").x, 1e-8);
    }
  }
}
