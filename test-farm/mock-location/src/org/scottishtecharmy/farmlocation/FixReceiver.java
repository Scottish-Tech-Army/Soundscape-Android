package org.scottishtecharmy.farmlocation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * The broadcast way in to Fixes, which the farm used before FixProvider:
 *
 *   am broadcast -n org.scottishtecharmy.farmlocation/.FixReceiver \
 *       -a org.scottishtecharmy.farmlocation.FIX \
 *       --es lat 55.9474 --es lon -4.3058 --es speed 1.4 --es bearing 92.5
 *
 * A broadcast has a delivery timeout, and on a busy phone (the Moto) late ones got
 * the helper reported as not responding, with a dialog over Soundscape once
 * KeepAlive made it a foreground app; a provider call has no such timeout.
 */
public class FixReceiver extends BroadcastReceiver {
    private static final String FIX = "org.scottishtecharmy.farmlocation.FIX";
    private static final String STOP = "org.scottishtecharmy.farmlocation.STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (STOP.equals(intent.getAction())) {
            Fixes.stop(context);
            setResult(Activity.RESULT_OK, "stopped", null);
        } else if (FIX.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            String error = Fixes.submit(context, extras == null ? new Bundle() : extras);
            setResult(error == null ? Activity.RESULT_OK : Activity.RESULT_CANCELED,
                    error == null ? "ok" : error, null);
        } else {
            setResult(Activity.RESULT_CANCELED, "unknown action", null);
        }
    }
}
