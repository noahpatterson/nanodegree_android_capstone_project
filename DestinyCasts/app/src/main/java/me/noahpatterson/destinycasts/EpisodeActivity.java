package me.noahpatterson.destinycasts;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import me.noahpatterson.destinycasts.data.PodcastContract;
import me.noahpatterson.destinycasts.data.PodcastDBHelper;

public class EpisodeActivity extends AppCompatActivity {
    private Context context;

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
                            PodcastContract.EpisodeEntry.COLUMN_URL
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
                TextView episodeDetailDescription = (TextView) findViewById(R.id.episodeDetailTitle);
                episodeDetailDescription.setText(Html.fromHtml(description).toString());
            }
            c.close();
        }
    }

}
