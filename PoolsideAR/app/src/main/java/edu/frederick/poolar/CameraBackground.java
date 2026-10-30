package edu.frederick.poolar;

import android.opengl.GLES20;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

final class CameraBackground {
  private int positionLocation;
  private int program;
  private int samplerLocation;
  private int texture;
  private int uvLocation;
  private final float[] corners = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
  private final float[] coordinates = new float[8];
  private final FloatBuffer positions = buffer(8);
  private final FloatBuffer uv = buffer(8);

  CameraBackground() {}

  private static FloatBuffer buffer(int size) {
    return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
  }

  void create() {
    int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);
    this.texture = textures[0];
    GLES20.glBindTexture(36197, this.texture);
    GLES20.glTexParameteri(36197, 10241, 9729);
    GLES20.glTexParameteri(36197, 10240, 9729);
    GLES20.glTexParameteri(36197, 10242, 33071);
    GLES20.glTexParameteri(36197, 10243, 33071);
    this.program =
        GlProgram.create(
            "attribute vec2 aPosition; attribute vec2 aUv; varying vec2 vUv; void"
                + " main(){gl_Position=vec4(aPosition,0.0,1.0);vUv=aUv;}",
            "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float; uniform samplerExternalOES uCamera; varying vec2 vUv;"
                + " void main(){gl_FragColor=texture2D(uCamera,vUv);}");
    this.positionLocation = GLES20.glGetAttribLocation(this.program, "aPosition");
    this.uvLocation = GLES20.glGetAttribLocation(this.program, "aUv");
    this.samplerLocation = GLES20.glGetUniformLocation(this.program, "uCamera");
    this.positions.put(this.corners).position(0);
  }

  int texture() {
    return this.texture;
  }

  void draw(Frame frame) {
    if (frame.getTimestamp() == 0) {
      return;
    }
    frame.transformCoordinates2d(
        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
        this.corners,
        Coordinates2d.TEXTURE_NORMALIZED,
        this.coordinates);
    this.uv.clear();
    this.uv.put(this.coordinates).position(0);
    GLES20.glDisable(2929);
    GLES20.glDepthMask(false);
    GLES20.glUseProgram(this.program);
    GLES20.glActiveTexture(33984);
    GLES20.glBindTexture(36197, this.texture);
    GLES20.glUniform1i(this.samplerLocation, 0);
    this.positions.position(0);
    this.uv.position(0);
    GLES20.glVertexAttribPointer(this.positionLocation, 2, 5126, false, 0, (Buffer) this.positions);
    GLES20.glVertexAttribPointer(this.uvLocation, 2, 5126, false, 0, (Buffer) this.uv);
    GLES20.glEnableVertexAttribArray(this.positionLocation);
    GLES20.glEnableVertexAttribArray(this.uvLocation);
    GLES20.glDrawArrays(5, 0, 4);
    GLES20.glDisableVertexAttribArray(this.positionLocation);
    GLES20.glDisableVertexAttribArray(this.uvLocation);
    GLES20.glDepthMask(true);
    GLES20.glEnable(2929);
  }
}
