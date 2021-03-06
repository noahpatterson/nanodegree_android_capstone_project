package me.noahpatterson.destinycasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Arrays;
import java.util.List;

import me.noahpatterson.destinycasts.data.PodcastContract;
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
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private static List<Podcast> podcastList;
    private static List<Podcast> favPodcastList;
    private static Tracker mTracker;
    private static final String LOG_TAG = "weeklyList";

    private static long ONE_DAY_IN_MILLI = 86400000;
    private static long todaysDateInMilli;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private boolean mIsRefreshing = false;
    private EpisodeFragment thisWeekFragment;
    private EpisodeFragment lastWeekFragment;
    private static boolean mTwoPane = false;
    private static int lastItemSelected;
    private static int selectedWeek;

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(FetchPodcastFeedsIntentService.BROADCAST_ACTION_STATE_CHANGE));
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "in onDestory");
        if (mViewPager != null) {
            SharedPreferences preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
            preferences.edit().putInt("selectedWeek", mViewPager.getCurrentItem()).apply();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "in on saveInstanceState");
        outState.putInt("episode_id", lastItemSelected);
        outState.putInt("selected_week", mViewPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "in onCreate");
        super.onCreate(savedInstanceState);

        //call the Rss lookup as fast as possible
        //get all podcast list
        podcastList = Arrays.asList(Utilities.podcastsList);

        // Get the shared preferences
        SharedPreferences preferences =  getSharedPreferences("my_preferences", MODE_PRIVATE);

        //set analytics tracker
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // Check if onboarding_complete is false
        if(!preferences.getBoolean("onboarding_complete",false)) {
            startOnboarding();
            return;
        }

        //only refresh once per day
        todaysDateInMilli = Utilities.getStartOfDayInMillis();
        if (preferences.getLong("refreshed_date", 0) < todaysDateInMilli){
            refreshPodcasts();
            preferences.edit()
                    .putLong("refreshed_date", todaysDateInMilli).apply();
        }

        //get podcast favorites
        favPodcastList = Utilities.getPodcastsFavorites(this, preferences);

        setContentView(R.layout.activity_weekly_list);

        //is this master detail layout?
        if (findViewById(R.id.player_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w600dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        } else {
            mTwoPane = false;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        selectedWeek = -1;
        if (savedInstanceState != null) {
            selectedWeek = savedInstanceState.getInt("selected_week");
            mViewPager.setCurrentItem(selectedWeek);
        }
        if (selectedWeek == -1) {
            mViewPager.setCurrentItem(preferences.getInt("selectedWeek", 0));
        }

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d(LOG_TAG, "in onPageChanged");
                String name;
                switch (position) {
                    case 0:
                        name = getString(R.string.this_week);
                        break;
                    case 1:
                        name = getString(R.string.last_week);
                        break;
                    default:
                        name = getString(R.string.analytics_no_page);
                }
                Log.d(LOG_TAG, "Setting screen name: " + name);
                mTracker.setScreenName(name + getString(R.string.analytics_screen_name_suffix));
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPodcasts();
            }
        });

        if (mTwoPane && savedInstanceState != null) {
            lastItemSelected = savedInstanceState.getInt("episode_id");
            Bundle args = new Bundle();
            args.putInt("podcast_id", lastItemSelected);
            EpisodeDetailFragment episodeDetailFragment = new EpisodeDetailFragment();
            episodeDetailFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_detail_container, episodeDetailFragment)
                    .commit();
        }
    }

    //handle refresh action
    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (FetchPodcastFeedsIntentService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(FetchPodcastFeedsIntentService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
                if (!mIsRefreshing && thisWeekFragment != null && lastWeekFragment != null) {
                    Log.d(LOG_TAG, "data has refreshed - in !mIsRefreshing");
                    thisWeekFragment.notifyDataIsRefreshed();
                    lastWeekFragment.notifyDataIsRefreshed();
                    mViewPager.invalidate();
                }
            }
        }
    };

    private void startOnboarding() {
        // Start the onboarding Activity
        Intent onboarding = new Intent(this, ChooseIntroActivity.class);
        startActivity(onboarding);
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Starting on boarding")
                .build());

        // Close the main Activity
        finish();
    }

    private void refreshPodcasts(){
        FetchPodcastFeedsIntentService.startActionFetchNew(this,podcastList);
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

    public static class EpisodeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_WEEK_NUMBER = "week_number";
        private EpisodeAdapter mEpisodeAdapters;
        private RecyclerView mRecyclerView;
        private static final String LOG_TAG = "EpisodeFragment";
        private int cursorLoaderId;
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

        public void notifyDataIsRefreshed() {
            Log.d(LOG_TAG, "in notifyDataIsRefreshed");
            getLoaderManager().restartLoader(cursorLoaderId,null,this);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            cursorLoaderId = this.getArguments().getInt(ARG_WEEK_NUMBER);
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
            Log.d(LOG_TAG, "in onCreateView");


            mEpisodeAdapters = new EpisodeAdapter(getActivity(), null, 0, cursorLoaderId, new EpisodeAdapter.EpisodeAdapterOnClickHandler() {
                @Override
                public void onClick(int podcastId, int position, EpisodeAdapter.ViewHolder vh) {
                    lastItemSelected = podcastId;
                    mEpisodeAdapters.notifyItemChanged(position);
                    if (mTwoPane) {
                        Bundle args = new Bundle();
                        args.putInt("podcast_id", podcastId);
                        EpisodeDetailFragment episodeDetailFragment = new EpisodeDetailFragment();
                        episodeDetailFragment.setArguments(args);
                        getFragmentManager().beginTransaction()
                                .replace(R.id.player_detail_container, episodeDetailFragment)
                                .commit();
                    } else {
                        Intent intent = new Intent(getActivity(), EpisodeActivity.class);
                        intent.putExtra("podcast_id", podcastId);
                        startActivity(intent);
                    }
                }
            });

            View rootView = inflater.inflate(R.layout.fragment_weekly_list, container, false);
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_episodes);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(mEpisodeAdapters);

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(LOG_TAG, "in onResume");
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(LOG_TAG, "in onCreateLoader");
            int weekPageNumber = cursorLoaderId;
            StringBuilder sb = new StringBuilder();
            if (favPodcastList != null) {
                for (int i = 0; i < favPodcastList.size(); i++) {
                    Podcast podcast = favPodcastList.get(i);
                    if (podcast.isSelected) {
                        sb.append("\"").append(podcast.name).append("\",");
                    }
                }
                sb.deleteCharAt(sb.length() - 1);
            }

            //get the database
            String favoritePodcastSelection = " AND " + PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE + " IN ("+ sb.toString() + ")";
            switch (weekPageNumber) {
                case 0:
//                    return "This Week";
                    Log.d(LOG_TAG, "in this week");
                    return new CursorLoader(getActivity(),
                            PodcastContract.EpisodeEntry.CONTENT_URI,
                            new String[] {
                                    PodcastContract.EpisodeEntry._ID,
                                    PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL,
                                    PodcastContract.EpisodeEntry.COLUMN_TITLE,
                                    PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION,
                                    PodcastContract.EpisodeEntry.COLUMN_PUB_DATE,
                                    PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE
                            },
                            PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + ">=?" + favoritePodcastSelection,
                            new String[] { Long.toString(todaysDateInMilli - (ONE_DAY_IN_MILLI * 7))},
                            null);
                case 1:
//                    return "Last Week";
                    Log.d(LOG_TAG, "in last week");
                    return new CursorLoader(getActivity(),
                            PodcastContract.EpisodeEntry.CONTENT_URI,
                            new String[] {
                                    PodcastContract.EpisodeEntry._ID,
                                    PodcastContract.EpisodeEntry.COLUMN_IMAGE_URL,
                                    PodcastContract.EpisodeEntry.COLUMN_TITLE,
                                    PodcastContract.EpisodeEntry.COLUMN_DESCRIPTION,
                                    PodcastContract.EpisodeEntry.COLUMN_PUB_DATE,
                                    PodcastContract.EpisodeEntry.COLUMN_PODCAST_TITLE
                            },
                            PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + "<? AND " + PodcastContract.EpisodeEntry.COLUMN_PUB_DATE + ">=?" + favoritePodcastSelection,
                            new String[] { Long.toString(todaysDateInMilli - (ONE_DAY_IN_MILLI * 7)),Long.toString(todaysDateInMilli - (ONE_DAY_IN_MILLI * 14))},
                            null);
            }
            return null;

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.d(LOG_TAG, "in onLoadFinished");
            mEpisodeAdapters.swapCursor(data);
            mEpisodeAdapters.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.d(LOG_TAG, "in onLoaderReset");
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
                    selectedWeek = 0;
                    return getString(R.string.this_week);
                case 1:
                    selectedWeek = 1;
                    return getString(R.string.last_week);
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    thisWeekFragment = (EpisodeFragment) createdFragment;
                    break;
                case 1:
                    lastWeekFragment = (EpisodeFragment) createdFragment;
                    break;
            }
            return createdFragment;
        }
    }


}
