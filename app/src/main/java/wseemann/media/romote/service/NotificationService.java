package wseemann.media.romote.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jaku.core.JakuRequest;
import com.jaku.parser.AppsParser;
import com.jaku.parser.IconParser;
import com.jaku.request.QueryActiveAppRequest;
import com.jaku.request.QueryIconRequest;

import java.util.List;
import java.util.Random;

import com.jaku.model.Channel;

import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.NotificationUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 6/19/16.
 */
public class NotificationService extends Service {

    public static final String TAG = NotificationService.class.getName();

    public static final int NOTIFICATION = 100;

    private NotificationManager mNM;
    private Notification notification;

    private Channel mChannel;
    private Device mDevice;

    private SharedPreferences mPreferences;

    private MediaSession mediaSession;

    private int mServiceStartId;
    private boolean mServiceInUse = true;

    private final Random mGenerator = new Random();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NotificationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NotificationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setUpMediaSession();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.UPDATE_DEVICE_BROADCAST);
        registerReceiver(mUpdateReceiver, intentFilter);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getString(R.string.app_name), getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(TAG);
            channel.enableLights(false);
            channel.enableVibration(false);
            mNM.createNotificationChannel(channel);
        }

        //startForeground(NotificationService.NOTIFICATION, notification);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangedListener);

        try {
            mDevice = PreferenceUtils.getConnectedDevice(this);

            if (enableNotification && mDevice != null) {
                notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null, mediaSession.getSessionToken());

                mNM.notify(NOTIFICATION, notification);
                sendStatusCommand();
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        mServiceStartId = startId;

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mUpdateReceiver);

        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferencesChangedListener);
        mediaSession.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mServiceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;
        return true;
    }

    private void sendStatusCommand() {
        String url = CommandHelper.getDeviceURL(this);

        QueryActiveAppRequest queryActiveAppRequest = new QueryActiveAppRequest(url);
        JakuRequest request = new JakuRequest(queryActiveAppRequest, new AppsParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                List<Channel> channels = (List<Channel>) result.mResultValue;

                if (channels.size() > 0) {
                    mChannel = channels.get(0);
                    getAppIcon(mChannel.getId());
                }
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                Log.d(TAG, "That didn't work!");
            }
        }).execute(RokuRequestTypes.query_active_app);
    }

    private void getAppIcon(String appId) {
        String url = CommandHelper.getDeviceURL(this);

        QueryIconRequest queryIconRequest = new QueryIconRequest(url, appId);
        JakuRequest request = new JakuRequest(queryIconRequest, new IconParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                try {
                    byte [] data = (byte []) result.mResultValue;

                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                    mDevice = PreferenceUtils.getConnectedDevice(NotificationService.this);
                    updateMediaSessionMetadata(mChannel, bitmap);
                    notification = NotificationUtils.buildNotification(
                            NotificationService.this,
                            mDevice.getModelName(),
                            mChannel.getTitle(),
                            bitmap,
                            mediaSession.getSessionToken());
                    mNM.notify(NOTIFICATION, notification);
                } catch (Exception ex) {
                }
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                Log.d(TAG, "That didn't work!");
            }
        }).execute(RokuRequestTypes.query_icon);
    }

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationService.this);
            boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

            if (enableNotification) {
                notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null, mediaSession.getSessionToken());

                if (enableNotification && mDevice != null) {
                    mNM.notify(NOTIFICATION, notification);
                    sendStatusCommand();
                } else {
                    mNM.cancel(NOTIFICATION);
                }
            }
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("notification_checkbox_preference")) {
                mPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationService.this);
                boolean enableNotification = mPreferences.getBoolean("notification_checkbox_preference", false);

                mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangedListener);

                if (notification == null) {
                    notification = NotificationUtils.buildNotification(NotificationService.this, null, null, null, mediaSession.getSessionToken());
                }

                if (enableNotification && mDevice != null) {
                    mNM.notify(NOTIFICATION, notification);
                    sendStatusCommand();
                } else {
                    mNM.cancel(NOTIFICATION);
                }
            }
        }
    };

    private void setUpMediaSession() {
        mediaSession = new MediaSession(this, TAG);
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(
                new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PLAYING, 0L, 0F)
                        .setActions(PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_PLAY |
                                PlaybackState.ACTION_REWIND |
                                PlaybackState.ACTION_FAST_FORWARD)
                        .build());
        mediaSession.setMetadata(new MediaMetadata.Builder().build());
    }

    private void updateMediaSessionMetadata(final Channel channel, final Bitmap bitmap) {
        MediaMetadata.Builder builder = new MediaMetadata.Builder();
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, channel.getTitle());
        builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);

        mediaSession.setMetadata(builder.build());
    }
}