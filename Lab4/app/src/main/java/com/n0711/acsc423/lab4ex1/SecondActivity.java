package com.n0711.acsc423.lab4ex1;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Second screen. Reads the name that MainActivity passed in the Intent and shows it.
 */
public class SecondActivity extends AppCompatActivity {

    /** Key of the Intent extra that carries the name */
    public static final String EXTRA_NAME = "com.n0711.acsc423.lab4ex1.EXTRA_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        String name = getIntent().getStringExtra(EXTRA_NAME);

        TextView lblUserName = findViewById(R.id.lblUserName);
        lblUserName.setText(name);
    }
}
