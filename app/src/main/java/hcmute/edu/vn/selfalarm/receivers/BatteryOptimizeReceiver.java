package hcmute.edu.vn.selfalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import hcmute.edu.vn.selfalarm.service.BatteryOptimizeService;

public class BatteryOptimizeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String message = "";

        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

            SharedPreferences prefs = context.getSharedPreferences("battery_prefs", Context.MODE_PRIVATE);
            boolean isOptimized = prefs.getBoolean("is_optimized", false);

            if (level <= 20 && !isCharging) {
                // Gọi Service để giảm độ sáng
                Intent optimizeIntent = new Intent(context, BatteryOptimizeService.class);
                optimizeIntent.putExtra("update_message", "Lows battery, optimize...");
                optimizeIntent.putExtra("action", "optimize");
                context.startService(optimizeIntent);
                prefs.edit().putBoolean("is_optimized", true).apply();
            } else if (isCharging) {
                // Gọi Service để khôi phục độ sáng
                Intent restoreIntent = new Intent(context, BatteryOptimizeService.class);
                restoreIntent.putExtra("update_message", "Đang sạc, khôi phục độ sáng");
                restoreIntent.putExtra("action", "restore");
                context.startService(restoreIntent);
                prefs.edit().putBoolean("is_optimized", false).apply();
            }
        }

    }
}
