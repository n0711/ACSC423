package edu.frederick.poolar.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class SceneMeshTest {
  @Test
  public void geometryHasFiniteCoordinatesAndValidColors() {
    for (Sport sport : Sport.values()) {
      Mesh m = SceneMesh.build(sport, PlaybackTest.polo().frames.get(0), false);
      assertEquals(0, m.vertexCount() % 3);
      assertEquals(0, m.vertices.length % 6);
      for (int i = 0; i < m.vertices.length; i++) {
        assertTrue(Float.isFinite(m.vertices[i]));
        if (i % 6 >= 3) assertTrue(m.vertices[i] >= 0 && m.vertices[i] <= 1);
      }
    }
  }

  @Test
  public void hiddenBallRemovesGeometry() {
    Frame frame = PlaybackTest.polo().frames.get(0);
    assertTrue(
        SceneMesh.build(Sport.WATER_POLO, frame, false).vertexCount()
            > SceneMesh.build(Sport.WATER_POLO, frame, true).vertexCount());
  }

  @Test
  public void swimmersAddGeometry() {
    Frame empty = ReplayTest.frame(0);
    Frame swimmer = ReplayTest.frame(0, new Entity("swimmer1", Entity.Kind.SWIMMER, 2, 0, 1, 1));
    assertTrue(
        SceneMesh.build(Sport.SWIMMING, swimmer, false).vertexCount()
            > SceneMesh.build(Sport.SWIMMING, empty, false).vertexCount());
  }
}
