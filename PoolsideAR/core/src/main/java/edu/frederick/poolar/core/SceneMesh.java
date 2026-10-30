package edu.frederick.poolar.core;

import java.util.Arrays;

public final class SceneMesh {
  private float[] data = new float[16384];
  private int size;

  private void vertex(double x, double y, double z, float[] color, double shade) {
    if (this.size + 6 > this.data.length) {
      this.data = Arrays.copyOf(this.data, this.data.length * 2);
    }
    data[size++] = (float) x;
    data[size++] = (float) y;
    data[size++] = (float) z;
    data[size++] = (float) (color[0] * shade);
    data[size++] = (float) (color[1] * shade);
    data[size++] = (float) (color[2] * shade);
  }

  private void triangle(double[] a, double[] b, double[] c, float[] color, double shade) {
    vertex(a[0], a[1], a[2], color, shade);
    vertex(b[0], b[1], b[2], color, shade);
    vertex(c[0], c[1], c[2], color, shade);
  }

  private void box(double x, double y, double z, double w, double h, double d, float[] color) {
    double[][] v = {
      new double[] {x, y, z},
      new double[] {x + w, y, z},
      new double[] {x + w, y, z + d},
      new double[] {x, y, z + d},
      new double[] {x, y + h, z},
      new double[] {x + w, y + h, z},
      new double[] {x + w, y + h, z + d},
      new double[] {x, y + h, z + d}
    };
    int[][] faces = {
      new int[] {4, 5, 6, 7},
      new int[] {0, 1, 5, 4},
      new int[] {1, 2, 6, 5},
      new int[] {2, 3, 7, 6},
      new int[] {3, 0, 4, 7},
      new int[] {0, 3, 2, 1}
    };
    double[] shades = {1.0d, 0.75d, 0.62d, 0.72d, 0.85d, 0.55d};
    for (int i = 0; i < faces.length; i++) {
      int[] f = faces[i];
      triangle(v[f[0]], v[f[1]], v[f[2]], color, shades[i]);
      triangle(v[f[0]], v[f[2]], v[f[3]], color, shades[i]);
    }
  }

  private void ellipsoid(
      double x, double y, double z, double rx, double ry, double rz, float[] color) {
    for (int lat = 0; lat < 6; lat++) {
      double d = 3.141592653589793d;
      double p0 = ((lat * 3.141592653589793d) / 6.0d) - 1.5707963267948966d;
      double p1 = p0 + 0.5235987755982988d;
      int lon = 0;
      while (lon < 12) {
        double t0 = (lon * d) / 6.0d;
        double t1 = t0 + 0.5235987755982988d;
        double[] a = point(x, y, z, rx, ry, rz, p0, t0);
        double[] b = point(x, y, z, rx, ry, rz, p1, t0);
        double[] c = point(x, y, z, rx, ry, rz, p1, t1);
        double[] d2 = point(x, y, z, rx, ry, rz, p0, t1);
        double shade = (Math.max(0.0d, Math.sin((p0 + p1) / 2.0d)) * 0.32d) + 0.68d;
        triangle(a, b, c, color, shade);
        triangle(a, c, d2, color, shade);
        lon++;
        d = 3.141592653589793d;
      }
    }
  }

  private double[] point(
      double x, double y, double z, double rx, double ry, double rz, double p, double t) {
    return new double[] {
      (Math.cos(p) * rx * Math.cos(t)) + x,
      (Math.sin(p) * ry) + y,
      (Math.cos(p) * rz * Math.sin(t)) + z
    };
  }

  public static Mesh build(Sport sport, Frame frame, boolean hideBall) {
    SceneMesh m = new SceneMesh();
    float[] white = {0.9f, 0.95f, 0.98f};
    float[] water = {0.04f, 0.59f, 0.73f};
    float[] deck = {0.28f, 0.39f, 0.44f};
    float[] red = {1.0f, 0.24f, 0.3f};
    float[] blue = {0.13f, 0.45f, 1.0f};
    float[] yellow = {1.0f, 0.84f, 0.17f};
    double l = sport.length;
    double w = sport.width;
    double ox = (-l) / 2.0d;
    double oz = (-w) / 2.0d;
    m.box(ox - 0.65d, -0.55d, oz - 0.65d, l + 1.3d, 0.55d, w + 1.3d, deck);
    m.box(ox, -0.06d, oz, l, 0.1d, w, water);
    m.box(ox - 0.2d, 0.05d, oz - 0.2d, l + 0.4d, 0.1d, 0.14d, white);
    m.box(ox - 0.2d, 0.05d, oz + w + 0.06d, l + 0.4d, 0.1d, 0.14d, white);
    m.box(ox - 0.2d, 0.05d, oz, 0.14d, 0.1d, w, white);
    m.box(ox + l + 0.06d, 0.05d, oz, 0.14d, 0.1d, w, white);
    if (sport == Sport.SWIMMING) {
      for (int lane = 1; lane < 8; lane++) {
        float[] rope = lane % 2 == 0 ? yellow : blue;
        m.box(ox, 0.07d, (oz + (lane * 2)) - 0.055d, l, 0.08d, 0.11d, rope);
      }
      for (int lane2 = 0; lane2 < 8; lane2++) {
        m.box(
            ox + 0.6d,
            0.045d,
            oz + (lane2 * 2) + 0.95d,
            l - 1.2d,
            0.012d,
            0.1d,
            new float[] {0.025f, 0.25f, 0.34f});
      }
    } else {
      double[] dArr = {ox - 0.05d, (ox + l) - 0.05d};
      for (int i = 0; i < 2; i++) {
        double gx = dArr[i];
        m.box(gx, 0.1d, -1.5d, 0.12d, 1.2d, 0.12d, white);
        m.box(gx, 0.1d, 1.38d, 0.12d, 1.2d, 0.12d, white);
        m.box(gx, 1.25d, -1.5d, 0.12d, 0.12d, 3.0d, white);
      }
      m.box(-0.05d, 0.045d, oz, 0.1d, 0.015d, w, white);
    }
    for (Entity entity : frame.entities) {
      double x = ox + entity.x;
      double z = oz + entity.z;
      if (entity.kind == Entity.Kind.BALL) {
        if (!hideBall) {
          m.ellipsoid(x, entity.y + 0.3d, z, 0.33d, 0.33d, 0.33d, yellow);
        }
      } else {
        float[] color =
            entity.kind == Entity.Kind.RED ? red : entity.kind == Entity.Kind.BLUE ? blue : yellow;
        m.ellipsoid(x, entity.y + 0.16d, z, 0.85d, 0.18d, 0.3d, color);
        m.ellipsoid(x + 0.6d, entity.y + 0.38d, z, 0.27d, 0.25d, 0.27d, white);
        m.ellipsoid(x + 0.6d, entity.y + 0.53d, z, 0.25d, 0.11d, 0.25d, color);
      }
    }
    return new Mesh(Arrays.copyOf(m.data, m.size));
  }
}
