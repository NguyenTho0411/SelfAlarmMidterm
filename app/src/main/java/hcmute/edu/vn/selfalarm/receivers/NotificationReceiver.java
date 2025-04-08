package hcmute.edu.vn.selfalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import hcmute.edu.vn.selfalarm.data.NotificationHelper;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("task_title");
        String description = intent.getStringExtra("task_description");
        
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showTaskDueNotification(title, description);
    }
} 