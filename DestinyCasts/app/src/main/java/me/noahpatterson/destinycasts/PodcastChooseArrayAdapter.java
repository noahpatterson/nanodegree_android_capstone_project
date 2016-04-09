package me.noahpatterson.destinycasts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by noahpatterson on 4/9/16.
 */
public class PodcastChooseArrayAdapter extends ArrayAdapter<Podcast> {
    public PodcastChooseArrayAdapter(Context context, List<Podcast> podcastList) {
        super(context, 0, podcastList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Podcast podcast = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.podcast_list_view, parent, false);
        }

//        ImageView iconView = (ImageView) convertView.findViewById(R.id.list_item_icon);
//        iconView.setImageResource(androidFlavor.image);

//        TextView versionNameView = (TextView) convertView.findViewById(R.id.list_item_version_name);
//        versionNameView.setText(androidFlavor.versionName);

        TextView podcastTitleView = (TextView) convertView.findViewById(R.id.podcastTitleTextView);
        podcastTitleView.setText(podcast.name);
        return convertView;
    }
}
