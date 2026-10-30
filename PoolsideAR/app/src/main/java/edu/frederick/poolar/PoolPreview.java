package edu.frederick.poolar;

import android.graphics.*;
import android.view.*;
import edu.frederick.poolar.core.*;

/** Camera-free, aspect-correct replay. All coordinates use the same pool metres as AR. */
// Constructed programmatically with its required replay model; not inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
final class PoolPreview extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF pool = new RectF();
  private final ViewerState state;
  private final PoolArActivity activity;
  private float tapX, tapY;

  PoolPreview(PoolArActivity activity, ViewerState state) {
    super(activity);
    this.activity = activity;
    this.state = state;
    setContentDescription(activity.getString(R.string.preview_description));
    setFocusable(true);
    setClickable(true);
  }

  private float dp(float value) {
    return value * getResources().getDisplayMetrics().density;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ViewerState.Snapshot snapshot = state.snapshot(System.nanoTime());
    canvas.drawColor(Color.rgb(11, 38, 54));
    float margin = dp(26);
    float scale =
        Math.min(
            Math.max(1, getWidth() - margin * 2) / (float) snapshot.sport.length,
            Math.max(1, getHeight() - margin * 2) / (float) snapshot.sport.width);
    float width = (float) snapshot.sport.length * scale,
        height = (float) snapshot.sport.width * scale;
    pool.set(
        (getWidth() - width) / 2,
        (getHeight() - height) / 2,
        (getWidth() + width) / 2,
        (getHeight() + height) / 2);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.rgb(15, 103, 125));
    canvas.drawRoundRect(pool, dp(5), dp(5), paint);
    paint.setColor(Color.rgb(95, 199, 206));
    paint.setStrokeWidth(dp(1));
    if (snapshot.sport == Sport.SWIMMING) {
      for (int lane = 1; lane < 8; lane++) {
        float y = pool.top + lane * 2 * scale;
        canvas.drawLine(pool.left, y, pool.right, y, paint);
      }
    } else {
      canvas.drawLine(pool.centerX(), pool.top, pool.centerX(), pool.bottom, paint);
      paint.setStyle(Paint.Style.STROKE);
      canvas.drawCircle(pool.centerX(), pool.centerY(), 2 * scale, paint);
      paint.setStrokeWidth(dp(3));
      paint.setColor(Color.WHITE);
      canvas.drawLine(
          pool.left,
          pool.centerY() - 1.5f * scale,
          pool.left,
          pool.centerY() + 1.5f * scale,
          paint);
      canvas.drawLine(
          pool.right,
          pool.centerY() - 1.5f * scale,
          pool.right,
          pool.centerY() + 1.5f * scale,
          paint);
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(2));
    paint.setColor(Color.rgb(184, 237, 235));
    canvas.drawRoundRect(pool, dp(5), dp(5), paint);
    paint.setStyle(Paint.Style.FILL);
    for (Entity entity : snapshot.frame.entities) {
      if (entity.kind == Entity.Kind.BALL && snapshot.hideBall) continue;
      float x = pool.left + (float) entity.x * scale, y = pool.top + (float) entity.z * scale;
      boolean ball = entity.kind == Entity.Kind.BALL;
      int color =
          ball
              ? Color.rgb(255, 213, 88)
              : entity.kind == Entity.Kind.RED
                  ? Color.rgb(178, 36, 64)
                  : entity.kind == Entity.Kind.BLUE
                      ? Color.rgb(35, 73, 168)
                      : Color.rgb(255, 213, 88);
      float radius = ball ? dp(5) : Math.max(dp(7), Math.min(dp(11), scale * .75f));
      paint.setColor(Color.rgb(11, 38, 54));
      canvas.drawCircle(x, y + dp(2), radius + dp(2), paint);
      paint.setColor(color);
      canvas.drawCircle(x, y, radius, paint);
      if (!ball) {
        paint.setColor(entity.kind == Entity.Kind.SWIMMER ? Color.rgb(11, 38, 54) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(9));
        String label =
            entity.kind == Entity.Kind.RED
                ? "R"
                : entity.kind == Entity.Kind.BLUE ? "B" : entity.id.replace("swimmer", "");
        canvas.drawText(label, x, y - (paint.ascent() + paint.descent()) / 2, paint);
      }
    }
    if (snapshot.playing && getWindowVisibility() == VISIBLE) postInvalidateOnAnimation();
  }

  @Override
  public boolean onTouchEvent(android.view.MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      tapX = event.getX();
      tapY = event.getY();
      performClick();
      return true;
    }
    return event.getAction() == MotionEvent.ACTION_DOWN || super.onTouchEvent(event);
  }

  @Override
  public boolean performClick() {
    super.performClick();
    ViewerState.Snapshot snapshot = state.snapshot(System.nanoTime());
    if (snapshot.hideBall && pool.width() > 0 && pool.height() > 0) {
      if (pool.contains(tapX, tapY))
        activity.guess(
            (tapX - pool.left) / pool.width() * snapshot.sport.length,
            (tapY - pool.top) / pool.height() * snapshot.sport.width);
    }
    return true;
  }
}
