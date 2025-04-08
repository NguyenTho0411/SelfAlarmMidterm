package hcmute.edu.vn.selfalarm.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent; // Thêm import
import android.os.Build; // Thêm import
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import hcmute.edu.vn.selfalarm.model.CallLogEntry;
import hcmute.edu.vn.selfalarm.providers.CallContentProvider;
import hcmute.edu.vn.selfalarm.service.BlacklistService; // Thêm import

public class CallReceiver extends PhoneStateListener {
    private static final String TAG = "CallReceiver";
    private Context context;
    private String currentNumber = null; // Dùng biến này để theo dõi số hiện tại
    private long callStartTime = 0; // Khởi tạo = 0
    private boolean isIncoming = false; // Cờ để biết cuộc gọi đến hay đi

    public CallReceiver(Context context) {
        // Lưu application context để tránh leak memory
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCallStateChanged(int state, String phoneNumber) { // phoneNumber là tên biến cũ, giữ nguyên
        // Log.d(TAG, "onCallStateChanged - State: " + state + ", Number: " + phoneNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                currentNumber = phoneNumber; // Lưu số gọi đến
                callStartTime = System.currentTimeMillis(); // Lưu thời điểm bắt đầu đổ chuông
                Log.d(TAG, "Incoming call RINGING from: " + currentNumber);

                // **Khởi chạy BlacklistService để kiểm tra**
                if (currentNumber != null && !currentNumber.isEmpty()) {
                    Intent serviceIntent = new Intent(context, BlacklistService.class);
                    serviceIntent.setAction(BlacklistService.ACTION_CHECK_CALL);
                    serviceIntent.putExtra(BlacklistService.EXTRA_PHONE_NUMBER, currentNumber);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                }
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (!isIncoming) { // Nếu không phải cuộc gọi đến -> là cuộc gọi đi
                    // Ở đây phoneNumber thường là null khi offhook cuộc gọi đi, cần lấy số từ nguồn khác nếu muốn log
                    currentNumber = phoneNumber; // Thử lấy số, có thể null
                    Log.d(TAG, "Outgoing call ACTIVE to: " + currentNumber);
                    if (callStartTime == 0) callStartTime = System.currentTimeMillis(); // Bắt đầu đếm giờ gọi đi
                } else {
                    // Cuộc gọi đến được trả lời
                    Log.d(TAG, "Incoming call ANSWERED from: " + currentNumber);
                    // callStartTime đã được set khi RINGING
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                Log.d(TAG, "Call state IDLE. Previous number: " + currentNumber);
                // Chỉ lưu log nếu có số điện thoại và thời gian bắt đầu hợp lệ
                if (currentNumber != null && !currentNumber.isEmpty() && callStartTime > 0) {
                    long callEndTime = System.currentTimeMillis();
                    long durationMillis = callEndTime - callStartTime;
                    int durationSeconds = (int) (durationMillis / 1000);

                    int callType;
                    if (isIncoming) {
                        // Kiểm tra xem có offhook không để phân biệt nhỡ và nghe máy
                        // Tuy nhiên, PhoneStateListener có thể không nhận được OFFHOOK nếu cuộc gọi bị chặn nhanh
                        // Giả định đơn giản: nếu duration > 0 là nghe máy, = 0 là nhỡ/bị chặn
                        callType = (durationSeconds > 0) ? android.provider.CallLog.Calls.INCOMING_TYPE : android.provider.CallLog.Calls.MISSED_TYPE;
                        Log.d(TAG, "Incoming call ended/missed. Number: " + currentNumber + ", Duration: " + durationSeconds + "s, Type: " + callType);
                    } else {
                        callType = android.provider.CallLog.Calls.OUTGOING_TYPE;
                        Log.d(TAG, "Outgoing call ended. Number: " + currentNumber + ", Duration: " + durationSeconds + "s");
                    }

                    // **Lưu vào ContentProvider của bạn (Giữ nguyên logic)**
                    saveCallToProvider(currentNumber, callStartTime, durationSeconds, callType);

                } else {
                    Log.d(TAG, "IDLE state, but no valid number or start time to log.");
                }
                // Reset trạng thái sau khi xử lý IDLE
                currentNumber = null;
                callStartTime = 0;
                isIncoming = false;
                break;
        }
    }

    // Giữ nguyên hàm này
    private void saveCallToProvider(String number, long date, int duration, int type) {
        CallLogEntry call = new CallLogEntry(number, date, duration, type);
        ContentValues values = new ContentValues();
        values.put("number", call.getNumber());
        values.put("date", call.getDate());
        values.put("duration", call.getDuration());
        values.put("type", call.getType());
        try { // Thêm try-catch
            context.getContentResolver().insert(CallContentProvider.CONTENT_URI, values);
        } catch (Exception e) {
            Log.e(TAG, "Error saving call log to local provider", e);
        }
    }
}