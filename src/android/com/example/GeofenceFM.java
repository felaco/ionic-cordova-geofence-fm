/**
 */
package com.example;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class GeofenceFM extends CordovaPlugin {
    private static final String TAG = "GeofenceFM";
    private GeofenceSingleton geofenceSingleton;
    private Context context;

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
        context = this.cordova.getActivity().getApplicationContext();
        Log.d(TAG, "Inicializando GeofenceFM");
        geofenceSingleton = new GeofenceSingleton(this.cordova.getActivity());
        this.cordova.getActivity().startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+this.cordova.getActivity().getPackageName())));
    }



    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        executedAction = new Action(action, args, callbackContext);

        cordova.getThreadPool().execute(() -> {
            if (action.equals("init")) {
                if (callbackContext == null)
                    Log.d(TAG, "Callback nulo 0");
                else
                    Log.d(TAG, "Callback non-nulo 0");
                initialize(callbackContext);
            }

            if (action.equals("addOrUpdateFence")) {
                try {
                    for (int i = 0; i < args.optJSONArray(0).length(); i++) {

                        Log.d(TAG, "args.getJSONObject -> " + args.optJSONArray(0).optJSONObject(i));

                        String id = args.optJSONArray(0).optJSONObject(i).optString("id");
                        double latitud = args.optJSONArray(0).optJSONObject(i).optDouble("latitud");
                        double longitud = args.optJSONArray(0).optJSONObject(i).optDouble("longitud");
                        int radius = args.optJSONArray(0).optJSONObject(i).optInt("radius");

                        geofenceSingleton.addGeofence(latitud, longitud, radius, id);
                    }

                    geofenceSingleton.startGeofencing(cordova.getActivity());

                    final PluginResult result = new PluginResult(PluginResult.Status.OK,
                                                                 "Hola todo addOrUpdateFence... ");
                    callbackContext.sendPluginResult(result);

                } catch (Exception e) {

                    Log.e(TAG, "execute: Error " + e.getMessage());
                    callbackContext.error(e.getMessage());
                }

            } else if (action.equals("removeGeofence")){
                try {
                    String id = args.optJSONObject(0).optString("id");
                    geofenceSingleton.removeGeoFence(id);

                    PluginResult result = new PluginResult(PluginResult.Status.OK, "Exito, creo...");
                    callbackContext.sendPluginResult(result);

                } catch (Exception e){
                    callbackContext.error(e.getMessage());
                }
            }

        });
        return true;
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
            for (int r:grantResults) {
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

}
