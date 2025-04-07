package hcmute.edu.vn.selfalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import hcmute.edu.vn.selfalarm.model.Sms;
import hcmute.edu.vn.selfalarm.providers.SmsContentProvider;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();
                        long timestamp = smsMessage.getTimestampMillis();

                        Log.d(TAG, "SMS received from: " + sender + ", message: " + messageBody);

                        // Save the SMS to our ContentProvider
                        saveSmsToProvider(context, sender, messageBody, timestamp);
                    }
                }
            }
        }
    }

    private void saveSmsToProvider(Context context, String address, String body, long date) {
        // Create a new SMS object (false for isOutgoing since this is an incoming message)
        Sms sms = new Sms(address, body, date, false);

        // Save to ContentProvider
        ContentValues values = new ContentValues();
        values.put("address", sms.getAddress());
        values.put("body", sms.getBody());
        values.put("date", sms.getDate());
        values.put("type", sms.getType());

        context.getContentResolver().insert(SmsContentProvider.CONTENT_URI, values);
    }
} 