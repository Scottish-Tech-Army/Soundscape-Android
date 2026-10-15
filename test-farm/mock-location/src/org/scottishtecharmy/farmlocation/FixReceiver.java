package org.scottishtecharmy.farmlocation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

/**
 * Sets one mock fix on the gps and network test providers, sent by the test farm
 * once a second:
 *
 *   am broadcast -n org.scottishtecharmy.farmlocation/.FixReceiver \
 *       -a org.scottishtecharmy.farmlocation.FIX \
 *       --es lat 55.9474 --es lon -4.3058 --es speed 1.4 --es bearing 92.5
 *
 * speed and bearing are optional. Soundscape only takes its direction from travel
 * when a fix says the user is moving, so without them it falls back to the
 * phone's compass. Action STOP removes the test providers again.
 *
 * Needs the mock location app-op: appops set <this package> android:mock_location allow
 */
public class FixReceiver extends BroadcastReceiver {
    private static final String FIX = "org.scottishtecharmy.farmlocation.FIX";
    private static final String STOP = "org.scottishtecharmy.farmlocation.STOP";
    private static final String[] PROVIDERS = {
        LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER
    };

    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context context, Intent intent) {
        LocationManager lm = context.getSystemService(LocationManager.class);
        try {
            if (STOP.equals(intent.getAction())) {
                for (String p : PROVIDERS) {
                    try {
                        lm.removeTestProvider(p);
                    } catch (IllegalArgumentException notAdded) {
                        // nothing to remove
                    }
                }
                setResult(Activity.RESULT_OK, "stopped", null);
                return;
            }
            if (!FIX.equals(intent.getAction())) {
                setResult(Activity.RESULT_CANCELED, "unknown action", null);
                return;
            }
            double lat = Double.parseDouble(intent.getStringExtra("lat"));
            double lon = Double.parseDouble(intent.getStringExtra("lon"));
            float accuracy = parse(intent.getStringExtra("accuracy"), 5f);
            float speed = parse(intent.getStringExtra("speed"), -1f);
            float bearing = parse(intent.getStringExtra("bearing"), -1f);

            for (String p : PROVIDERS) {
                try {
                    lm.addTestProvider(p, false, false, false, false, true, true, true,
                            Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
                } catch (IllegalArgumentException alreadyAdded) {
                    // a provider from an earlier fix
                }
                lm.setTestProviderEnabled(p, true);

                Location fix = new Location(p);
                fix.setLatitude(lat);
                fix.setLongitude(lon);
                fix.setAltitude(0);
                fix.setAccuracy(accuracy);
                fix.setVerticalAccuracyMeters(accuracy);
                fix.setTime(System.currentTimeMillis());
                fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                if (speed >= 0) {
                    fix.setSpeed(speed);
                    fix.setSpeedAccuracyMetersPerSecond(0.3f);
                }
                if (bearing >= 0) {
                    fix.setBearing(bearing);
                    fix.setBearingAccuracyDegrees(10f);
                }
                lm.setTestProviderLocation(p, fix);
            }
            setResult(Activity.RESULT_OK, "ok", null);
        } catch (RuntimeException e) {
            // Most likely a SecurityException: the mock location app-op isn't allowed.
            setResult(Activity.RESULT_CANCELED, e.getClass().getSimpleName() + ": " + e.getMessage(), null);
        }
    }

    private static float parse(String value, float fallback) {
        return value == null ? fallback : Float.parseFloat(value);
    }
}
