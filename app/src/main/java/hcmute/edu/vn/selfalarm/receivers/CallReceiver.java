package hcmute.edu.vn.selfalarm.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import hcmute.edu.vn.selfalarm.model.CallLogEntry;
import hcmute.edu.vn.selfalarm.providers.CallContentProvider;

public class CallReceiver extends PhoneStateListener {
    private static final String TAG = "CallReceiver";
    private Context context;
    private String incomingNumber = "";
    private long callStartTime;

    public CallReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                // Incoming call is ringing
                incomingNumber = phoneNumber;
                Log.d(TAG, "Incoming call from: " + incomingNumber);
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                // Call is answered or outgoing
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    // Outgoing call
                    Log.d(TAG, "Outgoing call to: " + phoneNumber);
                    callStartTime = System.currentTimeMillis();
                } else if (!incomingNumber.isEmpty()) {
                    // Incoming call answered
                    Log.d(TAG, "Incoming call answered from: " + incomingNumber);
                    callStartTime = System.currentTimeMillis();
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                // Call ended
                if (!incomingNumber.isEmpty()) {
                    long callDuration = (System.currentTimeMillis() - callStartTime) / 1000; // in seconds
                    Log.d(TAG, "Call ended with: " + incomingNumber + ", duration: " + callDuration + "s");
                    
                    // Save the call to our ContentProvider
                    saveCallToProvider(incomingNumber, callStartTime, (int) callDuration, 
                            TelephonyManager.CALL_STATE_RINGING);
                    
                    // Reset the incoming number
                    incomingNumber = "";
                }
                break;
        }
    }

    private void saveCallToProvider(String number, long date, int duration, int type) {
        // Create a new CallLogEntry object
        CallLogEntry call = new CallLogEntry(number, date, duration, type);

        // Save to ContentProvider
        ContentValues values = new ContentValues();
        values.put("number", call.getNumber());
        values.put("date", call.getDate());
        values.put("duration", call.getDuration());
        values.put("type", call.getType());

        context.getContentResolver().insert(CallContentProvider.CONTENT_URI, values);
    }
} 