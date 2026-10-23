package com.n0711.acsc423.lab4ex1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

/**
 * First screen, built with ConstraintLayout (res/layout/activity_main.xml).
 * When SUBMIT is pressed the name is put in the Intent as an extra and SecondActivity is started.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        EditText txtName = findViewById(R.id.txtName);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String name = txtName.getText().toString().trim();
            if (name.isEmpty()) {
                txtName.setError(getString(R.string.error_empty_name));
                return;
            }

            // Pass the name as an argument to the second activity
            Intent intent = new Intent(this, SecondActivity.class);
            intent.putExtra(SecondActivity.EXTRA_NAME, name);
            startActivity(intent);
        });
    }

    /** The layout to show. RelativeMainActivity overrides this to use the RelativeLayout version. */
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.activity_main;
    }
}
