package me.noahpatterson.destinycasts;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import me.noahpatterson.destinycasts.data.PodcastContract;

/**
 * Created by noahpatterson on 4/12/16.
 */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private static final String LOG_TAG = EpisodeAdapter.class.getSimpleName();
    private Context mContext;
    private static int sLoaderID;
    private Cursor mCursor;

    public EpisodeAdapter(Context context, Cursor c, int flags, int loaderID, EpisodeAdapterOnClickHandler episodeAdapterOnClickHandler){
        Log.d(LOG_TAG, "EpisodeAdapter");
        mContext = context;
        sLoaderID = loaderID;
        mCursor = c;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView episodeImageView;
        public final TextView episodeTitleTextView;
        public final TextView episodeDateTextView;
        public final TextView episodeDescriptionTextView;

        public ViewHolder(View view){
            super(view);
            episodeImageView = (ImageView) view.findViewById(R.id.episodeImageView);
            episodeTitleTextView = (TextView) view.findViewById(R.id.episodeTitleTextView);
            episodeDateTextView = (TextView) view.findViewById(R.id.episodeDateTextView);
            episodeDescriptionTextView = (TextView) view.findViewById(R.id.episodeDescriptionTextView);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);

        }
    }

    public static interface EpisodeAdapterOnClickHandler {
        void onClick(int podcastId, ViewHolder vh);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = R.layout.episode_list_view;

        Log.d(LOG_TAG, "In new View");

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Log.d(LOG_TAG, "In bind View");
        mCursor.moveToPosition(position);

        //podcast title
        int podcastTitleIndex = mCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE);
        final String podcastTitle = mCursor.getString(podcastTitleIndex);

        //title
        int episodeIndex = mCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_TITLE);
        final String episodeName = mCursor.getString(episodeIndex);
        viewHolder.episodeTitleTextView.setText(episodeName);

        //image
        int imageIndex = mCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL);
        String imageUrl = mCursor.getString(imageIndex);

        Log.i(LOG_TAG, "Image reference extracted: " + imageUrl);
        Glide.with(mContext).load(imageUrl.equals("") ? Utilities.findPodcastImage(podcastTitle) : imageUrl).into(viewHolder.episodeImageView);

        //date
        int episodeDateIndex = mCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_PUB_DATE);
        final long episodeDate = mCursor.getLong(episodeDateIndex);
        SimpleDateFormat formatter = new SimpleDateFormat("MMM d", Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(episodeDate);
        viewHolder.episodeDateTextView.setText(formatter.format(calendar.getTime()));

        //description
        int descriptionIndex = mCursor.getColumnIndex(PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION);
        final String description = mCursor.getString(descriptionIndex);
        viewHolder.episodeDescriptionTextView.setText(Html.fromHtml(description).toString());
    }

    @Override
    public int getItemCount() {
        if ( null == mCursor) return 0;
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }
}
