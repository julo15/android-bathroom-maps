package com.apolloyang.bathroommaps;

import android.app.Application;

import com.apolloyang.bathroommaps.model.BathroomMapsAPI;
import com.apolloyang.bathroommaps.model.Telemetry;

/**
 * Created by julianlo on 10/27/15.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Telemetry.initialize(this);
        BathroomMapsAPI.initialize(this);
    }
}
