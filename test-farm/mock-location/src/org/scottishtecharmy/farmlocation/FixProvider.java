package org.scottishtecharmy.farmlocation;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * How the farm sends fixes (see Fixes):
 *
 *   content call --uri content://org.scottishtecharmy.farmlocation --method fix \
 *       --extra lat:s:55.9474 --extra lon:s:-4.3058 --extra speed:s:1.4 --extra bearing:s:92.5
 *
 * The answer is a Bundle with result "ok" or an error. call() runs on a binder
 * thread with no timeout, so unlike a broadcast a busy phone can't get the helper
 * reported as not responding. Only callers holding android.permission.DUMP (adb's
 * shell) are served, as with FixReceiver.
 */
public class FixProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle answer = new Bundle();
        if (getContext().checkCallingPermission("android.permission.DUMP")
                != PackageManager.PERMISSION_GRANTED) {
            answer.putString("result", "DUMP permission required");
        } else if ("stop".equals(method)) {
            Fixes.stop(getContext());
            answer.putString("result", "stopped");
        } else if ("fix".equals(method)) {
            String error = Fixes.submit(getContext(), extras == null ? new Bundle() : extras);
            answer.putString("result", error == null ? "ok" : error);
        } else {
            answer.putString("result", "unknown method " + method);
        }
        return answer;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
