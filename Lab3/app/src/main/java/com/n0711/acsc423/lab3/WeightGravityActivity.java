package com.n0711.acsc423.lab3;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Slides 7 and 8: three buttons with layout_weight 1, 2 and 3 and layout_gravity left, center and right.
 */
public class WeightGravityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_gravity);
    }
}
