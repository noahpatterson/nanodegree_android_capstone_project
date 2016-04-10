package me.noahpatterson.destinycasts.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by noahpatterson on 4/10/16.
 */
public class PodcastContract {

    public static final String CONTENT_AUTHORITY = "me.noahpatterson.destinycasts.app";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final class PodcastEntry implements BaseColumns {
        // table name
        public static final String TABLE_PODCAST = "podcast";
        // columns
        public static final String _ID = "_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PODCAST_URL = "podcastUrl";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_SUBTITLE = "subtitle";

        // create content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_PODCAST).build();
        // create cursor of base type directory for multiple entries
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + TABLE_PODCAST;
        // create cursor of base type item for single entry
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +"/" + CONTENT_AUTHORITY + "/" + TABLE_PODCAST;

        // for building URIs on insertion
        public static Uri buildPodcastUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class EpisodeEntry implements BaseColumns {
        // table name
        public static final String TABLE_EPISODE = "episode";
        // columns
        public static final String _ID = "_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PUB_DATE = "publish_date";
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_URL = "mp3_url";
        public static final String COLUMN_PODCAST_ID = "podcast_id";

        // create content uri
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_EPISODE).build();
        // create cursor of base type directory for multiple entries
        public static final String CONTENT_DIR_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + TABLE_EPISODE;
        // create cursor of base type item for single entry
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE +"/" + CONTENT_AUTHORITY + "/" + TABLE_EPISODE;

        // for building URIs on insertion
        public static Uri buildEpisodeUri(long id){
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
