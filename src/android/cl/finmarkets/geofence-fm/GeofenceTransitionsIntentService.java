package cl.finmarkets.geofence;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationRequest;

import java.util.List;
import java.util.Random;

import cl.finmarkets.demo.geofence.MainActivity;
import cl.finmarkets.demo.geofence.R;

public class GeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = "Geofence-Service";
    String NOTIFICATION_CHANNEL_ID = "cl.finmarkets.geofence-demo";
    private static String[] messageText = new String[] {
            "Solo durante hoy, todo frutas con 20% de descuento",
            "Solo durante hoy, las carnes Chilenas con 15% de descuento",
            "Con tu tarjeta cencosud acumula el doble de puntos en tu próxima compra"
    };
    private static int notificationId = 2;

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "onStartCommand DESDE SERVICE BACKGROUND!!!******");

        String action = intent.getStringExtra("action");

        Log.i(TAG, "onReceive -> action -> " + action);

        if(action != null && action.equals("STOP_SERVICE")){
            stopSelf();
            return START_NOT_STICKY;
        }

        final GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Error in geofencing event");
            return START_NOT_STICKY;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        Log.i(TAG, "FenceTransition -> " + transitionType);

        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            createNotificationChannel();

            List<Geofence> triggerList = geofencingEvent.getTriggeringGeofences();
            Intent resultIntent = new Intent(this, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            for (Geofence fence : triggerList) {
                String fenceId = fence.getRequestId();

                Log.i(TAG, "Entered -> " + fenceId);
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.jumbo);
                String alertText = messageText[new Random().nextInt(messageText.length)];

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                                                                                    "cl.finmarkets.demo.geofence")
                        .setSmallIcon(getApplicationContext().getApplicationInfo().icon)
                        .setContentTitle("Notificación de oferta")
                        .setContentText(alertText)
                        .setVibrate(new long[] {500, 1000})
                        .setContentIntent(resultPendingIntent)
                        .setLargeIcon(b)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(alertText));

                NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notManager.notify(notificationId, builder.build());
                notificationId++;
            }
        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // do nothing
        } else {

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(Constants.LOCATION_INTERVAL);
            locationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


            Intent intentAction = new Intent(getApplicationContext(), GeofenceTransitionsIntentService.class);
            intentAction.putExtra("action", "STOP_SERVICE");
            PendingIntent pIntentlogin = PendingIntent.getService(getApplicationContext(), 1, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i(TAG, "Entro a uno -> **********");

                String channelName = "My Background Service";
                NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
                chan.setLightColor(Color.BLUE);
                chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                assert manager != null;
                manager.createNotificationChannel(chan);

                String packageName = getApplicationContext().getPackageName();
                Intent resultIntent = getApplicationContext().getPackageManager()
                                                             .getLaunchIntentForPackage(packageName);


                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                        1, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
                Notification notification = notificationBuilder.setOngoing(true)
                                                               .setSmallIcon(getApplicationContext().getApplicationInfo().icon)
                                                               .setContentTitle("App ejecutandose en segundo plano")
                                                               .setPriority(NotificationManager.IMPORTANCE_MIN)
                                                               .setCategory(Notification.CATEGORY_SERVICE)
                                                               .setContentIntent(resultPendingIntent)
                                                               .addAction(getApplicationContext().getApplicationInfo().icon, "APAGAR", pIntentlogin)
                                                               .build();


                startForeground(1, notification);

            } else {
                Log.i(TAG, "Entro a dos -> **********");
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                        .setContentTitle("App ejecutandose en segundo plano")
                        .setSmallIcon(getApplicationContext().getApplicationInfo().icon)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .addAction(getApplicationContext().getApplicationInfo().icon, "APAGAR", pIntentlogin)
                        .setAutoCancel(true);

                Notification notification = builder.build();

                startForeground(1, notification);
            }
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel(){
        String NOTIFICATION_CHANNEL_ID = "cl.finmarkets.geofence-demo";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "finmarkets-geofence-demo";
            String description = "Canal de notificaciones para app de muestra de geofence desde finmarkets";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    name,
                    importance);

            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
