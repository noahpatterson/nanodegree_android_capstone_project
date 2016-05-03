package me.noahpatterson.destinycasts.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.Random;

import me.noahpatterson.destinycasts.EpisodeActivity;
import me.noahpatterson.destinycasts.R;
import me.noahpatterson.destinycasts.Utilities;
import me.noahpatterson.destinycasts.WeeklyListActivity;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final String ACTION_CURR_POSITION = "me.noahpatterson.destinycasts.CURR_POSITION";
    public static final String ACTION_COMPLETE = "me.noahpatterson.destinycasts.CURR_COMPLETE";
    private MediaPlayer mMediaPlayer;
    private String playingURL = null;
    private Thread updaterThread;
    private Handler handler;
    private int seek = 0;
    private String LOG = "player service";

    // Intent String Constants
    public static final String CURR_TRACK_POSITION = "current_track_position";
    public static final String PLAYING_URL = "playingURL";
    public static final String EPISODE_NAME = "EPISODE_NAME";
    public static final String PODCAST_TITLE = "PODCAST_TITLE";
    public static final String PODCAST_ID = "PODCAST_ID";

    //player data
    private String episodeName;
    private String podcastTitle;
    private int podcastId;

    //player notification id
    private static int NOTIFICATION_ID = 101;

    // this controls updating the seekBar and time while playing
    private void startUpdater() {
        updaterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread myCurrent = Thread.currentThread();
                while (!myCurrent.isInterrupted() && updaterThread == myCurrent) {
                    notifyUpdate();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(LOG, "Error pausing update thread");
                        break;
                    }
                }
            }
        });
        updaterThread.start();
    }

    private void notifyStart() {
        startUpdater();
    }

    // sends a broadcast to the fragment to update seek and track time in real time
    private void notifyUpdate() {
        Log.d(LOG, "updating seekbar");
        if (mMediaPlayer != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                   Intent currPositionIntent = new Intent(ACTION_CURR_POSITION);
                    currPositionIntent.putExtra(CURR_TRACK_POSITION, mMediaPlayer.getCurrentPosition());
                    currPositionIntent.putExtra(PLAYING_URL, playingURL);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(currPositionIntent);
                }
            });
        }
    }

    // random name for the service for logging
    private String name = "PlayerService" + new Random().nextInt();

    @Override
    public void onCreate() {
        Log.d(LOG, "in onCreate: " + name);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        handler = new Handler();

        // register receivers for the pause, play, and seek actions from the fragment
        IntentFilter playTrackFilter = new IntentFilter(EpisodeActivity.ACTION_PLAY_TRACK);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(playTrackReciever, playTrackFilter);

        IntentFilter pauseTrackFilter = new IntentFilter(EpisodeActivity.ACTION_PAUSE_TRACK);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(pauseTrackReciever, pauseTrackFilter);

        IntentFilter seekTrackFilter = new IntentFilter(EpisodeActivity.ACTION_SEEK_TRACK);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(seekTrackReciever, seekTrackFilter);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG, "in onStartCommand: " + name);
        // moved normal implementation to the broadcast receivers because it seems easier to manipulate
        String previewURL = intent.getStringExtra(EpisodeActivity.TRACK_PREVIEW_URL);

        //get player data
        episodeName = intent.getStringExtra(EPISODE_NAME);
        podcastTitle = intent.getStringExtra(PODCAST_TITLE);
        podcastId = intent.getIntExtra(PODCAST_ID, 0);

        try {
            mMediaPlayer.setDataSource(previewURL);
        } catch(IllegalArgumentException e) {
            Log.e("PlayTrackService start", "malformed url");
        } catch (IOException e) {
            Log.e("PlayTrackService start", "track may not exist");
        }

        mMediaPlayer.prepareAsync(); // prepare async to not block main thread
        playingURL = previewURL;
        seek = intent.getIntExtra(EpisodeActivity.SEEK_POSITION, 0);
        return 1;
    }

    private void playTrack(Intent intent, String previewURL) {
        // play request to start an existing track
        if (playingURL != null && playingURL.equals(previewURL)) {
            mMediaPlayer.seekTo(intent.getIntExtra(EpisodeActivity.SEEK_POSITION, 0));
            notifyStart();
            mMediaPlayer.start();
        }
        // otherwise play the newly selected track
        else
        {
            mMediaPlayer.reset();
            try {
                mMediaPlayer.setDataSource(previewURL);
            } catch(IllegalArgumentException e) {
                Log.e("PlayTrackService start", "malformed url");
            } catch (IOException e) {
                Log.e("PlayTrackService start", "track may not exist");
            }

            mMediaPlayer.prepareAsync(); // prepare async to not block main thread
            playingURL = previewURL;
            seek = intent.getIntExtra(EpisodeActivity.SEEK_POSITION, 0);
        }
        showNotification();
    }

    private BroadcastReceiver playTrackReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG, "in playTrackReciever");
            String previewURL = intent.getStringExtra(EpisodeActivity.TRACK_PREVIEW_URL);

            playTrack(intent, previewURL);
        }
    };

    // receives a pause command from the fragment
    private BroadcastReceiver pauseTrackReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG, "in pauseTrackReciever");
            mMediaPlayer.pause();
            updaterThread.interrupt();
        }
    };

    // receives a seek command from the fragment
    private BroadcastReceiver seekTrackReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG, "in seekTrackReciever");
            int seekPosition = intent.getIntExtra(EpisodeActivity.SEEK_POSITION,0);

            // only seek if there is a player to seek on
            if (mMediaPlayer != null) {
                mMediaPlayer.seekTo(seekPosition);
            }
        }
    };

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer player) {
        Log.d(LOG, "in onPrepared: "+ name);
//        player.seekTo(seek);
//        notifyStart();
//        player.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("PlayTrackService start", "Error: " + what + ", " + extra);
        return true;
    }
    @Override

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(LOG, "in onCompletion: "+ name);
        updaterThread.interrupt();
        playingURL = null;
        mp.reset();

        // send a track completed broadcast to the fragment
        Intent playerComplete = new Intent(ACTION_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(playerComplete);
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG, "in onDestroy: "+ name);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(playTrackReciever);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(pauseTrackReciever);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(seekTrackReciever);
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (updaterThread != null) {
            updaterThread.interrupt();
        }
        stopForeground(true);
    }

//    public int getTotalTrackTime() {
//        if (mMediaPlayer != null) {
//            return mMediaPlayer.getDuration();
//        }
//        return 0;
//    }

    private void showNotification() {
        // The PendingIntent to launch our activity if the user selects this notification
        Intent contentIntent = new Intent(this, EpisodeActivity.class);
        contentIntent.putExtra("podcast_id", podcastId);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(EpisodeActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(contentIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        Bitmap largePodcastIcon = BitmapFactory.decodeResource(getResources(),
                Utilities.findPodcastImage(podcastTitle));

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)  // the status icon
//                .setTicker(text)  // the status text
//                .setWhen(System.currentTimeMillis())  // the time stamp
                .setLargeIcon(largePodcastIcon)
                .setContentTitle(episodeName)  // the label of the entry
                .setContentText(podcastTitle)  // the contents of the entry
                .setContentIntent(resultPendingIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        startForeground(NOTIFICATION_ID,
                notification);
    }
}
