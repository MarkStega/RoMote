package wseemann.media.romote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import wseemann.media.romote.R;
import wseemann.media.romote.activity.MainActivity;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.receiver.CommandReceiver;
import wseemann.media.romote.service.CommandService;
import wseemann.media.romote.utils.PreferenceUtils;

import com.jaku.core.KeypressKeyValues;

public class RokuAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = RokuAppWidgetProvider.class.getName();

    public static final String CMDAPPWIDGETUPDATE = RokuAppWidgetProvider.class.getName();

    private static RokuAppWidgetProvider sInstance;

    public static synchronized RokuAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new RokuAppWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called");

        defaultAppWidget(context, appWidgetIds);
    }

    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        Device device = null;

        try {
            device = PreferenceUtils.getConnectedDevice(context);
        } catch (Exception ex) {
            return;
        }

        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.roku_appwidget);

        views.setTextViewText(R.id.model_name_text, device.getModelName());

        linkButtons(context, views, false /* not playing */);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.info_layout, pendingIntent);

        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link CommandService}
     */
    void notifyChange(CommandService service, String what) {
        if (hasInstances(service)) {
            performUpdate(service, null);
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(CommandService service, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.roku_appwidget);

        // Link actions buttons to intents
        linkButtons(service, views, true);

        Intent intent = new Intent(service, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(service,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.info_layout, pendingIntent);

        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     *
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MainActivity},
     *            otherwise we launch {@link MainActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        Log.d(TAG, "linkButtons called");

        linkButton(context, views, KeypressKeyValues.BACK, R.id.back_button, 0);
        linkButton(context, views, KeypressKeyValues.UP, R.id.up_button, 1);
        linkButton(context, views, KeypressKeyValues.HOME, R.id.home_button, 2);

        linkButton(context, views, KeypressKeyValues.LEFT, R.id.left_button, 3);
        linkButton(context, views, KeypressKeyValues.SELECT, R.id.ok_button, 4);
        linkButton(context, views, KeypressKeyValues.RIGHT, R.id.right_button, 5);

        linkButton(context, views, KeypressKeyValues.INTANT_REPLAY, R.id.instant_replay_button, 6);
        linkButton(context, views, KeypressKeyValues.DOWN, R.id.down_button, 7);
        linkButton(context, views, KeypressKeyValues.INFO, R.id.info_button, 8);

        linkButton(context, views, KeypressKeyValues.REV, R.id.rev_button, 9);
        linkButton(context, views, KeypressKeyValues.PLAY, R.id.play_button, 10);
        linkButton(context, views, KeypressKeyValues.FWD, R.id.fwd_button, 11);
    }

    private void linkButton(Context context, RemoteViews views, KeypressKeyValues keypressKeyValue, int id, int requestCode) {
        // Connect up various buttons and touch events
        final ComponentName serviceName = new ComponentName(context, CommandReceiver.class);

        Intent intent = new Intent();
        intent.putExtra("keypress", keypressKeyValue);
        intent.setComponent(serviceName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                requestCode /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(id, pendingIntent);
    }
}
