package hcmute.edu.vn.selfalarm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import hcmute.edu.vn.selfalarm.R;

public class BatteryMonitorService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification("Battery Monitor is running..."));
    }

    private Notification buildNotification(String content){
        return new NotificationCompat.Builder(this, "monitor channel")
                .setSmallIcon(R.drawable.ic_low_battery)
                .setContentTitle("Battery Monitor")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    "monitor channel",
                    "Battery Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notification tracking battery");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("update_message")) {
            String msg = intent.getStringExtra("update_message");
            Notification updated = buildNotification(msg);
            NotificationManagerCompat.from(this).notify(1, updated);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
