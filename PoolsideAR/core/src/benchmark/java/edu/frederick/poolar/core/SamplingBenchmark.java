package edu.frederick.poolar.core;

import java.util.*;

/** Informational microbenchmark, not a CI performance threshold or a device FPS claim. */
public final class SamplingBenchmark {
  private static volatile double sink;

  public static void main(String[] args) {
    System.out.println("entities,frames,sample_us,mesh_us");
    for (int count : new int[] {8, 16, 32, 64}) {
      List<Frame> frames = new ArrayList<>();
      for (int f = 0; f < 1000; f++) {
        List<Entity> entities = new ArrayList<>();
        for (int e = 0; e < count; e++)
          entities.add(new Entity("red" + e, Entity.Kind.RED, (e % 25) + f * .001, 0, e % 20, 1));
        frames.add(new Frame(f * 100, entities));
      }
      Replay replay = new Replay(Sport.WATER_POLO, frames);
      for (int i = 0; i < 5000; i++) sink += replay.sample((i * 17) % 99900).entities.get(0).x;
      long start = System.nanoTime();
      for (int i = 0; i < 20000; i++) sink += replay.sample((i * 17) % 99900).entities.get(0).x;
      double sample = (System.nanoTime() - start) / 20000.0 / 1000;
      for (int i = 0; i < 20; i++)
        sink += SceneMesh.build(Sport.WATER_POLO, frames.get(0), false).vertexCount();
      start = System.nanoTime();
      for (int i = 0; i < 100; i++)
        sink += SceneMesh.build(Sport.WATER_POLO, frames.get(0), false).vertexCount();
      double mesh = (System.nanoTime() - start) / 100.0 / 1000;
      System.out.printf(Locale.ROOT, "%d,1000,%.3f,%.3f%n", count, sample, mesh);
    }
    System.out.println("checksum=" + sink);
  }
}
