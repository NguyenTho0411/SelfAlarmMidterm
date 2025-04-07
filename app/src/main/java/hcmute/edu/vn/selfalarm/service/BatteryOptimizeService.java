package hcmute.edu.vn.selfalarm.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import hcmute.edu.vn.selfalarm.R;

public class BatteryOptimizeService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification("Battery Optimize is running..."));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.hasExtra("update_message")) {
            String msg = intent.getStringExtra("update_message");
            Notification updated = buildNotification(msg);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(1, updated);
            }
            NotificationManagerCompat.from(this).notify(1, updated);


        }
        return START_STICKY;
    }
    private Notification buildNotification(String content){
        return new NotificationCompat.Builder(this, "battery optimize channel")
                .setSmallIcon(R.drawable.ic_low_battery)
                .setContentTitle("Battery Optimize")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    "optimize channel",
                    "Battery Optimize",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notification optimizing battery");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

}
