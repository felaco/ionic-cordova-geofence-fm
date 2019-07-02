/**
 *
 */
package cl.finmarkets.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cl.finmarkets.demo.geofence.MainActivity;

public class GeofenceFM extends CordovaPlugin {
    private static final String TAG = "GeofenceFM";
    private GeofencingClient geofencingClient;
    private Context context;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> geofenceList = new ArrayList<>();

    private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }

    private Action executedAction;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "Inicializando GeofenceFM");
        context = this.cordova.getActivity().getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(context);
        this.cordova.getActivity().startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + this.cordova.getActivity().getPackageName())));
    }


    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        executedAction = new Action(action, args, callbackContext);

        cordova.getThreadPool().execute(() -> {
            switch (action) {
                case "init":
                    if (callbackContext == null)
                        Log.d(TAG, "Callback nulo 0");
                    else
                        Log.d(TAG, "Callback non-nulo 0");
                    initialize(callbackContext);
                    break;
                case "addOrUpdateFence":
                    try {
                        Log.d(TAG, "args json object: " + args.getJSONObject(0));
                        String id = args.optJSONObject(0).optString("id");
                        double lat = args.optJSONObject(0).optDouble("latitud");
                        double lng = args.optJSONObject(0).optDouble("longitud");
                        Double radius = args.optJSONObject(0).optDouble("radius");

                        Geofence geofence = this.buildGeofence(id, lat, lng, radius.floatValue());
                        if (checkPermission())
                            this.geofencingClient.addGeofences(this.getGeofencingRequest(geofence),
                                    this.getGeofencePendingIntent());


                        final PluginResult result = new PluginResult(PluginResult.Status.OK,
                                "Hola todo addOrUpdateFence... ");
                        callbackContext.sendPluginResult(result);

                    } catch (Exception e) {

                        Log.e(TAG, "execute: Error " + e.getMessage());
                        callbackContext.error(e.getMessage());
                    }

                    break;
                case "removeGeofence":
                    try {
                        String id = args.optJSONObject(0).optString("id");
                        List<String> idList = new LinkedList<>();
                        idList.add(id);

                        Task<Void> task = this.geofencingClient.removeGeofences(idList);
                        task.addOnSuccessListener(aVoid -> callbackContext.success());
                        task.addOnFailureListener(e -> callbackContext.error(e.getMessage()));

                    } catch (Exception e) {
                        callbackContext.error(e.getMessage());
                    }
                    break;
            }

        });
        return true;
    }

    private PendingIntent getGeofencePendingIntent() {
        if (this.geofencePendingIntent != null)
            return this.geofencePendingIntent;

        Intent intent = new Intent(this.context, GeofenceBroadcastReceiver.class);
        this.geofencePendingIntent = PendingIntent.getBroadcast(this.context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return this.geofencePendingIntent;
    }

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL | GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private Geofence buildGeofence(String id, double lat, double lng, float radius) {
        return new Geofence.Builder()
                .setRequestId(id)
                .setLoiteringDelay(1000 * 60 * 10)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT
                        | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setNotificationResponsiveness(1000 * 40)
                .build();
    }

    public boolean execute(Action action) throws JSONException {
        return execute(action.action, action.args, action.callbackContext);
    }

    private void initialize(CallbackContext callbackContext) {
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        if (!hasPermissions(permissions)) {
            Log.d(TAG, "Solicitando Permisos");
            PermissionHelper.requestPermissions(this, 0, permissions);
        } else {
            if (callbackContext == null) {
                Log.d(TAG, "Callback nulo 1");
                return;
            }
            callbackContext.success();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!PermissionHelper.hasPermission(this, permission))
                return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {

        PluginResult result;

        if (executedAction != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    executedAction.callbackContext.sendPluginResult(result);
                    executedAction = null;
                    return;
                }
            }
            Log.d(TAG, "Permission Granted!");
            execute(executedAction);
            executedAction = null;
        }

    }

    private boolean checkPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this.context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

}
