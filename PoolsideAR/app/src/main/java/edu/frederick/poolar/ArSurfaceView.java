package edu.frederick.poolar;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

final class ArSurfaceView extends GLSurfaceView {
  private Activation activation;
  private float lastX;
  private float lastY;

  interface Activation {
    void tap(float f, float f2);
  }

  ArSurfaceView(Context context) {
    super(context);
    this.lastX = -1.0f;
    this.lastY = -1.0f;
    setFocusable(true);
    setContentDescription(
        "AR camera. Activate to place the pool on a detected table or submit a ball-game guess.");
  }

  void activation(Activation value) {
    this.activation = value;
  }

  @Override // android.view.View
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == 1) {
      this.lastX = event.getX();
      this.lastY = event.getY();
      performClick();
    }
    return true;
  }

  @Override // android.view.View
  public boolean performClick() {
    super.performClick();
    if (this.activation != null) {
      this.activation.tap(
          this.lastX >= 0.0f ? this.lastX : getWidth() / 2.0f,
          this.lastY >= 0.0f ? this.lastY : getHeight() / 2.0f);
      return true;
    }
    return true;
  }
}
