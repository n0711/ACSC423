package com.n0711.acsc423.lab3;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Home screen of the lab. Each button opens one of the layout examples from the slides.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Slide 2: the XML UI in res/layout is loaded at runtime with setContentView()
        setContentView(R.layout.activity_main);

        openOnClick(R.id.btnLinearLayout, LinearLayoutActivity.class);
        openOnClick(R.id.btnWeightGravity, WeightGravityActivity.class);
        openOnClick(R.id.btnNestedLinearLayout, NestedLinearLayoutActivity.class);
        openOnClick(R.id.btnRelativeLayout, RelativeLayoutActivity.class);
        openOnClick(R.id.btnConstraintLayout, ConstraintLayoutActivity.class);
    }

    /** Starts the given activity when the button with this id is tapped. */
    private void openOnClick(int buttonId, Class<?> activity) {
        findViewById(buttonId).setOnClickListener(v -> startActivity(new Intent(this, activity)));
    }
}
