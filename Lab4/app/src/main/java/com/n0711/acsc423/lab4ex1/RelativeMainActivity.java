package com.n0711.acsc423.lab4ex1;

/**
 * Same first screen, built with RelativeLayout (res/layout/activity_main_relative.xml).
 * Both layouts use the same view ids, so the submit logic in MainActivity works for both.
 */
public class RelativeMainActivity extends MainActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main_relative;
    }
}
