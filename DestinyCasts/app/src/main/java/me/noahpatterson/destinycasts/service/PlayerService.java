package me.noahpatterson.destinycasts.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.Random;

import me.noahpatterson.destinycasts.EpisodeActivity;

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
        player.seekTo(seek);
        notifyStart();
        player.start();
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
    }

//    public int getTotalTrackTime() {
//        if (mMediaPlayer != null) {
//            return mMediaPlayer.getDuration();
//        }
//        return 0;
//    }
}
