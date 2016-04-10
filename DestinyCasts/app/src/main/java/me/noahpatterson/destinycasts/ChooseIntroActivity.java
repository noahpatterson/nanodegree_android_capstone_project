package me.noahpatterson.destinycasts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;

import java.util.Arrays;

import me.noahpatterson.destinycasts.model.Podcast;

public class ChooseIntroActivity extends AppCompatActivity {

    private GridView podcastGridView;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_intro);
//        rootView = getLayoutInflater().inflate(R.layout.activity_choose_intro,null);

        podcastGridView = (GridView) findViewById(R.id.podcastGridView);
        podcastGridView.setAdapter(new PodcastChooseArrayAdapter(this, Arrays.asList(Utilities.podcastsList)));


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

    public void selectAllPodcasts(View view) {
        int count = podcastGridView.getChildCount();
//        View podcastItemView = LayoutInflater.from(this).inflate(R.layout.podcast_list_view, podcastGridView, false);
        PodcastChooseArrayAdapter podcastAdapter = (PodcastChooseArrayAdapter) podcastGridView.getAdapter();
        for(int i = 0;i  < count;i++) {
//           View podcastItem = podcastAdapter.getView(i,podcastItemView, podcastGridView);
//            podcastItem.setSelected(true);
            podcastAdapter.getItem(i).isSelected = true;
        }
        podcastAdapter.notifyDataSetChanged();
    }
}
