package hcmute.edu.vn.selfalarm.data;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.selfalarm.receivers.NotificationReceiver;
import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.activities.MainActivity;
import hcmute.edu.vn.selfalarm.model.Task;
import hcmute.edu.vn.selfalarm.activities.RingActivity;
import hcmute.edu.vn.selfalarm.model.Alarm;

public class NotificationHelper {
    private static final String CHANNEL_ID = "task_reminder_channel";
    private static final String CHANNEL_NAME = "Task Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for task reminders";
    private static final int NOTIFICATION_ID = 1;

    private Context context;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.enableLights(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleTaskNotification(Task task) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(), // Use task ID as request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.getDueTime(),
                        pendingIntent
                );
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.getDueTime(),
                    pendingIntent
            );
        }
    }

    public void cancelTaskNotification(int taskId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public void showTaskDueNotification(String title, String description) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_active)
                .setContentTitle("Task Due: " + title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setLights(android.graphics.Color.BLUE, 3000, 3000)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public Notification createAlarmNotification(Alarm alarm) {
        Intent notificationIntent = new Intent(context, RingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(context.getString(R.string.arg_alarm_obj), alarm);
        notificationIntent.putExtra(context.getString(R.string.bundle_alarm_obj), bundle);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String alarmTitle = alarm != null ? alarm.getTitle() : context.getString(R.string.alarm_title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Ring Ring .. Ring Ring")
                .setContentText(alarmTitle)
                .setSmallIcon(R.drawable.ic_alarm_white_24dp)
                .setSound(null)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true);

        return builder.build();
    }
} 