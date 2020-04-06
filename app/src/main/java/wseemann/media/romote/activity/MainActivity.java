package wseemann.media.romote.activity;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.jaku.core.JakuRequest;
import com.jaku.request.SearchRequest;

import wseemann.media.romote.R;
import wseemann.media.romote.fragment.ChannelFragment;
import wseemann.media.romote.fragment.InstallChannelDialog;
import wseemann.media.romote.fragment.MainFragment;
import wseemann.media.romote.fragment.RemoteFragment;
import wseemann.media.romote.fragment.SearchDialog;
import wseemann.media.romote.fragment.StoreFragment;
import wseemann.media.romote.service.NotificationService;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.RokuRequestTypes;

public class MainActivity extends ConnectivityActivity implements
        InstallChannelDialog.InstallChannelListener, SearchDialog.SearchDialogListener {

    private StoreFragment mStoreFragment;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private NotificationService mService;
    boolean mBound = false;

    private ChannelFragment mChannelFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("first_use", true)) {
            startActivity(new Intent(this, ConfigureDeviceActivity.class));
            finish();
        }

        Intent intent = getIntent();

        if (intent != null && intent.getData() != null) {
            String channelCode = intent.getData().getPath().replace("/install/", "");

            InstallChannelDialog fragment = InstallChannelDialog.getInstance(this, channelCode);
            fragment.show(getFragmentManager(), InstallChannelDialog.class.getName());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            public void onPageSelected(int position) {
                if (mChannelFragment != null) {
                    mChannelFragment.refresh();
                }
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        /*BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_devices) {
                    mViewPager.setCurrentItem(0);
                } else if (menuItem.getItemId() == R.id.action_remote) {
                    mViewPager.setCurrentItem(1);
                } else if (menuItem.getItemId() == R.id.action_channels) {
                    mViewPager.setCurrentItem(2);
                } else if (menuItem.getItemId() == R.id.action_store) {
                    mViewPager.setCurrentItem(3);
                }

                return false;
            }
        });*/

        // Bind to NotificationService
        Intent intent1 = new Intent(this, NotificationService.class);
        bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onWifiConnected() {
        if (mChannelFragment != null) {
            mChannelFragment.refresh();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_search) {
            SearchDialog fragment = SearchDialog.Companion.newInstance(this);
            fragment.show(getSupportFragmentManager(), SearchDialog.class.getName());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mViewPager.getCurrentItem() != 3) {
            return super.onKeyDown(keyCode, event);
        }

        if (mStoreFragment != null) {
            if (mStoreFragment.onKeyDown(keyCode, event)) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
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
            //return PlaceholderFragment.newInstance(position + 1);

            if (position == 0) {
                return new MainFragment();
            } else if (position == 1) {
                return new RemoteFragment();
            } else if (position == 2) {
                mChannelFragment = new ChannelFragment();
                return mChannelFragment;
            } else {
                mStoreFragment = new StoreFragment();
                return mStoreFragment;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_devices);
                case 1:
                    return getString(R.string.title_remote);
                case 2:
                    return getString(R.string.title_channels);
                case 3:
                    return getString(R.string.title_store);
            }
            return null;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onDialogCancelled(DialogFragment dialog) {
        dialog.dismiss();
    }

    @Override
    public void onInstallSelected(DialogFragment dialog) {
        dialog.dismiss();
    }

    @Override
    public void onSearch(String searchText) {
        performSearch(searchText);
    }

    private void performSearch(String searchText) {
        String url = CommandHelper.getDeviceURL(this);

        SearchRequest searchRequest = new SearchRequest(url, searchText, null, null, null, null, null, null, null, null, null);
        JakuRequest request = new JakuRequest(searchRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.search);
    }
}
