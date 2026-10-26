package org.scottishtecharmy.farmlocation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
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
 * The fix is set on a worker thread and the broadcast answered straight away:
 * on a phone under memory pressure (the Moto) the location calls sometimes took
 * longer than the broadcast timeout, and the helper was reported as not
 * responding, which with KeepAlive running puts a dialog over Soundscape. A fix
 * that fails is reported in the answer to the next one. Only the newest fix
 * waits to be set, so a slow phone skips fixes rather than falling behind.
 *
 * Needs the mock location app-op: appops set <this package> android:mock_location allow
 */
public class FixReceiver extends BroadcastReceiver {
    private static final String FIX = "org.scottishtecharmy.farmlocation.FIX";
    private static final String STOP = "org.scottishtecharmy.farmlocation.STOP";
    private static final String[] PROVIDERS = {
        LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER
    };

    private static final Object lock = new Object();
    private static Handler worker;
    private static Intent pending;      // the newest fix not yet set
    private static String lastError;    // from the last fix that failed, until reported
    private static boolean added;       // the test providers exist

    @Override
    public void onReceive(Context context, Intent intent) {
        if (STOP.equals(intent.getAction())) {
            LocationManager lm = context.getSystemService(LocationManager.class);
            synchronized (lock) {
                pending = null;
                added = false;
            }
            for (String p : PROVIDERS) {
                try {
                    lm.removeTestProvider(p);
                } catch (IllegalArgumentException notAdded) {
                    // nothing to remove
                } catch (RuntimeException e) {
                    // the app-op isn't allowed, so there's nothing of ours to remove
                }
            }
            setResult(Activity.RESULT_OK, "stopped", null);
            return;
        }
        if (!FIX.equals(intent.getAction())) {
            setResult(Activity.RESULT_CANCELED, "unknown action", null);
            return;
        }
        String error;
        synchronized (lock) {
            error = lastError;
            lastError = null;
            boolean scheduled = pending != null;
            pending = intent;
            if (worker == null) {
                HandlerThread thread = new HandlerThread("fixes");
                thread.start();
                worker = new Handler(thread.getLooper());
            }
            if (!scheduled) {
                Context app = context.getApplicationContext();
                worker.post(() -> setNewest(app));
            }
        }
        if (error == null) {
            setResult(Activity.RESULT_OK, "ok", null);
        } else {
            setResult(Activity.RESULT_CANCELED, error, null);
        }
    }

    private static void setNewest(Context context) {
        Intent intent;
        synchronized (lock) {
            intent = pending;
            pending = null;
        }
        if (intent == null) {
            return;
        }
        try {
            setFix(context.getSystemService(LocationManager.class), intent);
        } catch (RuntimeException e) {
            // Most likely a SecurityException: the mock location app-op isn't allowed.
            synchronized (lock) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                added = false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void setFix(LocationManager lm, Intent intent) {
        double lat = Double.parseDouble(intent.getStringExtra("lat"));
        double lon = Double.parseDouble(intent.getStringExtra("lon"));
        float accuracy = parse(intent.getStringExtra("accuracy"), 5f);
        float speed = parse(intent.getStringExtra("speed"), -1f);
        float bearing = parse(intent.getStringExtra("bearing"), -1f);

        boolean add;
        synchronized (lock) {
            add = !added;
        }
        for (String p : PROVIDERS) {
            if (add) {
                try {
                    lm.addTestProvider(p, false, false, false, false, true, true, true,
                            Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
                } catch (IllegalArgumentException alreadyAdded) {
                    // left from before this process started
                }
                lm.setTestProviderEnabled(p, true);
            }

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
        synchronized (lock) {
            added = true;
        }
    }

    private static float parse(String value, float fallback) {
        return value == null ? fallback : Float.parseFloat(value);
    }
}
