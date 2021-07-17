package com.jumperchuck.escpos;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PrinterManager.init(this);
    }
}
