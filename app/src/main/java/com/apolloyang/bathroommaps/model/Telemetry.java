package com.apolloyang.bathroommaps.model;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by julianlo on 10/27/15.
 */
public class Telemetry {
    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;

    private static String DEFAULT_TRACKER_ID = "UA-49886063-4";

    public static void initialize(Application application) {
        sAnalytics = GoogleAnalytics.getInstance(application);
        sAnalytics.setLocalDispatchPeriod(1800);
        sAnalytics.enableAutoActivityReports(application);

        sTracker = sAnalytics.newTracker(DEFAULT_TRACKER_ID);
        sTracker.enableExceptionReporting(true);
        sTracker.enableAdvertisingIdCollection(true); // TODO: Remove?
        sTracker.enableAutoActivityTracking(true);
    }

    public static void sendNullBathroomListFromApiEvent() {
        sendEvent("Errors", "GetBathrooms", "Null bathroom list object", 0);
    }

    public static void sendServerDownOnRefreshEvent() {
        sendEvent("Error", "GetBathrooms", "Server down", 0);
    }

    public static void sendApiErrorEvent(String api, String desc, long value) {
        sendEvent("Error", api, desc, value);
    }

    public static void sendTestEvent() {
        sendEvent("TestCategory", "Test", "Testing", 1);
    }

    private static void sendEvent(String category, String action, String label, long value) {
        sTracker.send(
                new HitBuilders.EventBuilder()
                        .setCategory(category)
                        .setAction(action)
                        .setLabel(label)
                        .setValue(value)
                        .build());
    }
}
