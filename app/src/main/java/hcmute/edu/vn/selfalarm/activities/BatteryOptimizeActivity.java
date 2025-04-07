package hcmute.edu.vn.selfalarm.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import hcmute.edu.vn.selfalarm.R;

public class BatteryOptimizeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        handleBrightness();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Settings.System.canWrite(this)) {
            handleBrightness();
        }
    }

    private void handleBrightness() {
        String action = getIntent().getStringExtra("action");
        if ("optimize".equals(action)) {
            setScreenBrightness(30); // Giảm độ sáng
        } else if ("restore".equals(action)) {
            setScreenBrightness(150); // Phục hồi độ sáng
        }
        finish();
    }

    private void setScreenBrightness(int brightnessValue) {
        Settings.System.putInt(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
        );
    }
}