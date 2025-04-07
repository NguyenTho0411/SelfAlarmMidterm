package hcmute.edu.vn.selfalarm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.selfalarm.R;

public class SystemMonitorService extends Service {
    private static final String TAG = "SystemMonitorService";
    private static final String CHANNEL_ID = "system_monitor_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_NAME = "System Monitor";
    private static final String CHANNEL_DESCRIPTION = "Monitors system state and optimizes settings";
    private static final String PREFS_NAME = "SystemMonitorPrefs";
    
    // Constants for brightness levels
    private static final int MIN_BRIGHTNESS = 50;
    private static final int MEDIUM_BRIGHTNESS = 150;
    private static final int MAX_BRIGHTNESS = 255;
    
    private SharedPreferences prefs;
    private WifiManager wifiManager;
    private boolean hasSyncPermission = false;
    private boolean hasSettingsPermission = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        checkPermissions();

    }

    private void checkPermissions() {
        try {
            // Check sync settings permission
            ContentResolver.getMasterSyncAutomatically();
            hasSyncPermission = true;
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read sync settings", e);
            hasSyncPermission = false;
        }

        try {
            // Check system settings permission
            Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            hasSettingsPermission = true;
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to read system settings", e);
            hasSettingsPermission = false;
        } catch (Settings.SettingNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification("System Monitor Service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (intent != null) {
            handleIntent(intent);
        }

        return START_STICKY;
    }

    private Notification buildNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Monitor")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void handleIntent(Intent intent) {
        String action = intent.getStringExtra("action");
        if (action != null) {
            switch (action) {
                case "screen_on":
                    handleScreenOn();
                    break;
                case "screen_off":
                    handleScreenOff();
                    break;
            }
        } else {
            float batteryLevel = intent.getFloatExtra("battery_level", -1);
            boolean isCharging = intent.getBooleanExtra("is_charging", false);
            int lowThreshold = intent.getIntExtra("low_threshold", 15);
            int mediumThreshold = intent.getIntExtra("medium_threshold", 30);

            if (batteryLevel >= 0) {
                optimizeSettings(batteryLevel, isCharging, lowThreshold, mediumThreshold);
            }
        }
    }

    private void optimizeSettings(float batteryLevel, boolean isCharging, int lowThreshold, int mediumThreshold) {
        try {
            boolean brightnessChanged = false;
            boolean wifiChanged = false;
            boolean syncChanged = false;

            if (batteryLevel <= lowThreshold && !isCharging) {
                // Low battery optimizations
                if (hasSettingsPermission && prefs.getBoolean("auto_brightness", true)) {
                    brightnessChanged = adjustBrightness(MIN_BRIGHTNESS);
                }

                if (prefs.getBoolean("auto_wifi", true)) {
                    wifiChanged = toggleWifi(false);
                }

                if (hasSyncPermission && prefs.getBoolean("auto_sync", true)) {
                    syncChanged = toggleSync(false);
                }

                showOptimizationService("Low battery", brightnessChanged, wifiChanged, syncChanged);

            } else if (batteryLevel <= mediumThreshold && !isCharging) {
                // Medium battery optimizations
                if (hasSettingsPermission && prefs.getBoolean("auto_brightness", true)) {
                    brightnessChanged = adjustBrightness(MEDIUM_BRIGHTNESS);
                    if (brightnessChanged) {
                        Toast.makeText(this, "Medium battery: Adjusting brightness", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (isCharging) {
                // Restore settings when charging
                if (hasSettingsPermission && prefs.getBoolean("auto_brightness", true)) {
                    brightnessChanged = adjustBrightness(MAX_BRIGHTNESS);
                }

                if (prefs.getBoolean("auto_wifi", true)) {
                    wifiChanged = toggleWifi(true);
                }

                if (hasSyncPermission && prefs.getBoolean("auto_sync", true)) {
                    syncChanged = toggleSync(true);
                }

                showOptimizationService("Charging", brightnessChanged, wifiChanged, syncChanged);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error optimizing settings", e);
            Toast.makeText(this, "Error optimizing settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean adjustBrightness(int targetBrightness) {
        try {
            int currentBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, MAX_BRIGHTNESS);
            if (currentBrightness != targetBrightness) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, targetBrightness);
                return true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to adjust brightness", e);
        }
        return false;
    }

    private boolean toggleWifi(boolean enable) {
        try {
            if (wifiManager.isWifiEnabled() != enable) {
                wifiManager.setWifiEnabled(enable);
                return true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to toggle WiFi", e);
        }
        return false;
    }

    private boolean toggleSync(boolean enable) {
        try {
            if (hasSyncPermission) {
                boolean currentState = ContentResolver.getMasterSyncAutomatically();
                if (currentState != enable) {
                    ContentResolver.setMasterSyncAutomatically(enable);
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to toggle sync", e);
        }
        return false;
    }

    private void showOptimizationService(String prefix, boolean brightnessChanged, boolean wifiChanged, boolean syncChanged) {
        String msg = "";
        if (brightnessChanged) {
            msg = prefix + ": Adjusting brightness";
            startForeground(1, buildNotification(msg));
        }

        if (wifiChanged) {
            String action = prefix.equals("Low battery") ? "Turning off" : "Restoring";
            msg = prefix + ": " + action + " WiFi";
            startForeground(1, buildNotification(msg));
        }

        if (syncChanged) {
            String action = prefix.equals("Low battery") ? "Disabling" : "Restoring";
            msg = prefix + ": " + action + " sync";
            startForeground(1, buildNotification(msg));
        }
    }

    private void handleScreenOn() {
        try {
            Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float) scale;
                boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == 
                                   BatteryManager.BATTERY_STATUS_CHARGING;

                optimizeSettings(batteryPct, isCharging, 15, 30);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling screen on", e);
        }
    }

    private void handleScreenOff() {
        if (prefs.getBoolean("auto_wifi", true)) {
            toggleWifi(false);
            Toast.makeText(this, "Screen off: Turning off WiFi", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 