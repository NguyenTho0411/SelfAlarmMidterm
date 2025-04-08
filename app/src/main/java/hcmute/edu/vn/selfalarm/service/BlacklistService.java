package hcmute.edu.vn.selfalarm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.data.BlacklistDbHelper;
import hcmute.edu.vn.selfalarm.providers.BlacklistContentProvider;

public class BlacklistService extends Service {

    private static final String TAG = "BlacklistService";
    private static final String CHANNEL_ID = "BlacklistServiceChannel";
    private static final int NOTIFICATION_ID = 3;

    public static final String ACTION_CHECK_SMS = "hcmute.edu.vn.selfalarm.ACTION_CHECK_SMS";
    public static final String ACTION_CHECK_CALL = "hcmute.edu.vn.selfalarm.ACTION_CHECK_CALL";
    public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";

    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SelfAlarm Protection")
                .setContentText("Checking number...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        if (intent != null) {
            final String action = intent.getAction();
            final String phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                executorService.submit(() -> {
                    boolean isBlocked = isNumberInBlacklist(phoneNumber);
                    if (isBlocked) {
                        if (ACTION_CHECK_CALL.equals(action)) {
                            Log.i(TAG, "Blocking incoming call from blacklisted number: " + phoneNumber);
                            blockCall();
                        } else if (ACTION_CHECK_SMS.equals(action)) {
                            Log.i(TAG, "Deleting SMS from blacklisted number: " + phoneNumber);
                            blockSms(phoneNumber);
                        }
                    }
                    stopSelf(startId);
                });
            } else {
                stopSelf(startId);
            }
        } else {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    private boolean isNumberInBlacklist(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return false;
        Cursor cursor = null;
        try {
            Uri queryUri = Uri.withAppendedPath(BlacklistContentProvider.CONTENT_URI, "number/" + Uri.encode(phoneNumber));
            cursor = getContentResolver().query(queryUri, new String[]{BlacklistDbHelper.COLUMN_ID}, null, null, null);
            return (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "Error querying blacklist for " + phoneNumber, e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void blockCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    boolean success = telecomManager.endCall();
                    Log.d(TAG, "TelecomManager endCall success: " + success);
                    return;
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException ending call with TelecomManager.", e);
                }
            }
            Log.w(TAG, "Unable to block call: TelecomManager failed or permission denied.");
        } else {
            Log.w(TAG, "Call blocking not supported on API < 28 without deprecated methods.");
            // Optionally, notify the user that call blocking isn’t available on older devices
        }
    }

    private void blockSms(String sender) {
        ContentResolver contentResolver = getContentResolver();
        Uri inboxUri = Uri.parse("content://sms/inbox");
        int deletedRows = 0;
        try {
            String selection = Telephony.Sms.ADDRESS + " = ?";
            String[] selectionArgs = {sender};
            deletedRows = contentResolver.delete(inboxUri, selection, selectionArgs);
            Log.d(TAG, "Deleted " + deletedRows + " system SMS from " + sender);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting system SMS from " + sender, e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Blacklist Check", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }
}