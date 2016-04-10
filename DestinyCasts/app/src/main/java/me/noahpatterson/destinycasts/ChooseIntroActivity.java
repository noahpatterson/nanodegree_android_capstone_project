package me.noahpatterson.destinycasts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.List;

import me.noahpatterson.destinycasts.model.Podcast;

public class ChooseIntroActivity extends AppCompatActivity {

    private GridView podcastGridView;
    private PodcastChooseArrayAdapter podcastAdapter;

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_intro);

        //grab list of saved podcast favorites
        SharedPreferences preferences =
                getSharedPreferences("my_preferences", MODE_PRIVATE);
        Gson gson = new Gson();
        String jsonText = preferences.getString("podcast favorites", null);
        List<Podcast> podcastList = gson.fromJson(jsonText, new TypeToken<List<Podcast>>(){}.getType());

        podcastGridView = (GridView) findViewById(R.id.podcastGridView);

        //if saved favorites, load them, else create a new list
        List<Podcast> favoritePodcasts;
        if (podcastList != null) {
            favoritePodcasts = podcastList;
        } else {
            favoritePodcasts = Arrays.asList(Utilities.podcastsList);
        }
        podcastAdapter = new PodcastChooseArrayAdapter(this, favoritePodcasts);
        podcastGridView.setAdapter(podcastAdapter);


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
        for(int i = 0;i  < count;i++) {
            podcastAdapter.getItem(i).isSelected = true;
        }
        podcastAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        storeSelectedPodcasts();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void storeSelectedPodcasts() {
        List podcastItems = podcastAdapter.getAllItems();
        Gson gson = new Gson();
        String jsonText = gson.toJson(podcastItems);
        SharedPreferences preferences =
                getSharedPreferences("my_preferences", MODE_PRIVATE);
        preferences.edit()
                .putString("podcast favorites",jsonText).apply();
    }
}
