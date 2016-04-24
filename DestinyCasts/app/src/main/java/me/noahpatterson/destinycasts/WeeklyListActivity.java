package me.noahpatterson.destinycasts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.stetho.Stetho;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.noahpatterson.destinycasts.data.PodcastContract;
import me.noahpatterson.destinycasts.data.PodcastDBHelper;
import me.noahpatterson.destinycasts.model.Podcast;

public class WeeklyListActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private EpisodeAdapter mEpisodeAdapters;
    private static List<Podcast> podcastList;
    private static List<Podcast> favPodcastList;

    private static long ONE_DAY_IN_MILLI = 86400000;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // Get the shared preferences
        SharedPreferences preferences =  getSharedPreferences("my_preferences", MODE_PRIVATE);

        // Check if onboarding_complete is false
        if(!preferences.getBoolean("onboarding_complete",false)) {
            startOnboarding();

            return;
        }

        //get all podcast list
        podcastList = Arrays.asList(Utilities.podcastsList);

        //get podcast favorites
        favPodcastList = Utilities.getPodcastsFavorites(this, preferences);

        setContentView(R.layout.activity_weekly_list);
        // Stetho is a tool created by facebook to view your database in chrome inspect.
        // The code below integrates Stetho into your app. More information here:
        // http://facebook.github.io/stetho/
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(
                                Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(
                                Stetho.defaultInspectorModulesProvider(this))
                        .build());

        ///////////////

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        FetchPodcastFeedsIntentService.startActionFetchNew(this,podcastList);
    }

    private void startOnboarding() {
        // Start the onboarding Activity
        Intent onboarding = new Intent(this, ChooseIntroActivity.class);
        startActivity(onboarding);

        // Close the main Activity
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weekly_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startOnboarding();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class EpisodeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_WEEK_NUMBER = "week_number";
        private EpisodeAdapter mEpisodeAdapters;
        private RecyclerView mRecyclerView;

        public EpisodeFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static EpisodeFragment newInstance(int weekNumber) {
            EpisodeFragment fragment = new EpisodeFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_WEEK_NUMBER, weekNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            int cursorLoaderId = this.getArguments().getInt(ARG_WEEK_NUMBER);
            getLoaderManager().initLoader(cursorLoaderId,null, this);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.d("weeklyList", "in onCreateView");
            int cursorLoaderId = this.getArguments().getInt(ARG_WEEK_NUMBER);
            mEpisodeAdapters = new EpisodeAdapter(getActivity(), null, 0, cursorLoaderId);

            View rootView = inflater.inflate(R.layout.fragment_weekly_list, container, false);
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_episodes);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(mEpisodeAdapters);
            return rootView;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d("weeklyList", "in onCreateLoader");
            long todaysDateInMilli = System.currentTimeMillis();
            int weekPageNumber = this.getArguments().getInt(ARG_WEEK_NUMBER);
            StringBuilder sb = new StringBuilder();
            if (favPodcastList != null) {
                for (int i = 0; i < favPodcastList.size(); i++) {
                    Podcast podcast = favPodcastList.get(i);
                    if (podcast.isSelected) {
                        sb.append("\"" + podcast.name + "\",");
                    }
                }
                sb.deleteCharAt(sb.length() - 1);
            }

            //get the database
            String favoritePodcastSelection = " AND " + PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE + " IN ("+ sb.toString() + ")";
//            Log.d("weeklyList", sb.toString());
//            Log.d("weeklyList", favoritePodcastSelection);
            switch (weekPageNumber) {
                case 0:
//                    return "This Week";
                    Log.d("weeklyList", "in this week");
                    return new CursorLoader(getActivity(),
                            PodcastContract.EpisodeEntry.CONTENT_URI,
                            new String[] {
                                    PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL,
                                    PodcastContract.EpisodeEntry.COLUMN_TITLE,
                                    PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION,
                                    PodcastContract.EpisodeEntry.COLUMN_PUB_DATE
                            },
                            PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + ">=?" + favoritePodcastSelection,
                            new String[] { Long.toString(todaysDateInMilli - (ONE_DAY_IN_MILLI * 7))},
                            null);
                case 1:
//                    return "Last Week";
                    Log.d("weeklyList", "in last week");
                    return new CursorLoader(getActivity(),
                            PodcastContract.EpisodeEntry.CONTENT_URI,
                            new String[] {
                                    PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL,
                                    PodcastContract.EpisodeEntry.COLUMN_TITLE,
                                    PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION,
                                    PodcastContract.EpisodeEntry.COLUMN_PUB_DATE
                            },
                            PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + "<? AND " + PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + ">=?" + favoritePodcastSelection,
                            new String[] { Long.toString(todaysDateInMilli - (ONE_DAY_IN_MILLI * 7)),Long.toString(todaysDateInMilli - (ONE_DAY_IN_MILLI * 14))},
                            null);
            }
            return null;

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.d("weeklyList", "in onLoadFinished");
            mEpisodeAdapters.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.d("weeklyList", "in onLoaderReset");
            mEpisodeAdapters.swapCursor(null);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return EpisodeFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "This Week";
                case 1:
                    return "Last Week";
            }
            return null;
        }
    }
}
