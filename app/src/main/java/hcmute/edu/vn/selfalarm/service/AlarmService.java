package hcmute.edu.vn.selfalarm.service;

import static hcmute.edu.vn.selfalarm.App.CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.activities.RingActivity;
import hcmute.edu.vn.selfalarm.data.NotificationHelper;
import hcmute.edu.vn.selfalarm.model.Alarm;

public class AlarmService extends Service {
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private NotificationHelper notificationHelper;
    private Alarm alarm;
    private Uri ringtone;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        notificationHelper = new NotificationHelper(this);
        ringtone = RingtoneManager.getActualDefaultRingtoneUri(this.getBaseContext(), RingtoneManager.TYPE_ALARM);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getBundleExtra(getString(R.string.bundle_alarm_obj));
        if (bundle != null) {
            alarm = (Alarm) bundle.getSerializable(getString(R.string.arg_alarm_obj));
        }

        // Start foreground service with proper type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificationHelper.createAlarmNotification(alarm), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notificationHelper.createAlarmNotification(alarm));
        }

        // Setup media player and start sound
        setupMediaPlayer();

        // Start vibration if enabled
        if (alarm != null && alarm.isVibrate()) {
            startVibration();
        }

        return START_STICKY;
    }

    private void setupMediaPlayer() {
        try {
            if (alarm != null && alarm.getTone() != null) {
                mediaPlayer.setDataSource(this.getBaseContext(), Uri.parse(alarm.getTone()));
            } else {
                mediaPlayer.setDataSource(this.getBaseContext(), ringtone);
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            ex.printStackTrace();
            // Fallback to default ringtone if custom one fails
            try {
                mediaPlayer.setDataSource(this.getBaseContext(), ringtone);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(1000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
