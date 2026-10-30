package edu.frederick.poolar.core;

public final class Mesh {
  public final float[] vertices;

  Mesh(float[] vertices) {
    this.vertices = vertices;
  }

  public int vertexCount() {
    return this.vertices.length / 6;
  }
}
