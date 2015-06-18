package com.matthijswillems.cervicapp;

import android.app.Application;
import android.widget.Toast;

//this class is fired once on application launch
public class MyApp extends Application {
    public static boolean doOnce;

    public MyApp() {


    }

    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(MyApp.this, "Welcome to Cervicapp", Toast.LENGTH_SHORT).show();
        doOnce = true;

    }
}