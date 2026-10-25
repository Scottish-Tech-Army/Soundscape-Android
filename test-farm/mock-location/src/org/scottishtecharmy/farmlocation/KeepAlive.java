package org.scottishtecharmy.farmlocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * A foreground service that does nothing but keep this process running. Without
 * it the process only exists while FixReceiver handles a fix, and a phone short of
 * memory (the 936 MB Cubot J10) killed it straight after nearly every one, so each
 * fix started a new process: about 3,000 in two hours, which slowed the phone
 * enough that Maestro's swipes arrived as taps. Started by the farm:
 *
 *   am start-foreground-service -n org.scottishtecharmy.farmlocation/.KeepAlive
 */
public class KeepAlive extends Service {
    private static final String CHANNEL = "farm";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Test farm", NotificationManager.IMPORTANCE_MIN));
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Soundscape farm location")
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
