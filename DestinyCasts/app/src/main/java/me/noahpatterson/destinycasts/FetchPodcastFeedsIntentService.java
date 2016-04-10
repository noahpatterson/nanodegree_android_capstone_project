package me.noahpatterson.destinycasts;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.einmalfel.earl.EarlParser;
import com.einmalfel.earl.Feed;
import com.einmalfel.earl.Item;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import me.noahpatterson.destinycasts.model.Podcast;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FetchPodcastFeedsIntentService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FETCH_NEW_FEED_ITEMS = "me.noahpatterson.destinycasts.action.FETCH_NEW";
//    private static final String ACTION_BAZ = "me.noahpatterson.destinycasts.action.BAZ";

    // TODO: Rename parameters
    private static final String FEED_URL_PARAM = "me.noahpatterson.destinycasts.extra.FEED_URL_PARAM";
//    private static final String EXTRA_PARAM2 = "me.noahpatterson.destinycasts.extra.PARAM2";
    private static final String LOG_TAG = "FeedsIntentService";

    public FetchPodcastFeedsIntentService() {
        super("FetchPodcastFeedsIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFetchNew(Context context, List<Podcast> podcastList) {
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
        intent.putExtra(FEED_URL_PARAM, favorites.get(0));
//        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
//    public static void startActionBaz(Context context, String param1, String param2) {
//        Intent intent = new Intent(context, FetchPodcastFeedsIntentService.class);
//        intent.setAction(ACTION_BAZ);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
//        context.startService(intent);
//    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_NEW_FEED_ITEMS.equals(action)) {
                final String feedUrl = intent.getStringExtra(FEED_URL_PARAM);
                handleActionFetchNew(feedUrl);
            }
//            else if (ACTION_BAZ.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionBaz(param1, param2);
//            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchNew(String feedUrl) {
        // TODO: Handle action Foo
//        throw new UnsupportedOperationException("Not yet implemented");
        OkHttpClient client = new OkHttpClient();

        try {
            Request request = new Request.Builder()
                    .url(feedUrl)
                    .build();

            Response response = client.newCall(request).execute();
//            Log.d(LOG_TAG, response.body().string());
            Feed feed = EarlParser.parseOrThrow(response.body().byteStream(), 0);
            Log.i(LOG_TAG, "Processing feed: " + feed.getTitle());
            for (Item item : feed.getItems()) {
                String title = item.getTitle();
                Log.i(LOG_TAG, "Item title: " + (title == null ? "N/A" : title));
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "xmlPullParserException ", e);
        } catch (DataFormatException e) {
            Log.e(LOG_TAG, "DataFormatException ", e);
        }


    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
//    private void handleActionBaz(String param1, String param2) {
//        // TODO: Handle action Baz
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
}
