package hcmute.edu.vn.selfalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import hcmute.edu.vn.selfalarm.service.BatteryMonitorService;

public class SystemReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String message = "";

        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            boolean isCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    == BatteryManager.BATTERY_STATUS_CHARGING;
            message = "Pin: " + level + "% - " + (isCharging ? "Đang sạc" : "Không sạc");
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            message = "Màn hình đã bật";
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            message = "Màn hình đã tắt";
        }

        // Gửi Intent vào Service để cập nhật notification
        Intent serviceIntent = new Intent(context, BatteryMonitorService.class);
        serviceIntent.putExtra("update_message", message);
        context.startService(serviceIntent);
    }
}
