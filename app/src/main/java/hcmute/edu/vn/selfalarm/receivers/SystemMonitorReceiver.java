package hcmute.edu.vn.selfalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.service.SystemMonitorService;

public class SystemMonitorReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "SystemMonitorPrefs";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_WIFI = "auto_wifi";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_LOW_BATTERY_THRESHOLD = "low_battery_threshold";
    private static final String KEY_MEDIUM_BATTERY_THRESHOLD = "medium_battery_threshold";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            handleBatteryChange(context, intent, prefs);
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            handleScreenOn(context, prefs);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            handleScreenOff(context, prefs);
        }
    }

    private void handleBatteryChange(Context context, Intent intent, SharedPreferences prefs) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = level * 100 / (float) scale;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        // Get thresholds from preferences
        int lowThreshold = prefs.getInt(KEY_LOW_BATTERY_THRESHOLD, 15);
        int mediumThreshold = prefs.getInt(KEY_MEDIUM_BATTERY_THRESHOLD, 30);

        // Update service with battery status
        Intent serviceIntent = new Intent(context, SystemMonitorService.class);
        serviceIntent.putExtra("battery_level", batteryPercentage);
        serviceIntent.putExtra("is_charging", isCharging);
        serviceIntent.putExtra("low_threshold", lowThreshold);
        serviceIntent.putExtra("medium_threshold", mediumThreshold);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Show battery status toast
        String statusText = String.format("Battery: %.1f%% %s",
                batteryPercentage,
                isCharging ? "(Charging)" : "");
        Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show();
    }

    private void handleScreenOn(Context context, SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_AUTO_BRIGHTNESS, true)) {
            // Restore brightness based on battery level
            Intent serviceIntent = new Intent(context, SystemMonitorService.class);
            serviceIntent.putExtra("action", "screen_on");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    private void handleScreenOff(Context context, SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_AUTO_WIFI, true)) {
            // Turn off WiFi when screen is off
            Intent serviceIntent = new Intent(context, SystemMonitorService.class);
            serviceIntent.putExtra("action", "screen_off");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
} 