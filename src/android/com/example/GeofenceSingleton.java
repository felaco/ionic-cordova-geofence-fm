package com.example;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

import static android.os.Looper.getMainLooper;

public class GeofenceSingleton {
    public static final String TAG = "GeofenceFM";

    private Context appContext;
    private static final String ERR_MSG = "Application Context is not set!! " +
            "Please call GeofenceSngleton.init() with proper application context";
    private PendingIntent mGeofencePendingIntent;
    private static ArrayList<Geofence> mGeofenceList;
    private GeofencingClient geofencingClient;
    private FusedLocationProviderClient mFusedLocationClient;

    public GeofenceSingleton(Activity activity) {
        if (activity == null)
            throw new IllegalStateException(ERR_MSG);

        appContext = activity.getApplicationContext();
        mGeofenceList = new ArrayList<Geofence>();

        geofencingClient = LocationServices.getGeofencingClient(appContext);

        if(!isServiceRunning(activity, GeofenceTransitionsIntentService.class.getName())) {
            Log.d(TAG, "isServiceRunning -> FALSO");
            Intent myService = new Intent(activity.getApplicationContext(), GeofenceTransitionsIntentService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(myService);
            } else {
                activity.startService(myService);
            }
        } else {
            Log.d(TAG, "isServiceRunning -> VERDAD");
        }
    }

    public void addGeofence(double latitude, double longitude, int radius, String uid) {
        Log.d(TAG, "addGeofence -> " + latitude + ", " + longitude + ", " + radius + ", " + uid);
        mGeofenceList.add(new Geofence.Builder()
                                  .setRequestId(uid)
                                  .setCircularRegion(
                                          latitude,
                                          longitude,
                                          radius
                                  )
                                  .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                                              Geofence.GEOFENCE_TRANSITION_EXIT)
                                  .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                  .build());
    }

    public synchronized void removeGeoFence(String id){
        List <String> removeList = new ArrayList<>();
        removeList.add(id);

        for (int i = 0; i < mGeofenceList.size(); i++){
            if (mGeofenceList.get(i).getRequestId().equals(id)){
                mGeofenceList.remove(i);
                break;
            }
        }

        LocationServices.getGeofencingClient(appContext).removeGeofences(removeList);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL | GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            Log.d(TAG, "mGeofencePendingIntent nulo");
            return mGeofencePendingIntent;
        }

        Log.d(TAG, "mGeofencePendingIntent non-nulo");
        Intent intent = new Intent(appContext, GeofenceTransitionsIntentService.class);
        mGeofencePendingIntent = PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    public void startGeofencing(Activity appActivity) {

        if (checkPermission()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);

            mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        Log.d(TAG, "Fused YO LOCAL");
                                        if (location != null) {
                                            if (checkPermission())
                                                geofencingClient.addGeofences(
                                                        getGeofencingRequest(),
                                                        getGeofencePendingIntent()
                                                ).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // your success code
                                                        Log.d(TAG, "YO LOCAL");
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // your fail code;
                                                        Log.d(TAG, "NOO LOCAL -> " + e.getMessage());
                                                    }
                                                });
                                            Log.d(TAG, "Geofencing started");
                                        }
                                    }
                                });

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(Constants.LOCATION_INTERVAL);
            locationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationCallback mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        Log.d(TAG, "Geofencing Latitude NOW -> " + String.valueOf(location.getLatitude()));
                        Log.d(TAG, "Geofencing Longitude NOW -> " + String.valueOf(location.getLongitude()));
                    }
                }
            };

            Handler mHandler = new Handler(getMainLooper());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (checkPermission())
                        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
                    Log.d(TAG, "FusedLocationClient requestLocationUpdates");
                }
            });


        } else {
            Log.d(TAG, "Not permissions");
        }
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        return (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    public void removeGeofence() {
        LocationServices.getGeofencingClient(appContext).removeGeofences(
                getGeofencePendingIntent()
        );
    }

    public void checkLocation(Activity activity) {
        LocationManager lm = (LocationManager)appContext.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        Log.d(TAG, "gps_enabled -> " + gps_enabled + ", network_enabled -> " + network_enabled);

    }

    public static boolean isServiceRunning(Activity activity, String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }
}
