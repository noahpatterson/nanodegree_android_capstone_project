package me.noahpatterson.destinycasts;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.einmalfel.earl.EarlParser;
import com.einmalfel.earl.Feed;
import com.einmalfel.earl.Item;
import com.einmalfel.earl.RSSFeed;
import com.einmalfel.earl.RSSItem;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import me.noahpatterson.destinycasts.data.PodcastContract;
import me.noahpatterson.destinycasts.model.Podcast;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class FetchPodcastFeedsIntentService extends IntentService {
    private static final String ACTION_FETCH_NEW_FEED_ITEMS = "me.noahpatterson.destinycasts.action.FETCH_NEW";

    private static final String FEED_URL_PARAM = "me.noahpatterson.destinycasts.extra.FEED_URL_PARAM";
    private static final String LOG_TAG = "FeedsIntentService";
    private static Context mContext;

    public FetchPodcastFeedsIntentService() {
        super("FetchPodcastFeedsIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchNew(Context context, List<Podcast> podcastList) {
        mContext = context;
        //pull out only podcast favorite urls
        ArrayList<String> favorites = new ArrayList<>();
        int favoriteLength = 0;
        for(int i =0;i < podcastList.size();i++) {
            Podcast podcast = podcastList.get(i);
            if (podcast.isSelected) {
                favorites.add(favoriteLength, podcast.rssFeedUrlString);
                favoriteLength++;
            }
        }

        Intent intent = new Intent(context, FetchPodcastFeedsIntentService.class);
        intent.setAction(ACTION_FETCH_NEW_FEED_ITEMS);
//        intent.putExtra(FEED_URL_PARAM, favorites.get(0));
        intent.putStringArrayListExtra(FEED_URL_PARAM, favorites);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_NEW_FEED_ITEMS.equals(action)) {
                final ArrayList<String> feedUrls = intent.getStringArrayListExtra(FEED_URL_PARAM);
                handleActionFetchNew(feedUrls);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchNew(ArrayList<String> feedUrls) {
        OkHttpClient client = new OkHttpClient();

        try {

            //loop through favorite podcast urls
            for(String feedUrl : feedUrls) {
                Request request = new Request.Builder()
                        .url(feedUrl)
                        .build();

                Response response = client.newCall(request).execute();
                Feed feed = EarlParser.parseOrThrow(response.body().byteStream(), 0);

                insertData(feed);
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "xmlPullParserException ", e);
        } catch (DataFormatException e) {
            Log.e(LOG_TAG, "DataFormatException ", e);
        }


    }

    // insert data into database
    private void insertData(Feed feed){
//        ContentValues[] podcastValuesArr = new ContentValues[feedUrls.size()];
        List episodeList = feed.getItems();
        ContentValues[] episodeValuesArr = new ContentValues[10];

//        ContentValues podcastData = new ContentValues();

        long podcastId = addPodcast(feed.getTitle(),
                                   ((RSSFeed) feed).itunes.subtitle,
                                   ((RSSFeed) feed).itunes.subtitle,
                                   feed.getLink(),
                                   ((RSSFeed) feed).itunes.summary);

        //populate content values of episodes -- first 10
        for(int i = 0; i < 10; i++){
            episodeValuesArr[i] = new ContentValues();
            Item episode = ((Item) episodeList.get(i));
            RSSItem episodeRssItem = ((RSSItem) episode);
            String title = episode.getTitle();
            episodeValuesArr[i].put(PodcastContract.EpisodeEntry.COLUMN_TITLE, (title == null ? "N/A" : title));
            episodeValuesArr[i].put(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION, episode.getDescription());
            episodeValuesArr[i].put(PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL, episodeRssItem.itunes.image.toString());
            episodeValuesArr[i].put(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE, episode.getPublicationDate().getTime());
            episodeValuesArr[i].put(PodcastContract.EpisodeEntry.COLUMN_URL, episodeRssItem.media.contents.get(0).url.toString());
            episodeValuesArr[i].put(PodcastContract.EpisodeEntry.COLUMN_PODCAST_ID, podcastId);
        }

        // bulkInsert our ContentValues array
        mContext.getContentResolver().bulkInsert(PodcastContract.EpisodeEntry.CONTENT_URI,
                episodeValuesArr);
    }

    long addPodcast(String title, String subtitle, String summary, String podcastUrl, String description) {
        long podcastId;

        // First, check if the podcast with this name exists in the db
        Cursor podcastCursor = mContext.getContentResolver().query(
                PodcastContract.PodcastEntry.CONTENT_URI,
                new String[]{PodcastContract.PodcastEntry._ID},
                PodcastContract.PodcastEntry.COLUMN_TITLE + " = ?",
                new String[]{title},
                null);

        if (podcastCursor.moveToFirst()) {
            int podcastIdIndex = podcastCursor.getColumnIndex(PodcastContract.PodcastEntry._ID);
            podcastId = podcastCursor.getLong(podcastIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues podcastValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            podcastValues.put(PodcastContract.PodcastEntry.COLUMN_TITLE, title);
            podcastValues.put(PodcastContract.PodcastEntry.COLUMN_SUBTITLE, subtitle);
            podcastValues.put(PodcastContract.PodcastEntry.COLUMN_DESCRIPTION, description);
            podcastValues.put(PodcastContract.PodcastEntry.COLUMN_PODCAST_URL, podcastUrl);

            // Finally, insert location data into the database.
            Uri insertedUri = mContext.getContentResolver().insert(
                    PodcastContract.PodcastEntry.CONTENT_URI,
                    podcastValues
            );

            // The resulting URI contains the ID for the row.  Extract the podcastId from the Uri.
            podcastId = ContentUris.parseId(insertedUri);
        }

        podcastCursor.close();
        // Wait, that worked?  Yes!
        return podcastId;
    }
}
