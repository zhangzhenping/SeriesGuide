
package com.battlelancer.seriesguide.appwidget;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.Utils;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

@TargetApi(11)
public class ListWidgetProvider extends AppWidgetProvider {

    public static final String UPDATE = "com.battlelancer.seriesguide.appwidget.UPDATE";

    public static final long REPETITION_INTERVAL = 5 * DateUtils.MINUTE_IN_MILLIS;

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        // remove the update alarm if the last widget is gone
        Intent update = new Intent(UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 195, update, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                    ListWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view);
            // Toast.makeText(context,
            // "ListWidgets called to refresh " + Arrays.toString(appWidgetIds),
            // Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            // Toast.makeText(context, "Refreshing widget " + appWidgetIds[i],
            // Toast.LENGTH_SHORT)
            // .show();

            RemoteViews rv = buildRemoteViews(context, appWidgetIds[i]);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // set an alarm to update widgets every x mins if the device is awake
        Intent update = new Intent(UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 195, update, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()
                + REPETITION_INTERVAL, REPETITION_INTERVAL, pi);
    }

    @SuppressWarnings("deprecation")
    public static RemoteViews buildRemoteViews(Context context, int appWidgetId) {

        // Here we setup the intent which points to the StackViewService
        // which will provide the views for this collection.
        Intent intent = new Intent(context, ListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // When intents are compared, the extras are ignored, so we need to
        // embed the extras into the data so that the extras will not be
        // ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_v11);
        if (Utils.isICSOrHigher()) {
            rv.setRemoteAdapter(R.id.list_view, intent);
        } else {
            rv.setRemoteAdapter(appWidgetId, R.id.list_view, intent);
        }

        // The empty view is displayed when the collection has no items. It
        // should be a sibling of the collection view.
        rv.setEmptyView(R.id.list_view, R.id.empty_view);

        // change title based on config
        SharedPreferences prefs = context.getSharedPreferences(ListWidgetConfigure.PREFS_NAME, 0);
        int listType = prefs.getInt(ListWidgetConfigure.PREF_LISTTYPE_KEY + appWidgetId,
                R.id.radioUpcoming);
        int selection = 0;
        switch (listType) {
            case R.id.radioUpcoming:
                selection = 0;
                rv.setTextViewText(R.id.widgetTitle, context.getString(R.string.upcoming));
                break;
            case R.id.radioRecent:
                selection = 1;
                rv.setTextViewText(R.id.widgetTitle, context.getString(R.string.recent));
                break;
        }

        // Create an Intent to launch Upcoming
        Intent pi = new Intent(context, UpcomingRecentActivity.class);
        pi.putExtra(UpcomingRecentActivity.InitBundle.SELECTED_TAB, selection);
        pi.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, pi,
                PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

        // Create intents for items to launch an EpisodeDetailsActivity
        Intent itemIntent = new Intent(context, EpisodeDetailsActivity.class);
        PendingIntent pendingIntentTemplate = PendingIntent.getActivity(context, 1, itemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.list_view, pendingIntentTemplate);
        return rv;
    }
}
