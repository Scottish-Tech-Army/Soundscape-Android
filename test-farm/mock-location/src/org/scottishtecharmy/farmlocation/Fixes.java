package org.scottishtecharmy.farmlocation;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

/**
 * Sets mock fixes on the gps and network test providers. The test farm sends one
 * a second, through FixProvider (and FixReceiver, its broadcast predecessor):
 *
 *   content call --uri content://org.scottishtecharmy.farmlocation --method fix \
 *       --extra lat:s:55.9474 --extra lon:s:-4.3058 --extra speed:s:1.4 --extra bearing:s:92.5
 *
 * speed and bearing are optional. Soundscape only takes its direction from travel
 * when a fix says the user is moving, so without them it falls back to the
 * phone's compass. Method (or action) stop removes the test providers again.
 *
 * The fix is set on a worker thread and the call answered straight away: on a
 * phone under memory pressure (the Moto) the location calls sometimes took
 * longer than a broadcast's timeout. A fix that fails is reported in the answer
 * to the next one. Only the newest fix waits to be set, so a slow phone skips
 * fixes rather than falling behind.
 *
 * Needs the mock location app-op: appops set <this package> android:mock_location allow
 */
final class Fixes {
    private Fixes() {}

    private static final String[] PROVIDERS = {
        LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER
    };

    private static final Object lock = new Object();
    private static Handler worker;
    private static Bundle pending;      // the newest fix not yet set
    private static String lastError;    // from the last fix that failed, until reported
    private static boolean added;       // the test providers exist

    /** Removes the test providers. */
    static void stop(Context context) {
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
    }

    /**
     * Queues a fix (lat, lon and optional accuracy, speed and bearing, as strings)
     * to be set on the worker thread, and returns at once: null, or the error from
     * an earlier fix that failed.
     */
    static String submit(Context context, Bundle fix) {
        String error;
        synchronized (lock) {
            error = lastError;
            lastError = null;
            boolean scheduled = pending != null;
            pending = fix;
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
        return error;
    }

    private static void setNewest(Context context) {
        Bundle fix;
        synchronized (lock) {
            fix = pending;
            pending = null;
        }
        if (fix == null) {
            return;
        }
        try {
            setFix(context.getSystemService(LocationManager.class), fix);
        } catch (RuntimeException e) {
            // Most likely a SecurityException: the mock location app-op isn't allowed.
            synchronized (lock) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                added = false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void setFix(LocationManager lm, Bundle extras) {
        double lat = Double.parseDouble(extras.getString("lat"));
        double lon = Double.parseDouble(extras.getString("lon"));
        float accuracy = parse(extras.getString("accuracy"), 5f);
        float speed = parse(extras.getString("speed"), -1f);
        float bearing = parse(extras.getString("bearing"), -1f);

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
