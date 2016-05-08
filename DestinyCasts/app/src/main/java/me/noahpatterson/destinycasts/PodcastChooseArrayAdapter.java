package me.noahpatterson.destinycasts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import me.noahpatterson.destinycasts.model.Podcast;

/**
 * Created by noahpatterson on 4/9/16.
 */
public class PodcastChooseArrayAdapter extends ArrayAdapter<Podcast> {
    private Context mContext;
    List<Podcast> podcastList;
    public PodcastChooseArrayAdapter(Context context, List<Podcast> podcastList) {
        super(context, 0, podcastList);
        mContext = context;
        this.podcastList = podcastList;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final Podcast podcast = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.podcast_list_view, parent, false);
        }

        ImageView podcastImageView = (ImageView) convertView.findViewById(R.id.podcastImageView);
        podcastImageView.setContentDescription(podcast.name + mContext.getString(R.string.content_desc_logo));
        Glide.with(mContext).load(podcast.photo).into(podcastImageView);

        Utilities.setImageSelected(convertView, podcast);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utilities.togglePodcastSelected(v, podcast);
                ((AdapterView) parent).performItemClick(v, position, 0);
            }
        });

        TextView podcastTitleView = (TextView) convertView.findViewById(R.id.podcastTitleTextView);
        podcastTitleView.setText(podcast.name);


        return convertView;
    }
}
