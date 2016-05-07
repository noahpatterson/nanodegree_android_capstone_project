package me.noahpatterson.destinycasts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

import me.noahpatterson.destinycasts.model.Podcast;

public class ChooseIntroActivity extends AppCompatActivity {

    private GridView podcastGridView;
    private PodcastChooseArrayAdapter podcastAdapter;
    private SharedPreferences preferences;
    private List<Podcast> favoritePodcasts;
    private Button continueButton;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_intro);

        continueButton = (Button) findViewById(R.id.continueButton);

        // Get the shared preferences
        preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);

        List<Podcast> podcastList = Utilities.getPodcastsFavorites(this, preferences);

        podcastGridView = (GridView) findViewById(R.id.podcastGridView);
        podcastGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int count = podcastGridView.getChildCount();
                    boolean aPodcastIsSelected = false;
                    for(int i = 0;i  < count;i++) {
                        if (podcastAdapter.getItem(i).isSelected) {
                            aPodcastIsSelected = true;
                            return;
                        }
                    }
                    if (aPodcastIsSelected) {
                        enableContinueButton();
                    } else {
                        disableContinueButton();
                    }
                }
        });

        //if saved favorites, load them, else create a new list
//        List<Podcast> favoritePodcasts;
        if (podcastList != null) {
            favoritePodcasts = podcastList;
        } else {
            favoritePodcasts = Arrays.asList(Utilities.podcastsList);
        }
        podcastAdapter = new PodcastChooseArrayAdapter(this, favoritePodcasts);
        podcastGridView.setAdapter(podcastAdapter);

        //set analytics tracker
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }


    public void finishOnboarding(View view) {
        // Set onboarding_complete to true
        preferences.edit()
                .putBoolean("onboarding_complete",true).apply();
        //changed settings, now refresh podcasts by changing the last refreshed date
        preferences.edit()
                .putLong("refreshed_date", Utilities.getStartOfDayInMillis()-1).apply();

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
        enableContinueButton();
    }

    @Override
    protected void onDestroy() {
        storeSelectedPodcasts();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        storeSelectedPodcasts();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onPause() {
        storeSelectedPodcasts();
        super.onPause();
    }

    private void storeSelectedPodcasts() {
//        List podcastItems = podcastAdapter.getAllItems();
        Gson gson = new Gson();
        String jsonText = gson.toJson(favoritePodcasts);
        preferences.edit()
                .putString("podcast favorites",jsonText).apply();
    }

    private void enableContinueButton() {
        continueButton.setAlpha(1f);
        continueButton.setClickable(true);
    }

    private void disableContinueButton() {
        continueButton.setAlpha(0.5f);
        continueButton.setClickable(false);
    }

}
