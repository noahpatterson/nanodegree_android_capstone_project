package me.noahpatterson.destinycasts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

public class ChooseIntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_intro);

        GridView podcastGridView = (GridView) findViewById(R.id.podcastGridView);
        podcastGridView.setAdapter(new PodcastChooseArrayAdapter(this, Arrays.asList(podcastsList)));
    }

    public void finishOnboarding(View view) {
        // Get the shared preferences
        SharedPreferences preferences =
                getSharedPreferences("my_preferences", MODE_PRIVATE);

        // Set onboarding_complete to true
        preferences.edit()
                .putBoolean("onboarding_complete",true).apply();

        // Launch the main Activity, called MainActivity
        Intent main = new Intent(this, WeeklyListActivity.class);
        startActivity(main);

        // Close the OnboardingActivity
        finish();
    }

    private Podcast[] podcastsList = {
            new Podcast("Destiny The Show", R.drawable.destiny_the_show, "http://destinytheshow.podbean.com/feed/"),
            new Podcast("Destiny Ghost Stories", R.drawable.destiny_ghost_stories, "http://destinyghoststories.podbean.com/feed/"),
            new Podcast("Fireteam Chat", R.drawable.fireteam_chat, "http://feeds.feedburner.com/FireteamChatIgnsDestinyPodcast"),
            new Podcast("Guardian Radio", R.drawable.guardian_radio, "http://theguardiansofdestiny.com/feed/podcast/"),
            new Podcast("Crucible Radio", R.drawable.crucible_radio, "http://feeds.feedburner.com/CrucibleRadio")
    };
}
