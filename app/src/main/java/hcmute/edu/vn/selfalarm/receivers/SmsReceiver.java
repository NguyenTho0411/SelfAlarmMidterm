package hcmute.edu.vn.selfalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build; // Thêm import
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import hcmute.edu.vn.selfalarm.model.Sms;
import hcmute.edu.vn.selfalarm.providers.SmsContentProvider;
import hcmute.edu.vn.selfalarm.service.BlacklistService; // Thêm import

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                String format = bundle.getString("format"); // Thêm lấy format

                if (pdus != null && pdus.length > 0) {
                    SmsMessage smsMessage = null;
                    String sender = null;
                    String messageBody = null;
                    long timestamp = 0;

                    try {
                        // Lấy thông tin từ PDU đầu tiên
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0], format);
                        } else {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0]);
                        }

                        sender = smsMessage.getOriginatingAddress();
                        messageBody = smsMessage.getMessageBody(); // Có thể cần nối các phần nếu tin nhắn dài
                        timestamp = smsMessage.getTimestampMillis();

                        Log.d(TAG, "SMS received from: " + sender + ", message: " + messageBody);

                        // **1. Lưu vào ContentProvider của bạn (Giữ nguyên code gốc)**
                        if (sender != null && messageBody != null) {
                            saveSmsToProvider(context, sender, messageBody, timestamp);
                        }

                        // **2. Khởi chạy BlacklistService để kiểm tra và chặn nếu cần**
                        if (sender != null) {
                            Intent serviceIntent = new Intent(context, BlacklistService.class);
                            serviceIntent.setAction(BlacklistService.ACTION_CHECK_SMS);
                            serviceIntent.putExtra(BlacklistService.EXTRA_PHONE_NUMBER, sender);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent);
                            } else {
                                context.startService(serviceIntent);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS PDU", e);
                    }
                }
            }
        }
    }

    // Giữ nguyên hàm này
    private void saveSmsToProvider(Context context, String address, String body, long date) {
        Sms sms = new Sms(address, body, date, false);
        ContentValues values = new ContentValues();
        values.put("address", sms.getAddress());
        values.put("body", sms.getBody());
        values.put("date", sms.getDate());
        values.put("type", sms.getType());
        try { // Thêm try-catch để tránh crash nếu provider lỗi
            context.getContentResolver().insert(SmsContentProvider.CONTENT_URI, values);
        } catch (Exception e) {
            Log.e(TAG, "Error saving SMS to local provider", e);
        }
    }
}