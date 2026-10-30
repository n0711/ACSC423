package edu.frederick.poolar;

import android.opengl.GLES20;

final class GlProgram {
  private GlProgram() {}

  static int create(String vertexSource, String fragmentSource) {
    int vertex = shader(35633, vertexSource);
    int fragment = shader(35632, fragmentSource);
    int program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vertex);
    GLES20.glAttachShader(program, fragment);
    GLES20.glLinkProgram(program);
    GLES20.glDeleteShader(vertex);
    GLES20.glDeleteShader(fragment);
    int[] status = new int[1];
    GLES20.glGetProgramiv(program, 35714, status, 0);
    if (status[0] == 0) {
      String error = GLES20.glGetProgramInfoLog(program);
      GLES20.glDeleteProgram(program);
      throw new IllegalStateException("OpenGL program: " + error);
    }
    return program;
  }

  private static int shader(int type, String source) {
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, source);
    GLES20.glCompileShader(shader);
    int[] status = new int[1];
    GLES20.glGetShaderiv(shader, 35713, status, 0);
    if (status[0] == 0) {
      String error = GLES20.glGetShaderInfoLog(shader);
      GLES20.glDeleteShader(shader);
      throw new IllegalStateException("OpenGL shader: " + error);
    }
    return shader;
  }
}
