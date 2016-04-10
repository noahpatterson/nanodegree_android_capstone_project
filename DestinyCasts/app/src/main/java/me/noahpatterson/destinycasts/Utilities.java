package me.noahpatterson.destinycasts;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import me.noahpatterson.destinycasts.model.Podcast;

/**
 * Created by noahpatterson on 4/10/16.
 */
public class Utilities {
    public static void setImageSelected(View imageView, Podcast podcast) {
        boolean selected = podcast.isSelected;
        if (selected) {
            imageView.setAlpha(0.5f);
        } else {
            imageView.setAlpha(1.0f);
        }
    }

    public static void togglePodcastSelected(View podcastView, Podcast podcast) {
        boolean selected = podcast.isSelected;
        if (!selected) {
            podcastView.setAlpha(0.5f);
            podcast.isSelected = true;
        } else {
            podcastView.setAlpha(1.0f);
            podcast.isSelected = false;
        }
    }

    public static Podcast[] podcastsList = {
            new Podcast("Destiny The Show", R.drawable.destiny_the_show, "http://destinytheshow.podbean.com/feed/"),
            new Podcast("Destiny Ghost Stories", R.drawable.destiny_ghost_stories, "http://destinyghoststories.podbean.com/feed/"),
            new Podcast("Fireteam Chat", R.drawable.fireteam_chat, "http://feeds.feedburner.com/FireteamChatIgnsDestinyPodcast"),
            new Podcast("Guardian Radio", R.drawable.guardian_radio, "http://theguardiansofdestiny.com/feed/podcast/"),
            new Podcast("Crucible Radio", R.drawable.crucible_radio, "http://feeds.feedburner.com/CrucibleRadio")
    };

    @Nullable
    public static List<Podcast> getPodcastsFavorites(Context context, SharedPreferences preferences) {
        //grab list of saved podcast favorites
        Gson gson = new Gson();
        String jsonText = preferences.getString("podcast favorites", null);
        return gson.fromJson(jsonText, new TypeToken<List<Podcast>>(){}.getType());
    }
}
