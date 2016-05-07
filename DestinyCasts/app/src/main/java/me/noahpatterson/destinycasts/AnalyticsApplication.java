package me.noahpatterson.destinycasts;

/**
 * Created by noahpatterson on 5/6/16.
 */
import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class AnalyticsApplication extends Application {
    private Tracker mTracker;

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker_manual);
            // Enable tracking of activities
            analytics.enableAutoActivityReports(this);
            mTracker.enableAutoActivityTracking(true);

            // Set the log level to verbose.
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        }
        return mTracker;
    }
}

