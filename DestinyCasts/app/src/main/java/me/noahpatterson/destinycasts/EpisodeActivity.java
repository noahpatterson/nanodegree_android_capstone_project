package me.noahpatterson.destinycasts;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import me.noahpatterson.destinycasts.service.PlayerService;
import me.noahpatterson.destinycasts.data.PodcastContract;
import me.noahpatterson.destinycasts.data.PodcastDBHelper;
import me.noahpatterson.destinycasts.EpisodeDetailFragment;

public class EpisodeActivity extends AppCompatActivity {
    private static final int ONE_MILLI_SECOND = 1000;
    private Context context;

    private Boolean playing = false;
    private String playingURL;
    private int seek = 0;
    private boolean hasService = false;
    private static final String LOG = "EpisodeActivity";

    private int totalTrackLength;
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
    public static final String PLAYING_URL = "playingURL";
    public static final String IS_PLAYING = "playing";

    //episode data
    private String episodeUrl;
    private String episodeName;
    private String podcastTitle;
    private int podcastID;

    //play area
    private ImageButton playButton;
    private SeekBar seekBar;
    private TextView trackTimeTextView;
    private InterstitialAd mInterstitialAd;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = this;
        podcastID = getIntent().getIntExtra("podcast_id",0);

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putInt("podcast_id", podcastID);
//            EpisodeDetailFragment episodeDetailFragment = new EpisodeDetailFragment();
            EpisodeDetailFragment episodeDetailFragment = new EpisodeDetailFragment();
            episodeDetailFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_detail_container, episodeDetailFragment)
                    .commit();
        }


        //set analytics tracker
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
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
