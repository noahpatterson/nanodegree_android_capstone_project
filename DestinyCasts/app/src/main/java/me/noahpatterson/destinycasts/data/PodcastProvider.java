package me.noahpatterson.destinycasts.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Created by noahpatterson on 4/10/16.
 */
public class PodcastProvider extends ContentProvider {
    private static final String LOG_TAG = PodcastProvider.class.getSimpleName();
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private PodcastDBHelper mOpenHelper;

    // Codes for the UriMatcher //////
    private static final int PODCAST = 100;
    private static final int PODCAST_WITH_ID = 200;
    private static final int EPISODE = 300;
    private static final int EPISODE_WITH_ID = 400;
    ////////

    private static UriMatcher buildUriMatcher(){
        // Build a UriMatcher by adding a specific code to return based on a match
        // It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PodcastContract.CONTENT_AUTHORITY;

        // add a code for each type of URI you want
        matcher.addURI(authority, PodcastContract.PodcastEntry.TABLE_PODCAST, PODCAST);
        matcher.addURI(authority, PodcastContract.PodcastEntry.TABLE_PODCAST + "/#", PODCAST_WITH_ID);
        matcher.addURI(authority, PodcastContract.EpisodeEntry.TABLE_EPISODE, EPISODE);
        matcher.addURI(authority, PodcastContract.EpisodeEntry.TABLE_EPISODE + "/#", EPISODE_WITH_ID);

        return matcher;
    }

    @Override
    public boolean onCreate(){
        mOpenHelper = new PodcastDBHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri){
        final int match = sUriMatcher.match(uri);

        switch (match){
            case PODCAST:{
                return PodcastContract.PodcastEntry.CONTENT_DIR_TYPE;
            }
            case PODCAST_WITH_ID:{
                return PodcastContract.PodcastEntry.CONTENT_ITEM_TYPE;
            }
            case EPISODE:{
                return PodcastContract.EpisodeEntry.CONTENT_DIR_TYPE;
            }
            case EPISODE_WITH_ID:{
                return PodcastContract.EpisodeEntry.CONTENT_ITEM_TYPE;
            }
            default:{
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        Cursor retCursor;
        switch(sUriMatcher.match(uri)){
            // All Flavors selected
            case PODCAST:{
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.PodcastEntry.TABLE_PODCAST,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                return retCursor;
            }
            // Individual flavor based on Id selected
            case PODCAST_WITH_ID:{
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.PodcastEntry.TABLE_PODCAST,
                        projection,
                        PodcastContract.PodcastEntry._ID + " = ?",
                        new String[] {String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder);
                return retCursor;
            }
            case EPISODE:{
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.EpisodeEntry.TABLE_EPISODE,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                return retCursor;
            }
            // Individual flavor based on Id selected
            case EPISODE_WITH_ID:{
                retCursor = mOpenHelper.getReadableDatabase().query(
                        PodcastContract.EpisodeEntry.TABLE_EPISODE,
                        projection,
                        PodcastContract.EpisodeEntry._ID + " = ?",
                        new String[] {String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder);
                return retCursor;
            }
            default:{
                // By default, we assume a bad URI
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values){
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;
        switch (sUriMatcher.match(uri)) {
            case PODCAST: {
                long _id = db.insert(PodcastContract.PodcastEntry.TABLE_PODCAST, null, values);
                // insert unless it is already contained in the database
                if (_id > 0) {
                    returnUri = PodcastContract.PodcastEntry.buildPodcastUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            case EPISODE: {
                long _id = db.insert(PodcastContract.EpisodeEntry.TABLE_EPISODE, null, values);
                // insert unless it is already contained in the database
                if (_id > 0) {
                    returnUri = PodcastContract.EpisodeEntry.buildEpisodeUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }

            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);

            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs){
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int numDeleted;
        switch(match){
            case PODCAST:
                numDeleted = db.delete(
                        PodcastContract.PodcastEntry.TABLE_PODCAST, selection, selectionArgs);
                // reset _ID
                db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                        PodcastContract.PodcastEntry.TABLE_PODCAST + "'");
                break;
            case PODCAST_WITH_ID:
                numDeleted = db.delete(PodcastContract.PodcastEntry.TABLE_PODCAST,
                        PodcastContract.PodcastEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                // reset _ID
                db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                        PodcastContract.PodcastEntry.TABLE_PODCAST + "'");

                break;
            case EPISODE:
                numDeleted = db.delete(
                        PodcastContract.EpisodeEntry.TABLE_EPISODE, selection, selectionArgs);
                // reset _ID
                db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                        PodcastContract.EpisodeEntry.TABLE_EPISODE + "'");
                break;
            case EPISODE_WITH_ID:
                numDeleted = db.delete(PodcastContract.EpisodeEntry.TABLE_EPISODE,
                        PodcastContract.EpisodeEntry._ID + " = ?",
                        new String[]{String.valueOf(ContentUris.parseId(uri))});
                // reset _ID
                db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" +
                        PodcastContract.EpisodeEntry.TABLE_EPISODE + "'");

                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        return numDeleted;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values){
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch(match){
            case PODCAST:
                // allows for multiple transactions
                db.beginTransaction();

                // keep track of successful inserts
                int numInserted = 0;
                try{
                    for(ContentValues value : values){
                        if (value == null){
                            throw new IllegalArgumentException("Cannot have null content values");
                        }
                        long _id = -1;
                        try{
                            _id = db.insertOrThrow(PodcastContract.PodcastEntry.TABLE_PODCAST,
                                    null, value);
                        }catch(SQLiteConstraintException e) {
                            Log.w(LOG_TAG, "Attempting to insert " +
                                    value.getAsString(
                                            PodcastContract.PodcastEntry.COLUMN_TITLE)
                                    + " but value is already in database.");
                        }
                        if (_id != -1){
                            numInserted++;
                        }
                    }
                    if(numInserted > 0){
                        // If no errors, declare a successful transaction.
                        // database will not populate if this is not called
                        db.setTransactionSuccessful();
                    }
                } finally {
                    // all transactions occur at once
                    db.endTransaction();
                }
                if (numInserted > 0){
                    // if there was successful insertion, notify the content resolver that there
                    // was a change
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return numInserted;
            case EPISODE:
                // allows for multiple transactions
                db.beginTransaction();

                // keep track of successful inserts
                int episodeNumInserted = 0;
                try{
                    for(ContentValues value : values){
                        if (value == null){
                            throw new IllegalArgumentException("Cannot have null content values");
                        }
                        long _id = -1;
                        try{
                            _id = db.insertOrThrow(PodcastContract.EpisodeEntry.TABLE_EPISODE,
                                    null, value);
                        }catch(SQLiteConstraintException e) {
                            Log.w(LOG_TAG, "Attempting to insert " +
                                    value.getAsString(
                                            PodcastContract.EpisodeEntry.COLUMN_TITLE)
                                    + " but value is already in database.");
                        }
                        if (_id != -1){
                            episodeNumInserted++;
                        }
                    }
                    if(episodeNumInserted > 0){
                        // If no errors, declare a successful transaction.
                        // database will not populate if this is not called
                        db.setTransactionSuccessful();
                    }
                } finally {
                    // all transactions occur at once
                    db.endTransaction();
                }
                if (episodeNumInserted > 0){
                    // if there was successful insertion, notify the content resolver that there
                    // was a change
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return episodeNumInserted;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs){
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int numUpdated = 0;

        if (contentValues == null){
            throw new IllegalArgumentException("Cannot have null content values");
        }

        switch(sUriMatcher.match(uri)){
            case PODCAST:{
                numUpdated = db.update(PodcastContract.PodcastEntry.TABLE_PODCAST,
                        contentValues,
                        selection,
                        selectionArgs);
                break;
            }
            case PODCAST_WITH_ID: {
                numUpdated = db.update(PodcastContract.PodcastEntry.TABLE_PODCAST,
                        contentValues,
                        PodcastContract.PodcastEntry._ID + " = ?",
                        new String[] {String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            case EPISODE:{
                numUpdated = db.update(PodcastContract.EpisodeEntry.TABLE_EPISODE,
                        contentValues,
                        selection,
                        selectionArgs);
                break;
            }
            case EPISODE_WITH_ID: {
                numUpdated = db.update(PodcastContract.EpisodeEntry.TABLE_EPISODE,
                        contentValues,
                        PodcastContract.EpisodeEntry._ID + " = ?",
                        new String[] {String.valueOf(ContentUris.parseId(uri))});
                break;
            }
            default:{
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (numUpdated > 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numUpdated;
    }

}
