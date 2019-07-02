package cl.finmarkets.geofence;

import android.content.Context;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.GeofenceStatusCodes;

public class GeofenceErrorMessages {

    /**
     * Returns the error string for a geofencing exception.
     */
    public static String getErrorString(Context context, Exception e) {
        if (e instanceof ApiException) {
            return getErrorString(context, ((ApiException) e).getStatusCode());
        } else {
            return "Error desconocido de geofence";
        }
    }

    /**
     * Returns the error string for a geofencing error code.
     */
    public static String getErrorString(Context context, int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "El servicio de geofence no está disponible";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Demasiadas geofences agregadas";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Demasiados pending intents están siendo utilizados";
            default:
                return "Error desconocido de geofence";
        }
    }
}
