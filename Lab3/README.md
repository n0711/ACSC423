# Lab 3: User Interface

ACSC423 Mobile & Ubiquitous Computing and Applications, Frederick University.

An Android app in Java with one screen for each layout example in the Lab 3 slides. The home screen has a button for every example, and each example opens with its own title and a back arrow.

| Screen | Slides | Layout file | What it shows |
| --- | --- | --- | --- |
| LinearLayout | 4 | `activity_linear_layout.xml` | A vertical LinearLayout with a TextView that fills the width (`match_parent`) and wraps its height (`wrap_content`) |
| Layout weight & gravity | 7, 8 | `activity_weight_gravity.xml` | Three 160dp buttons with weights 1, 2 and 3, so they take about 16.6%, 33.3% and 50% of the height, with gravity left, center and right |
| Nested LinearLayout | 8 | `activity_nested_linear_layout.xml` | Text 1 to Text 3 in a column, then Button 1 to Button 3 in a horizontal LinearLayout nested inside the vertical one |
| RelativeLayout | 9 to 11 | `activity_relative_layout.xml` | The comments form: the label at the top start, a 100dp comments box below it, then Cancel and Save under the box, lined up with its start and end |
| ConstraintLayout | 12 to 17 | `activity_constraint_layout.xml` | The TextView from slide 15 tied to the parent's left, top and right with 16dp margins, plus a baseline constraint and the three size modes (wrap content, any size, fixed size) |

Every activity follows slide 2. Its `onCreate()` loads the XML layout from `res/layout` with `setContentView()`. The back arrow comes from `android:parentActivityName` in the manifest, so the example activities need no extra code.

## Differences from the slide code

The slides use some older names, so a few things are updated to build with current tools:

- `fill_parent` is now `match_parent`. They mean the same thing, and `fill_parent` has been deprecated since API 8.
- `android.support.constraint.ConstraintLayout` is now `androidx.constraintlayout.widget.ConstraintLayout`, because AndroidX replaced the old support library.
- Text is kept in `res/values/strings.xml` instead of being hardcoded, the same way slide 4 uses `@string/hello`.
- The comments box has `inputType="textMultiLine"` so the keyboard suits a longer comment.

## Running it

1. Open the `Lab3` folder in Android Studio (Otter 3 Feature Drop or newer).
2. Let Gradle sync. It downloads Gradle 9.1.0 and the API 36 SDK platform if they are missing.
3. Run the `app` configuration on an emulator or a phone with Android 8.0 or newer.

Build setup: Android Gradle plugin 9.0.1, Gradle 9.1.0, compileSdk and targetSdk 36, minSdk 26, Java 11.
