package me.noahpatterson.destinycasts;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import me.noahpatterson.destinycasts.service.PlayerService;
import me.noahpatterson.destinycasts.data.PodcastContract;
import me.noahpatterson.destinycasts.data.PodcastDBHelper;

public class EpisodeActivity extends AppCompatActivity {
    private Context context;

//    private ParcelableTrack parcelableTrack = null;
    private Boolean playing = false;
    private String playingURL;
    private int seek = 0;
    private View fragmentView;
//    private ArrayList<ParcelableTrack> mParcelableTrackArrayList;
    private int mCurrentTrackPosition;
//    private PlayerViewHolder mPlayerViewHolder;
    private boolean hasService = false;
    private static final String LOG = "PlayerFragment";

    private int totalTrackLength;
    // Service broacast constants
    public static final String ACTION_PLAY_TRACK = "me.noahpatterson.destinycasts.PLAY_TRACK";
    public static final String ACTION_PAUSE_TRACK = "me.noahpatterson.destinycasts.PAUSE_TRACK";
    public static final String ACTION_SEEK_TRACK = "me.noahpatterson.destinycasts.SEEK_TRACK";
    public static final String SEEK_POSITION = "seek_position";
    public static final String TRACK_PREVIEW_URL = "previewURL";

    // onSaveInstantState constants
    public static final String PLAYING_URL = "playingURL";
    public static final String CURR_TRACK = "parcelableTrack";
    public static final String CURRENT_TRACK_LIST_POSITION = "currentPosition";
    public static final String IS_PLAYING = "playing";

    //episode url
    private String episodeUrl;
    private ImageButton playButton;
    private SeekBar seekBar;
    private TextView trackTimeTextView;

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

        int podcastID = getIntent().getIntExtra("podcast_id", 0);
        if (podcastID != -1) {
            PodcastDBHelper dbHelper = new PodcastDBHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            Cursor c = db.query(PodcastContract.EpisodeEntry.TABLE_EPISODE,
                    new String[] {
                            PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE,
                            PodcastContract.EpisodeEntry.COLUMN_TITLE,
                            PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL,
                            PodcastContract.EpisodeEntry.COLUMN_PUB_DATE,
                            PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION,
                            PodcastContract.EpisodeEntry.COLUMN_URL,
                            PodcastContract.EpisodeEntry.COLUMN_EPISODE_LENGTH
                    },
                    PodcastContract.EpisodeEntry._ID + "=?",
                    new String[] {
                            Integer.toString(podcastID)
                    },null, null, null);
            if (c.getCount() > 0) {
                c.moveToFirst();

                //podcast title
                int podcastTitleIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE);
                final String podcastTitle = c.getString(podcastTitleIndex);

                //image
                int imageIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL);
                String imageUrl = c.getString(imageIndex);

                ImageView episodeImageView = (ImageView) findViewById(R.id.episodeDetailImageView);
                Glide.with(context).load(imageUrl.equals("") ? Utilities.findPodcastImage(podcastTitle) : imageUrl).into(episodeImageView);

                //title
                int episodeIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_TITLE);
                final String episodeName = c.getString(episodeIndex);
                TextView episodeTitle = (TextView) findViewById(R.id.episodeDetailTitle);
                episodeTitle.setText(episodeName);

                //date
                int episodeDateIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE);
                final long episodeDate = c.getLong(episodeDateIndex);
                SimpleDateFormat formatter = new SimpleDateFormat("MMM d", Locale.US);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(episodeDate);
                TextView episodeDetailDate = (TextView) findViewById(R.id.episodeDetailDate);
                episodeDetailDate.setText(formatter.format(calendar.getTime()));

                //description
                int descriptionIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION);
                final String description = c.getString(descriptionIndex);
                TextView episodeDetailDescription = (TextView) findViewById(R.id.episodeDetailDescription);
                episodeDetailDescription.setText(Html.fromHtml(description).toString());

                //url
                int episodeUrlIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_URL);
                episodeUrl = c.getString(episodeUrlIndex);

                //episode length
                int episodeLengthIndex = c.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_EPISODE_LENGTH);
                totalTrackLength = c.getInt(episodeLengthIndex) * 1000;
            }
            c.close();
        }

        //setting up player
        if (savedInstanceState != null) {
            playing = savedInstanceState.getBoolean(IS_PLAYING, false);
            playingURL = savedInstanceState.getString(PLAYING_URL, null);
//            parcelableTrack = savedInstanceState.getParcelable(CURR_TRACK);
//            mCurrentTrackPosition = savedInstanceState.getInt(CURRENT_TRACK_LIST_POSITION);
            seek = savedInstanceState.getInt(SEEK_POSITION);
        } else {
//            parcelableTrack = getIntent().getParcelableExtra(TopTracksFragment.CURR_TRACK);
//            mCurrentTrackPosition = getActivity().getIntent().getIntExtra(TopTracksFragment.CURRENT_TRACK_LIST_POSITION, 0);
        }

        // here we make sure only to start 1 playerService at a time
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            // if the service exists, don't start another one
            if (PlayerService.class.getName().equals(service.service.getClassName())) {
                Log.d(LOG, "service is running");
                hasService = true;
            }
        }
        // otherwise start a new service
        if (!hasService) {
            Intent startPlayerService = new Intent(this, PlayerService.class);
            startPlayerService.putExtra(TRACK_PREVIEW_URL, episodeUrl);
            startService(startPlayerService);
        }

        //assign trackDuration
        //TODO: this should really be the preview track length, possibly obtained from mediaPlayer

        // make sure the play button is in the right state
        playButton = (ImageButton) findViewById(R.id.playerPlayButton);
        if (playing) {
            playButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playButton.setImageResource(android.R.drawable.ic_media_play);
        }

        // set playbutton click listener
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTrack();
            }
        });

        //set seekBar change listener
        //assign trackDuration
        //TODO: this should really be the preview track length, possibly obtained from mediaPlayer
        seekBar = (SeekBar) findViewById(R.id.playerSeekBar);
        trackTimeTextView = (TextView) findViewById(R.id.playerCurrentTrackPosition);
        TextView trackTimeTotalTextView = (TextView) findViewById(R.id.playerTotalTrackTime);
        String formattedDuration = new SimpleDateFormat("hh:mm:ss").format(new Date(totalTrackLength));
        trackTimeTotalTextView.setText(formattedDuration);
//        seekBar.setMax(PlayerService.totalTrackTime);
//        seekBar.setMax(30 * 1000);
        seekBar.setMax(totalTrackLength);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {

                    String formattedDuration = new SimpleDateFormat("mm:ss").format(new Date(progress));
                    trackTimeTextView.setText(formattedDuration);

                    // if the track is playing we need to tell the playerService to scrub the track
                    if (playing) {
                        Intent intent = new Intent(ACTION_SEEK_TRACK);
                        intent.putExtra(SEEK_POSITION, progress);
                        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                    }

                    // store how far we seek
                    seek = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void onStart() {
        Log.d(LOG, "in onStart");
        super.onStart();

    }

    @Override
    public void onResume() {
        Log.d(LOG, "in onResume");
        super.onResume();

        // start our PlayerService receivers
        IntentFilter currPositionFilter = new IntentFilter(PlayerService.ACTION_CURR_POSITION);
        LocalBroadcastManager.getInstance(this).registerReceiver(currPositionReciever, currPositionFilter);

        IntentFilter playerCompleteFilter = new IntentFilter(PlayerService.ACTION_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(playerCompleteReciever, playerCompleteFilter);

        //auto start track
//        if (!playing && !episodeUrl.equals(playingURL)) {
//            playTrack();
//        }
    }

    @Override
    public void onPause() {
        Log.d(LOG, "in onPause");
        super.onPause();

        // trash the playerService receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(currPositionReciever);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playerCompleteReciever);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG, "in onSaveInstanceState");

        // save whether the track is playing
        outState.putBoolean(IS_PLAYING, playing);

        //if we have a track, store it's data
//        if (parcelableTrack != null) {
//            outState.putString(PLAYING_URL, parcelableTrack.previewURL);
//            outState.putParcelable(CURR_TRACK, parcelableTrack);
//            outState.putInt(CURRENT_TRACK_LIST_POSITION, mCurrentTrackPosition);
//            outState.putInt(SEEK_POSITION, seek);
//        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        Log.d(LOG, "in onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG, "in onDestroy");
        super.onDestroy();
    }

    public void playTrack() {
        Log.d(LOG, "in playTrack");

        // if playing and the current selected track's URL matches the PlayingURL we pause the track
        if (playing && episodeUrl.equals(playingURL)) {

            //pause command
            Intent intent = new Intent(ACTION_PAUSE_TRACK);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            // we are no longer playing
            playing = false;

            // swap the pause button to play
            playButton.setImageResource(android.R.drawable.ic_media_play);

            // store the track's playing position
//            seek = mPlayerViewHolder.seekBar.getProgress();
        }

        // otherwise we start the selected track
        else {
            Intent intent = new Intent(ACTION_PLAY_TRACK);
            intent.putExtra(TRACK_PREVIEW_URL, episodeUrl);
            intent.putExtra(SEEK_POSITION, seek);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            playingURL = episodeUrl;
            playButton.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    // receives the currently playing tracks seek position every second and updateas the UI
    private BroadcastReceiver currPositionReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // PlayerService knows what it is playing so we update the view
//            if (playingURL == null) {
            playingURL = intent.getStringExtra(PlayerService.PLAYING_URL);
//            }

            // only update the seekbar and trackTime if we're viewing the playing track
            if (episodeUrl.equals(playingURL)) {
                int currPosition = intent.getIntExtra(PlayerService.CURR_TRACK_POSITION, 0);

                playButton.setImageResource(android.R.drawable.ic_media_pause);
                seekBar.setProgress(currPosition);

                String formattedDuration = new SimpleDateFormat("mm:ss").format(new Date(currPosition));
                trackTimeTextView.setText(formattedDuration);
                playing = true;

                //update seek position in real time
                seek = currPosition;

            }
        }
    };

    // knows when the track is finsihed playing. Resets the player UI
    private BroadcastReceiver playerCompleteReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playButton.setImageResource(android.R.drawable.ic_media_play);
            seekBar.setProgress(0);
            trackTimeTextView.setText(getString(R.string.trackStartTime));
            playing = false;
            seek = 0;
        }
    };

}
