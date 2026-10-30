package edu.frederick.poolar;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import edu.frederick.poolar.core.Mesh;
import edu.frederick.poolar.core.SceneMesh;
import edu.frederick.poolar.core.ViewerState;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class PoolRenderer implements GLSurfaceView.Renderer {
  private final PoolArActivity activity;
  private Anchor anchor;
  private int colorLocation;
  private boolean failed;
  private int matrixLocation;
  private volatile boolean placed;
  private int positionLocation;
  private int program;
  private volatile Session session;
  private final ViewerState state;
  private FloatBuffer vertices;
  private Mesh cachedMesh;
  private long cachedTime = -1;
  private edu.frederick.poolar.core.Sport cachedSport;
  private boolean cachedHideBall;
  private final CameraBackground background = new CameraBackground();
  private final AtomicReference<float[]> tap = new AtomicReference<>();
  private final AtomicBoolean reset = new AtomicBoolean();
  private final float[] projection = new float[16];
  private final float[] view = new float[16];
  private final float[] model = new float[16];
  private final float[] viewModel = new float[16];
  private final float[] mvp = new float[16];
  private volatile float tableLength = 0.75f;
  private int width = 1;
  private int height = 1;

  PoolRenderer(PoolArActivity activity, ViewerState state) {
    this.activity = activity;
    this.state = state;
  }

  void session(Session value) {
    this.session = value;
    this.failed = false;
  }

  boolean hasAnchor() {
    return this.placed;
  }

  void resetPlacement() {
    this.reset.set(true);
  }

  void tableLength(float meters) {
    this.tableLength = meters;
  }

  void tap(float x, float y) {
    this.tap.set(new float[] {x, y});
  }

  void close() {
    if (this.anchor != null) {
      this.anchor.detach();
      this.anchor = null;
    }
    this.placed = false;
    this.session = null;
  }

  @Override // android.opengl.GLSurfaceView.Renderer
  public void onSurfaceCreated(GL10 ignored, EGLConfig config) {
    GLES20.glClearColor(0.02f, 0.07f, 0.1f, 1.0f);
    this.background.create();
    this.program =
        GlProgram.create(
            "uniform mat4 uMvp; attribute vec3 aPosition; attribute vec3 aColor; varying vec3"
                + " vColor; void main(){gl_Position=uMvp*vec4(aPosition,1.0);vColor=aColor;}",
            "precision mediump float; varying vec3 vColor; void"
                + " main(){gl_FragColor=vec4(vColor,1.0);}");
    this.positionLocation = GLES20.glGetAttribLocation(this.program, "aPosition");
    this.colorLocation = GLES20.glGetAttribLocation(this.program, "aColor");
    this.matrixLocation = GLES20.glGetUniformLocation(this.program, "uMvp");
    GLES20.glEnable(2929);
    GLES20.glDisable(2884);
  }

  @Override // android.opengl.GLSurfaceView.Renderer
  public void onSurfaceChanged(GL10 ignored, int width, int height) {
    this.width = width;
    this.height = height;
    GLES20.glViewport(0, 0, width, height);
  }

  @Override // android.opengl.GLSurfaceView.Renderer
  public void onDrawFrame(GL10 ignored) {
    GLES20.glClear(16640);
    Session active = this.session;
    if (active != null && !this.failed) {
      try {
        active.setDisplayGeometry(
            this.activity.getWindowManager().getDefaultDisplay().getRotation(),
            this.width,
            this.height);
        active.setCameraTextureName(this.background.texture());
        Frame frame = active.update();
        this.background.draw(frame);
        if (this.reset.getAndSet(false)) {
          if (this.anchor != null) {
            this.anchor.detach();
          }
          this.anchor = null;
          this.placed = false;
          this.tap.set(null);
        }
        Camera camera = frame.getCamera();
        if (camera.getTrackingState() != TrackingState.TRACKING) {
          this.tap.set(null);
          this.activity.showStatus("Tracking paused. Move slowly and improve the lighting.");
          return;
        }
        float[] point = this.tap.getAndSet(null);
        ViewerState.Snapshot snapshot = this.state.snapshot(System.nanoTime());
        if (point != null) {
          handleTap(frame, point, snapshot);
        }
        if (this.anchor == null) {
          boolean planeFound = false;
          for (Plane plane : active.getAllTrackables(Plane.class)) {
            if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING
                && plane.getTrackingState() == TrackingState.TRACKING) {
              planeFound = true;
            }
          }
          this.activity.showStatus(
              planeFound
                  ? "Table detected. Tap its surface to place the pool."
                  : "Scan a textured, well-lit table. Move the phone slowly.");
          return;
        }
        if (this.anchor.getTrackingState() != TrackingState.TRACKING) {
          this.activity.showStatus("Pool tracking paused. Scan the table again.");
          return;
        }
        this.activity.showStatus(
            "Pool anchored. Walk around it; use Reset table to place it again.");
        ViewerState.Snapshot snapshot2 = this.state.snapshot(System.nanoTime());
        camera.getProjectionMatrix(this.projection, 0, 0.05f, 100.0f);
        camera.getViewMatrix(this.view, 0);
        this.anchor.getPose().toMatrix(this.model, 0);
        float scale = this.tableLength / ((float) snapshot2.sport.length);
        Matrix.scaleM(this.model, 0, scale, scale, scale);
        Matrix.multiplyMM(this.viewModel, 0, this.view, 0, this.model, 0);
        Matrix.multiplyMM(this.mvp, 0, this.projection, 0, this.viewModel, 0);
        // A paused replay does not need its geometry rebuilt every camera frame.
        if (cachedMesh == null
            || cachedTime != snapshot2.frame.timeMs
            || cachedSport != snapshot2.sport
            || cachedHideBall != snapshot2.hideBall) {
          cachedMesh = SceneMesh.build(snapshot2.sport, snapshot2.frame, snapshot2.hideBall);
          cachedTime = snapshot2.frame.timeMs;
          cachedSport = snapshot2.sport;
          cachedHideBall = snapshot2.hideBall;
        }
        Mesh mesh = cachedMesh;
        if (this.vertices == null || this.vertices.capacity() < mesh.vertices.length) {
          this.vertices =
              ByteBuffer.allocateDirect(mesh.vertices.length * 4)
                  .order(ByteOrder.nativeOrder())
                  .asFloatBuffer();
        }
        this.vertices.clear();
        this.vertices.put(mesh.vertices);
        this.vertices.position(0);
        GLES20.glUseProgram(this.program);
        GLES20.glUniformMatrix4fv(this.matrixLocation, 1, false, this.mvp, 0);
        GLES20.glVertexAttribPointer(
            this.positionLocation, 3, 5126, false, 24, (Buffer) this.vertices);
        this.vertices.position(3);
        GLES20.glVertexAttribPointer(
            this.colorLocation, 3, 5126, false, 24, (Buffer) this.vertices);
        GLES20.glEnableVertexAttribArray(this.positionLocation);
        GLES20.glEnableVertexAttribArray(this.colorLocation);
        GLES20.glDrawArrays(4, 0, mesh.vertexCount());
        GLES20.glDisableVertexAttribArray(this.positionLocation);
        GLES20.glDisableVertexAttribArray(this.colorLocation);
      } catch (Exception failure) {
        this.failed = true;
        Log.e("PoolsideAR", "AR frame failed", failure);
        this.activity.showStatus(
            "AR stopped: "
                + failure.getClass().getSimpleName()
                + ". Choose 2D preview, then View in AR to retry.");
      }
    }
  }

  private void handleTap(Frame frame, float[] point, ViewerState.Snapshot snapshot) {
    if (this.anchor == null || this.anchor.getTrackingState() == TrackingState.TRACKING) {
      for (HitResult hit : frame.hitTest(point[0], point[1])) {
        if (hit.getTrackable() instanceof Plane) {
          Plane plane = (Plane) hit.getTrackable();
          if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING
              && plane.isPoseInPolygon(hit.getHitPose())) {
            if (this.anchor == null) {
              this.anchor = hit.createAnchor();
              this.placed = true;
              this.state.restart(System.nanoTime());
              return;
            } else if (snapshot.hideBall) {
              float[] local =
                  this.anchor.getPose().inverse().compose(hit.getHitPose()).getTranslation();
              if (Math.abs(local[1]) <= 0.05f) {
                double scale = this.tableLength / snapshot.sport.length;
                try {
                  double distance =
                      this.state.guess(
                          (local[0] / scale) + (snapshot.sport.length / 2.0d),
                          (local[2] / scale) + (snapshot.sport.width / 2.0d));
                  this.activity.showGame(
                      String.format(
                          Locale.ROOT,
                          "%s %.2f m from the ball. Play to continue.",
                          distance <= 2.0d ? "Found!" : "Another round?",
                          Double.valueOf(distance)));
                  return;
                } catch (IllegalArgumentException | IllegalStateException failure) {
                  this.activity.showGame(failure.getMessage());
                  return;
                }
              }
            } else {
              return;
            }
          }
        }
      }
    }
  }
}
