package cl.finmarkets.geofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.LinkedList;
import java.util.List;

import cl.finmarkets.demo.geofence.MainActivity;
import cl.finmarkets.demo.geofence.R;

public class GeofenceTransitionsJobIntentService extends JobIntentService {
    private static final int JOB_ID = 573;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";

    public static void enqueueWork(Context context, Intent intent){
        enqueueWork(context, GeofenceTransitionsJobIntentService.class,
                JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        String s = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
        if (geofencingEvent.hasError()){
            Log.e(TAG, s);
        }

        this.createNotification(getMessageFromEvent(geofencingEvent));
    }

    protected String getMessageFromEvent(GeofencingEvent event){
        int transitionType = event.getGeofenceTransition();
        String notificationText;
        List<String> fencesIdTextList = new LinkedList<>();
        String fencesIdText;

        for (Geofence geofence : event.getTriggeringGeofences()) {
            fencesIdTextList.add(geofence.getRequestId());
        }
        fencesIdText = TextUtils.join(", ", fencesIdTextList);

        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
            notificationText = "Has entrado en la(s) cerca(s): " + fencesIdText;
        else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
            notificationText = "Has salido de la(s) cerca(s): " + fencesIdText;
        else
            notificationText = "Has estado un rato en la(s) cerca(s): " + fencesIdText;

        return notificationText;
    }

    protected void createNotification (String notificationText){
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Geofence";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name,
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(getApplicationContext().getApplicationInfo().icon)
                .setContentText(notificationText)
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(new long[] {0, 1500});

        manager.notify(0, builder.build());
    }
}
