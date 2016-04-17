package me.noahpatterson.destinycasts.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by noahpatterson on 4/10/16.
 */
public class PodcastDBHelper extends SQLiteOpenHelper {
        public static final String LOG_TAG = PodcastDBHelper.class.getSimpleName();

        //name & version
        private static final String DATABASE_NAME = "podcast.db";
        private static final int DATABASE_VERSION = 5;

        public PodcastDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // Create the database
        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            final String SQL_CREATE_PODCAST_TABLE = "CREATE TABLE " +
                    PodcastContract.PodcastEntry.TABLE_PODCAST + "(" + PodcastContract.PodcastEntry._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PodcastContract.PodcastEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                    PodcastContract.PodcastEntry.COLUMN_DESCRIPTION +
                    " TEXT NOT NULL, " +
                    PodcastContract.PodcastEntry.COLUMN_PODCAST_URL +
                    " TEXT NOT NULL, " +
                    PodcastContract.PodcastEntry.COLUMN_SUBTITLE +
                    " TEXT NOT NULL);";

            final String SQL_CREATE_EPISODE_TABLE = "CREATE TABLE " +
                    PodcastContract.EpisodeEntry.TABLE_EPISODE + "(" + PodcastContract.EpisodeEntry._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PodcastContract.EpisodeEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                    PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION +
                    " TEXT NOT NULL, " +
                    PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL +
                    " TEXT NOT NULL, " +
                    PodcastContract.EpisodeEntry.COLUMN_PUB_DATE +
                    " INTEGER NOT NULL, " +
                    PodcastContract.EpisodeEntry.COLUMN_URL +
                    " TEXT NOT NULL, " +
                    PodcastContract.EpisodeEntry.COLUMN_PODCAST_ID + " INTEGER NOT NULL, " +
                    // Set up the location column as a foreign key to location table.
                    " FOREIGN KEY (" + PodcastContract.EpisodeEntry.COLUMN_PODCAST_ID + ") REFERENCES " +
                    PodcastContract.PodcastEntry.TABLE_PODCAST + " (" + PodcastContract.PodcastEntry._ID + "), " +
                    " UNIQUE(" + PodcastContract.EpisodeEntry.COLUMN_URL + "))";

            sqLiteDatabase.execSQL(SQL_CREATE_PODCAST_TABLE);
            sqLiteDatabase.execSQL(SQL_CREATE_EPISODE_TABLE);
        }

        // Upgrade database when version is changed.
        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to " +
                    newVersion + ". OLD DATA WILL BE DESTROYED");
            // Drop the table
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PodcastContract.PodcastEntry.TABLE_PODCAST);
            sqLiteDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                    PodcastContract.PodcastEntry.TABLE_PODCAST + "'");

            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PodcastContract.EpisodeEntry.TABLE_EPISODE);
            sqLiteDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                    PodcastContract.EpisodeEntry.TABLE_EPISODE + "'");

            // re-create database
            onCreate(sqLiteDatabase);
        }
}
