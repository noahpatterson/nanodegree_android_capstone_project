package me.noahpatterson.destinycasts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by noahpatterson on 4/9/16.
 */
public class PodcastChooseArrayAdapter extends ArrayAdapter<Podcast> {
    private Context mContext;
    public PodcastChooseArrayAdapter(Context context, List<Podcast> podcastList) {
        super(context, 0, podcastList);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Podcast podcast = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.podcast_list_view, parent, false);
        }

        ImageView podcastImageView = (ImageView) convertView.findViewById(R.id.podcastImageView);
        Glide.with(mContext).load(podcast.photo).into(podcastImageView);

        TextView podcastTitleView = (TextView) convertView.findViewById(R.id.podcastTitleTextView);
        podcastTitleView.setText(podcast.name);


        return convertView;
    }
}
