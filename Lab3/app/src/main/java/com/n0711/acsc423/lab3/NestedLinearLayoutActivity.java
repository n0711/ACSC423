package com.n0711.acsc423.lab3;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Slide 8 (second screenshot): three TextViews in a column and a row of three
 * buttons, made with a horizontal LinearLayout inside a vertical one.
 */
public class NestedLinearLayoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested_linear_layout);
    }
}
