package me.noahpatterson.destinycasts;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class EpisodeActivity extends AppCompatActivity {
    private static final String LOG = "EpisodeActivity";

    // Service broacast constants
    public static final String ACTION_PLAY_TRACK = "me.noahpatterson.destinycasts.PLAY_TRACK";
    public static final String ACTION_PAUSE_TRACK = "me.noahpatterson.destinycasts.PAUSE_TRACK";
    public static final String ACTION_SEEK_TRACK = "me.noahpatterson.destinycasts.SEEK_TRACK";
    public static final String SEEK_POSITION = "seek_position";
    public static final String TRACK_PREVIEW_URL = "previewURL";
    public static final String EPISODE_NAME = "episodeName";
    public static final String PODCAST_TITLE = "podcastTitle";
    public static final String PODCAST_ID = "podcastID";

    // onSaveInstantState constants
    public static final String IS_PLAYING = "playing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Context context = this;
        int podcastID = getIntent().getIntExtra("podcast_id", 0);

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putInt("podcast_id", podcastID);
            EpisodeDetailFragment episodeDetailFragment = new EpisodeDetailFragment();
            episodeDetailFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_detail_container, episodeDetailFragment)
                    .commit();
        }


        //set analytics tracker
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        Tracker mTracker = application.getDefaultTracker();
    }

    @Override
    public void onStart() {
        Log.d(LOG, "in onStart");
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        super.onStart();

    }

    @Override
    public void onResume() {
        Log.d(LOG, "in onResume");
        super.onResume();


    }

    @Override
    public void onPause() {
        Log.d(LOG, "in onPause");
        super.onPause();


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG, "in onSaveInstanceState");

        // save whether the track is playing
        Boolean playing = false;
        outState.putBoolean(IS_PLAYING, playing);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        Log.d(LOG, "in onStop");
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG, "in onDestroy");
        super.onDestroy();
    }



}
