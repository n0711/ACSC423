package com.n0711.acsc423.lab3;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Slides 12 to 17: the TextView from slide 15 constrained to the parent, plus a
 * baseline constraint and the three size modes (wrap content, any size, fixed size).
 */
public class ConstraintLayoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_constraint_layout);
    }
}
