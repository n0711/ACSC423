package edu.frederick.poolar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PoolUiTest {
  private ActivityScenario<PoolArActivity> scenario;

  @Before
  public void launch() {
    scenario = ActivityScenario.launch(PoolArActivity.class);
  }

  @After
  public void close() {
    scenario.close();
  }

  @Test
  public void opensPreviewWithoutRequestingCamera() {
    onView(withText(R.string.title)).check(matches(isDisplayed()));
    onView(withText(R.string.open_ar)).perform(scrollTo()).check(matches(isDisplayed()));
    scenario.onActivity(
        a ->
            assertEquals(
                PackageManager.PERMISSION_DENIED,
                a.checkSelfPermission(Manifest.permission.CAMERA)));
  }

  @Test
  public void pauseAndResume() {
    onView(withText(R.string.pause)).perform(scrollTo(), click());
    onView(withText(R.string.play)).check(matches(isDisplayed())).perform(click());
    onView(withText(R.string.pause)).check(matches(isDisplayed()));
  }

  @Test
  public void swimmingDisablesBallChallenge() {
    onView(withText(R.string.swimming)).perform(scrollTo(), click());
    onView(withText(R.string.find_ball)).perform(scrollTo()).check(matches(not(isEnabled())));
    onView(withText(R.string.water_polo)).perform(scrollTo(), click());
    onView(withText(R.string.find_ball)).perform(scrollTo()).check(matches(isEnabled()));
  }

  @Test
  public void challengeCanBeCompletedWithoutTouchingCanvas() {
    onView(withText(R.string.find_ball)).perform(scrollTo(), click());
    onView(withText(R.string.play)).check(matches(isDisplayed()));
    onView(withText(R.string.enter_guess)).perform(scrollTo(), click());
    onView(withHint(R.string.guess_x)).perform(typeText("10"));
    onView(withHint(R.string.guess_z)).perform(typeText("5"), closeSoftKeyboard());
    onView(withText(R.string.guess_submit)).perform(click());
    onView(withText(containsString("away. Play to continue."))).check(matches(isDisplayed()));
  }

  @Test
  public void invalidGuessKeepsDialogOpen() {
    onView(withText(R.string.find_ball)).perform(scrollTo(), click());
    onView(withText(R.string.enter_guess)).perform(scrollTo(), click());
    onView(withHint(R.string.guess_x)).perform(typeText("999"));
    onView(withHint(R.string.guess_z)).perform(typeText("5"), closeSoftKeyboard());
    onView(withText(R.string.guess_submit)).perform(click());
    onView(withHint(R.string.guess_x))
        .check(matches(hasErrorText("Use x from 0 to 30 and z from 0 to 20 metres.")));
    onView(withText(R.string.cancel)).perform(click());
  }

  @Test
  public void recreationPreservesSportAndPause() {
    onView(withText(R.string.swimming)).perform(scrollTo(), click());
    onView(withText(R.string.pause)).perform(scrollTo(), click());
    scenario.recreate();
    onView(withText(R.string.play)).perform(scrollTo()).check(matches(isDisplayed()));
    onView(withText(R.string.find_ball)).check(matches(not(isEnabled())));
  }

  @Test
  public void backgroundResumeKeepsManualPause() {
    onView(withText(R.string.pause)).perform(scrollTo(), click());
    scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
    scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
    onView(withText(R.string.play)).check(matches(isDisplayed()));
  }

  @Test
  public void timelineCanSeekAndSurviveRecreation() {
    onView(withText(R.string.pause)).perform(scrollTo(), click());
    onView(withContentDescription(R.string.timeline))
        .perform(
            scrollTo(),
            new androidx.test.espresso.ViewAction() {
              public org.hamcrest.Matcher<View> getConstraints() {
                return isAssignableFrom(SeekBar.class);
              }

              public String getDescription() {
                return "Seek to the middle using a keyboard accessibility action";
              }

              public void perform(androidx.test.espresso.UiController controller, View view) {
                android.os.Bundle args = new android.os.Bundle();
                args.putFloat(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,
                    500);
                view.performAccessibilityAction(
                    android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
                        .ACTION_SET_PROGRESS
                        .getId(),
                    args);
                controller.loopMainThreadUntilIdle();
              }
            });
    scenario.recreate();
    onView(withContentDescription(R.string.timeline))
        .perform(scrollTo())
        .check(
            (view, error) -> {
              if (error != null) throw error;
              assertEquals(500, ((SeekBar) view).getProgress());
            });
  }

  @Test
  public void landscapeShowsPoolAndPlaybackTogether() {
    scenario.onActivity(
        a ->
            a.setRequestedOrientation(
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    onView(withClassName(endsWith("PoolPreview"))).check(matches(isCompletelyDisplayed()));
    onView(withText(R.string.pause)).check(matches(isDisplayed()));
    scenario.onActivity(
        a ->
            a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
  }

  @Test
  public void helpExplainsSyntheticData() {
    onView(withText(R.string.help)).perform(scrollTo(), click());
    onView(withText(R.string.help_body)).check(matches(isDisplayed()));
    onView(withText(R.string.close)).perform(click());
  }

  @Test
  public void restartResumesPausedReplay() {
    onView(withText(R.string.pause)).perform(scrollTo(), click());
    onView(withText(R.string.restart)).perform(click());
    onView(withText(R.string.pause)).check(matches(isDisplayed()));
  }
}
