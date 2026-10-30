package edu.frederick.poolar;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import com.google.ar.core.*;
import edu.frederick.poolar.core.Replay;
import edu.frederick.poolar.core.ReplayCsv;
import edu.frederick.poolar.core.Sport;
import edu.frederick.poolar.core.ViewerState;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Owns UI and lifecycle; deterministic replay logic lives in the core module. */
public final class PoolArActivity extends Activity {
  private static final int CAMERA_REQUEST = 20;
  private static final int INK = 0xff0b2636, TEAL = 0xff087f8c, MUTED = 0xff49616d;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final EnumMap<Sport, Replay> replays = new EnumMap<>(Sport.class);
  private ViewerState state;
  private PoolPreview preview;
  private ArSurfaceView surface;
  private PoolRenderer renderer;
  private Session session;
  private TextView clock, stats, status, game, legend, size;
  private Button play, challenge, mode, reset, guessButton, polo, swimming;
  private SeekBar timeline, scale;
  private boolean foreground, arMode, surfaceRunning, checkingSupport, installRequested, scrubbing;
  private boolean resumePlayback;
  private float tableLength = .75f;
  private long previewTime = -1;
  private Sport previewSport;
  private boolean previewHidden;
  private volatile String lastStatus = "";

  private final Runnable update =
      new Runnable() {
        @Override
        public void run() {
          if (!foreground || state == null) return;
          refresh();
          handler.postDelayed(this, 100);
        }
      };

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    try {
      for (Sport sport : Sport.values())
        try (Reader reader =
            new InputStreamReader(
                getAssets().open("fixtures/" + sport.fixture), StandardCharsets.UTF_8)) {
          replays.put(sport, ReplayCsv.read(reader, sport));
        }
    } catch (IOException | IllegalArgumentException failure) {
      TextView error = new TextView(this);
      error.setText(R.string.data_error);
      error.setPadding(dp(24), dp(48), dp(24), dp(24));
      setContentView(error);
      return;
    }
    long now = System.nanoTime();
    Sport sport = saved != null && saved.getInt("sport") == 1 ? Sport.SWIMMING : Sport.WATER_POLO;
    state = new ViewerState(replays.get(sport), now);
    if (saved != null) {
      state.seek(saved.getDouble("position", 0), now);
      state.speed(saved.getDouble("speed", 1), now);
      state.setPlaying(saved.getBoolean("playing", true), now);
      tableLength = saved.getFloat("scale", .75f);
    }
    buildUi();
    refresh();
  }

  private void buildUi() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(INK);
    root.setOnApplyWindowInsetsListener(
        (view, insets) -> {
          root.setPadding(
              insets.getSystemWindowInsetLeft(),
              insets.getSystemWindowInsetTop(),
              insets.getSystemWindowInsetRight(),
              insets.getSystemWindowInsetBottom());
          return insets;
        });
    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setPadding(dp(20), dp(14), dp(20), dp(12));
    header.setBackgroundColor(INK);
    TextView title = label(getString(R.string.title), 26, Color.WHITE);
    title.setTypeface(Typeface.DEFAULT_BOLD);
    title.setLetterSpacing(.12f);
    header.addView(title);
    TextView subtitle = label(getString(R.string.subtitle), 10, 0xff8ce2d6);
    subtitle.setLetterSpacing(.08f);
    header.addView(subtitle);
    root.addView(header);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(0xfff1f5f4);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(16), dp(12), dp(16), dp(16));
    scroll.addView(content);
    content.addView(label(getString(R.string.simulation), 10, MUTED));
    LinearLayout sports = row();
    polo = button(R.string.water_polo, sports);
    swimming = button(R.string.swimming, sports);
    content.addView(sports);
    polo.setOnClickListener(v -> select(Sport.WATER_POLO));
    swimming.setOnClickListener(v -> select(Sport.SWIMMING));
    stats = label("", 12, MUTED);
    content.addView(stats);
    FrameLayout stage = new FrameLayout(this);
    int stageHeight = getResources().getConfiguration().orientation == 2 ? 270 : 240;
    content.addView(stage, new LinearLayout.LayoutParams(-1, dp(stageHeight)));
    preview = new PoolPreview(this, state);
    stage.addView(preview, new FrameLayout.LayoutParams(-1, -1));
    surface = new ArSurfaceView(this);
    surface.setEGLContextClientVersion(2);
    surface.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    surface.setPreserveEGLContextOnPause(true);
    renderer = new PoolRenderer(this, state);
    renderer.tableLength(tableLength);
    surface.setRenderer(renderer);
    surface.activation((x, y) -> renderer.tap(x, y));
    stage.addView(surface, new FrameLayout.LayoutParams(-1, -1));
    surface.setVisibility(View.GONE);
    legend = label("", 11, MUTED);
    content.addView(legend);
    LinearLayout timeRow = row();
    clock = label("", 13, INK);
    clock.setTypeface(Typeface.DEFAULT_BOLD);
    timeRow.addView(clock, new LinearLayout.LayoutParams(0, -2, 1));
    Spinner speed = new Spinner(this);
    speed.setContentDescription("Playback speed");
    speed.setAdapter(
        new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"0.5x", "1x", "1.5x", "2x"}));
    double currentSpeed = state.snapshot(System.nanoTime()).speed;
    speed.setSelection(
        currentSpeed == .5 ? 0 : currentSpeed == 1.5 ? 2 : currentSpeed == 2 ? 3 : 1);
    speed.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            state.speed(new double[] {.5, 1, 1.5, 2}[position], System.nanoTime());
            preview.invalidate();
          }

          public void onNothingSelected(AdapterView<?> parent) {}
        });
    timeRow.addView(speed, new LinearLayout.LayoutParams(dp(100), dp(48)));
    content.addView(timeRow);
    timeline = new SeekBar(this);
    timeline.setMax(1000);
    timeline.setContentDescription(getString(R.string.timeline));
    content.addView(timeline, new LinearLayout.LayoutParams(-1, dp(48)));
    timeline.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            if (user) {
              state.seek(progress / 1000.0, System.nanoTime());
              preview.invalidate();
              refresh();
            }
          }

          public void onStartTrackingTouch(SeekBar bar) {
            scrubbing = true;
          }

          public void onStopTrackingTouch(SeekBar bar) {
            scrubbing = false;
          }
        });
    LinearLayout actions = row();
    play = button(R.string.pause, actions);
    Button restart = button(R.string.restart, actions);
    challenge = button(R.string.find_ball, actions);
    content.addView(actions);
    play.setOnClickListener(
        v -> {
          state.toggle(System.nanoTime());
          game.setText("");
          refresh();
        });
    restart.setOnClickListener(
        v -> {
          state.restart(System.nanoTime());
          game.setText("");
          refresh();
        });
    challenge.setOnClickListener(
        v -> {
          try {
            state.beginChallenge(System.nanoTime());
            showGame(getString(R.string.challenge_hint));
            refresh();
          } catch (IllegalArgumentException | IllegalStateException e) {
            showGame(e.getMessage());
          }
        });
    game = label("", 13, INK);
    game.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    content.addView(game);
    guessButton = button(R.string.enter_guess, null);
    content.addView(guessButton);
    guessButton.setOnClickListener(v -> guessDialog());
    LinearLayout modes = row();
    mode = button(R.string.open_ar, modes);
    reset = button(R.string.reset_placement, modes);
    Button help = button(R.string.help, modes);
    content.addView(modes);
    mode.setOnClickListener(
        v -> {
          if (arMode) leaveAr();
          else enterAr();
        });
    reset.setOnClickListener(
        v -> {
          renderer.resetPlacement();
          state.seek(0, System.nanoTime());
          showStatus("Scan a table and tap to place the pool again.");
        });
    help.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.help_body)
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(
                    R.string.privacy,
                    (d, w) ->
                        openUrl("https://developers.google.com/ar/develop/privacy-requirements"))
                .show());
    size = label("", 12, MUTED);
    content.addView(size);
    scale = new SeekBar(this);
    scale.setMax(80);
    scale.setProgress(Math.round((tableLength - .4f) * 100));
    scale.setContentDescription(getString(R.string.scale));
    content.addView(scale, new LinearLayout.LayoutParams(-1, dp(48)));
    scale.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean user) {
            tableLength = .4f + progress / 100f;
            renderer.tableLength(tableLength);
            setTextIfChanged(size, getString(R.string.scale_value, tableLength));
          }

          public void onStartTrackingTouch(SeekBar bar) {}

          public void onStopTrackingTouch(SeekBar bar) {}
        });
    status = label(getString(R.string.preview_status), 13, MUTED);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    content.addView(status);
    if (getResources().getConfiguration().orientation
            == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        && getResources().getConfiguration().screenHeightDp >= 320
        && getResources().getConfiguration().fontScale <= 1.3f) {
      // Keep the complete pool visible beside independently scrolling controls.
      title.setTextSize(20);
      subtitle.setVisibility(View.GONE);
      header.setPadding(dp(20), dp(4), dp(20), dp(4));
      LinearLayout body = row();
      body.setBackgroundColor(0xfff1f5f4);
      root.removeView(scroll);
      root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
      LinearLayout visual = new LinearLayout(this);
      visual.setOrientation(LinearLayout.VERTICAL);
      visual.setPadding(dp(16), dp(8), 0, dp(8));
      for (View child : new View[] {sports, stats, stage, legend}) content.removeView(child);
      visual.addView(sports);
      visual.addView(stats);
      visual.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
      visual.addView(legend);
      body.addView(visual, new LinearLayout.LayoutParams(0, -1, 1.15f));
      body.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
    }
    setContentView(root);
    root.requestApplyInsets();
  }

  private void select(Sport sport) {
    if (state.snapshot(System.nanoTime()).sport != sport) {
      state.select(replays.get(sport), System.nanoTime());
      game.setText("");
    }
    refresh();
  }

  private void refresh() {
    ViewerState.Snapshot s = state.snapshot(System.nanoTime());
    setTextIfChanged(
        clock, getString(R.string.time_value, s.frame.timeMs / 1000.0, s.durationMs / 1000.0));
    setTextIfChanged(
        stats,
        getString(
            R.string.stats,
            s.frame.entities.size() - (s.hideBall ? 1 : 0),
            s.sport.length,
            s.sport.width));
    setTextIfChanged(play, getString(s.playing ? R.string.pause : R.string.play));
    challenge.setEnabled(
        s.sport == Sport.WATER_POLO && (!arMode || renderer.hasAnchor()) && !s.hideBall);
    guessButton.setVisibility(s.hideBall ? View.VISIBLE : View.GONE);
    polo.setSelected(s.sport == Sport.WATER_POLO);
    swimming.setSelected(s.sport == Sport.SWIMMING);
    polo.setTypeface(null, s.sport == Sport.WATER_POLO ? Typeface.BOLD : Typeface.NORMAL);
    swimming.setTypeface(null, s.sport == Sport.SWIMMING ? Typeface.BOLD : Typeface.NORMAL);
    setTextIfChanged(
        legend,
        getString(s.sport == Sport.WATER_POLO ? R.string.legend_polo : R.string.legend_swimming));
    if (!scrubbing) timeline.setProgress((int) (1000.0 * s.frame.timeMs / s.durationMs));
    setTextIfChanged(mode, getString(arMode ? R.string.preview : R.string.open_ar));
    reset.setVisibility(arMode ? View.VISIBLE : View.GONE);
    scale.setVisibility(arMode ? View.VISIBLE : View.GONE);
    size.setVisibility(arMode ? View.VISIBLE : View.GONE);
    setTextIfChanged(size, getString(R.string.scale_value, tableLength));
    if (previewTime != s.frame.timeMs || previewSport != s.sport || previewHidden != s.hideBall) {
      previewTime = s.frame.timeMs;
      previewSport = s.sport;
      previewHidden = s.hideBall;
      preview.invalidate();
    }
  }

  private static void setTextIfChanged(TextView view, String value) {
    if (!android.text.TextUtils.equals(view.getText(), value)) view.setText(value);
  }

  void guess(double x, double z) {
    try {
      double distance = state.guess(x, z);
      showGame(
          getString(
              R.string.guess_result,
              getString(distance <= 2 ? R.string.found : R.string.missed),
              distance));
      refresh();
    } catch (IllegalArgumentException | IllegalStateException e) {
      showGame(e.getMessage());
    }
  }

  private void guessDialog() {
    LinearLayout fields = new LinearLayout(this);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(dp(20), 0, dp(20), 0);
    EditText x = new EditText(this), z = new EditText(this);
    x.setHint(R.string.guess_x);
    z.setHint(R.string.guess_z);
    x.setInputType(8194);
    z.setInputType(8194);
    fields.addView(x);
    fields.addView(z);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.guess_title)
            .setView(fields)
            .setPositiveButton(R.string.guess_submit, null)
            .setNegativeButton(R.string.cancel, null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        double gx = Double.parseDouble(x.getText().toString()),
                            gz = Double.parseDouble(z.getText().toString());
                        if (!Double.isFinite(gx)
                            || !Double.isFinite(gz)
                            || gx < 0
                            || gx > 30
                            || gz < 0
                            || gz > 20) throw new NumberFormatException();
                        guess(gx, gz);
                        dialog.dismiss();
                      } catch (NumberFormatException e) {
                        x.setError("Use x from 0 to 30 and z from 0 to 20 metres.");
                      }
                    }));
    dialog.show();
  }

  private void enterAr() {
    arMode = true;
    installRequested = false;
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      if (getPreferences(MODE_PRIVATE).getBoolean("cameraAsked", false)
          && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
        leaveAr();
        new AlertDialog.Builder(this)
            .setMessage(R.string.camera_settings)
            .setPositiveButton(
                R.string.settings,
                (d, w) ->
                    startActivity(
                        new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()))))
            .setNegativeButton(R.string.cancel, null)
            .show();
        return;
      }
      getPreferences(MODE_PRIVATE).edit().putBoolean("cameraAsked", true).apply();
      requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST);
    } else startArIfReady();
    refresh();
  }

  private void startArIfReady() {
    if (!foreground || !arMode || surfaceRunning || checkingSupport) return;
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
      return;
    checkingSupport = true;
    showStatus(getString(R.string.checking_ar));
    ArCoreApk.getInstance()
        .checkAvailabilityAsync(
            this,
            availability ->
                runOnUiThread(
                    () -> {
                      checkingSupport = false;
                      if (!foreground || !arMode || isDestroyed()) return;
                      if (!availability.isSupported()) {
                        leaveAr();
                        showStatus(getString(R.string.ar_unavailable));
                        return;
                      }
                      try {
                        if (session == null) {
                          if (ArCoreApk.getInstance().requestInstall(this, !installRequested)
                              == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                            installRequested = true;
                            return;
                          }
                          session = new Session(this);
                          Config config = new Config(session);
                          config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
                          config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
                          session.configure(config);
                        }
                        renderer.session(session);
                        session.resume();
                        preview.setVisibility(View.GONE);
                        surface.setVisibility(View.VISIBLE);
                        surface.onResume();
                        surfaceRunning = true;
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        refresh();
                      } catch (Exception failure) {
                        android.util.Log.e("PoolsideAR", "Session start failed", failure);
                        leaveAr();
                        showStatus(getString(R.string.ar_error));
                      }
                    }));
  }

  private void pauseAr() {
    if (surfaceRunning) {
      surface.onPause();
      surfaceRunning = false;
    }
    if (session != null) session.pause();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  private void leaveAr() {
    pauseAr();
    arMode = false;
    surface.setVisibility(View.GONE);
    preview.setVisibility(View.VISIBLE);
    showStatus(getString(R.string.preview_status));
    refresh();
  }

  @Override
  protected void onResume() {
    super.onResume();
    foreground = true;
    if (state != null) {
      if (resumePlayback) state.setPlaying(true, System.nanoTime());
      handler.post(update);
      startArIfReady();
    }
  }

  @Override
  protected void onPause() {
    foreground = false;
    handler.removeCallbacks(update);
    if (state != null) {
      resumePlayback = state.snapshot(System.nanoTime()).playing;
      state.setPlaying(false, System.nanoTime());
      pauseAr();
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (renderer != null) renderer.close();
    if (session != null) session.close();
    super.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    if (state != null) {
      ViewerState.Snapshot s = state.snapshot(System.nanoTime());
      out.putInt("sport", s.sport.ordinal());
      out.putDouble("position", (double) s.frame.timeMs / s.durationMs);
      out.putDouble("speed", s.speed);
      out.putBoolean("playing", foreground ? s.playing : resumePlayback);
      out.putFloat("scale", tableLength);
    }
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    if (request == CAMERA_REQUEST) {
      if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startArIfReady();
      else {
        leaveAr();
        showStatus(getString(R.string.camera_denied));
      }
    }
  }

  void showStatus(String value) {
    if (value.equals(lastStatus)) return;
    lastStatus = value;
    runOnUiThread(
        () -> {
          if (!isDestroyed() && status != null) status.setText(value);
        });
  }

  void showGame(String value) {
    runOnUiThread(
        () -> {
          if (!isDestroyed() && game != null) {
            game.setText(value);
            refresh();
          }
        });
  }

  private void openUrl(String url) {
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (ActivityNotFoundException e) {
      new AlertDialog.Builder(this).setMessage(url).setPositiveButton(R.string.close, null).show();
    }
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private LinearLayout row() {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    return row;
  }

  private TextView label(String value, int sp, int color) {
    TextView text = new TextView(this);
    text.setText(value);
    text.setTextSize(sp);
    text.setTextColor(color);
    text.setPadding(0, dp(3), 0, dp(3));
    return text;
  }

  private Button button(int text, LinearLayout row) {
    Button button = new Button(this);
    button.setText(text);
    button.setAllCaps(false);
    button.setTextSize(13);
    button.setTextColor(INK);
    button.setMinHeight(dp(48));
    button.setMinimumHeight(dp(48));
    if (row != null) row.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
    return button;
  }
}
