package me.noahpatterson.destinycasts.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.Random;

import me.noahpatterson.destinycasts.EpisodeActivity;
import me.noahpatterson.destinycasts.EpisodeWidget;
import me.noahpatterson.destinycasts.R;
import me.noahpatterson.destinycasts.Utilities;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final String ACTION_CURR_POSITION = "me.noahpatterson.destinycasts.CURR_POSITION";
    public static final String ACTION_COMPLETE = "me.noahpatterson.destinycasts.CURR_COMPLETE";
    private static final String ACTION_CLOSE = "me.noahpatterson.destinycasts.CLOSE";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
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
    private String previewURL;

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

        //handle close intent if present
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case PlayerService.ACTION_CLOSE:
                    updaterThread.interrupt();
                    playingURL = null;
                    mMediaPlayer.reset();

                    // send a track completed broadcast to the fragment
                    Intent playerComplete = new Intent(ACTION_COMPLETE);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(playerComplete);
                    stopForeground(true);
                    stopSelf();
                    return 1;
                case PlayerService.ACTION_PLAY:
                    Intent playingIntent = new Intent(this,PlayerService.class);
                    playingIntent.putExtra(EpisodeActivity.SEEK_POSITION, mMediaPlayer.getCurrentPosition());
                    playTrack(playingIntent,playingURL);

                    //send playing to fragment
                    Intent playerPlaying = new Intent(ACTION_PLAY);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(playerPlaying);
                    showNotification();
                    sendWidgetUpdate();
                    return 1;
                case PlayerService.ACTION_PAUSE:
                    mMediaPlayer.pause();
                    updaterThread.interrupt();

                    //send pause to fragment
                    Intent playerPaused = new Intent(ACTION_PAUSE);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(playerPaused);
                    showNotification();
                    sendWidgetUpdate();
                    return 1;
            }

        }

        // moved normal implementation to the broadcast receivers because it seems easier to manipulate
        previewURL = intent.getStringExtra(EpisodeActivity.TRACK_PREVIEW_URL);

        //get player data
        episodeName = intent.getStringExtra(EPISODE_NAME);
        podcastTitle = intent.getStringExtra(PODCAST_TITLE);
        podcastId = intent.getIntExtra(PODCAST_ID, 0);


        return 1;
    }

    private void playTrack(Intent intent, String previewURL) {
        // play request to start an existing track
        if (playingURL != null && playingURL.equals(previewURL)) {
            playingURL = previewURL;
            seek = intent.getIntExtra(EpisodeActivity.SEEK_POSITION, 0);
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
            previewURL = intent.getStringExtra(EpisodeActivity.TRACK_PREVIEW_URL);
            episodeName = intent.getStringExtra(EpisodeActivity.EPISODE_NAME);
            podcastTitle = intent.getStringExtra(EpisodeActivity.PODCAST_TITLE);
            podcastId = intent.getIntExtra(EpisodeActivity.PODCAST_ID,0);

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
            showNotification();
            sendWidgetUpdate();
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
        showNotification();
        sendWidgetUpdate();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
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

    private void showNotification() {
        Bitmap largePodcastIcon = BitmapFactory.decodeResource(getResources(),
                Utilities.findPodcastImage(podcastTitle));
        //setup custom notification layout
        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.player_notification);
        remoteViews.setImageViewBitmap(R.id.episodeNotficationImageView,largePodcastIcon);
        remoteViews.setContentDescription(R.id.episodeNotficationImageView, podcastTitle +  getString(R.string.content_desc_logo));
        remoteViews.setTextViewText(R.id.notificationEpisodeName, episodeName);
        remoteViews.setTextViewText(R.id.notificationPodcastName, podcastTitle);

        //create close/stop service pending intent
        Intent deleteIntent = new Intent(this, PlayerService.class);
        deleteIntent.setAction(PlayerService.ACTION_CLOSE);
        PendingIntent deletePendingIntent = PendingIntent.getService(this,
                0,
                deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.closeNotificationButton, deletePendingIntent);

        //create play pending intent
        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction(PlayerService.ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this,
                0,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //create pause pending intent
        Intent pauseIntent = new Intent(this, PlayerService.class);
        pauseIntent.setAction(PlayerService.ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this,
                0,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (mMediaPlayer.isPlaying()) {
            remoteViews.setImageViewResource(R.id.notificationPlayPauseButton, android.R.drawable.ic_media_pause);
            remoteViews.setOnClickPendingIntent(R.id.notificationPlayPauseButton, pausePendingIntent);
        } else {
            remoteViews.setImageViewResource(R.id.notificationPlayPauseButton, android.R.drawable.ic_media_play);
            remoteViews.setOnClickPendingIntent(R.id.notificationPlayPauseButton, playPendingIntent);
        }

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



        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.destiny_logo)  // the status icon
                .setContentIntent(resultPendingIntent)  // The intent to send when the entry is clicked
                .setContent(remoteViews)
                .build();
        notification.bigContentView =remoteViews;

        // Send the notification.
        startForeground(NOTIFICATION_ID,
                notification);
    }

    private void sendWidgetUpdate() {
        //setup widge remote views
        Bitmap largePodcastIcon = BitmapFactory.decodeResource(getResources(),
                Utilities.findPodcastImage(podcastTitle));
        //setup custom notification layout
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.episode_widget);
        remoteViews.setImageViewBitmap(R.id.episodeNotficationImageView, largePodcastIcon);
        remoteViews.setContentDescription(R.id.episodeNotficationImageView, podcastTitle +  getString(R.string.content_desc_logo));
        remoteViews.setTextViewText(R.id.notificationEpisodeName, episodeName);
        remoteViews.setTextViewText(R.id.notificationPodcastName, podcastTitle);

        //create play pending intent
        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction(PlayerService.ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this,
                0,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //create pause pending intent
        Intent pauseIntent = new Intent(this, PlayerService.class);
        pauseIntent.setAction(PlayerService.ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this,
                0,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (mMediaPlayer.isPlaying()) {
            remoteViews.setImageViewResource(R.id.notificationPlayPauseButton, android.R.drawable.ic_media_pause);
            remoteViews.setOnClickPendingIntent(R.id.notificationPlayPauseButton, pausePendingIntent);
        } else {
            remoteViews.setImageViewResource(R.id.notificationPlayPauseButton, android.R.drawable.ic_media_play);
            remoteViews.setOnClickPendingIntent(R.id.notificationPlayPauseButton, playPendingIntent);
        }

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

        remoteViews.setOnClickPendingIntent(R.id.widgetParent, resultPendingIntent);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName episodeWidget = new ComponentName(this, EpisodeWidget.class);
        widgetManager.updateAppWidget(episodeWidget, remoteViews);
    }

}
