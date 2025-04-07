package hcmute.edu.vn.selfalarm.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.fragments.AlarmPageFragment;
import hcmute.edu.vn.selfalarm.fragments.CallsFragment;
import hcmute.edu.vn.selfalarm.fragments.SmsFragment;
import hcmute.edu.vn.selfalarm.receivers.CallReceiver;
import hcmute.edu.vn.selfalarm.receivers.SmsReceiver;
import hcmute.edu.vn.selfalarm.receivers.SystemMonitorReceiver;
import hcmute.edu.vn.selfalarm.service.SystemMonitorService;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor edit;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddCall, fabAddSms;
    private Fragment currentFragment;
    // Receivers
    private SmsReceiver smsReceiver;
    private CallReceiver callReceiver;
    private TelephonyManager telephonyManager;
    private SystemMonitorReceiver systemMonitorReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        edit = sharedPreferences.edit();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddCall = findViewById(R.id.fabAddCall);
        fabAddSms = findViewById(R.id.fabSendSms);

        requestPermission();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_call) {
                loadFragment(new CallsFragment());
                fabAddCall.setVisibility(View.VISIBLE);
                fabAddSms.setVisibility(View.GONE);
            } else if (itemId == R.id.nav_sms) {
                loadFragment(new SmsFragment());
                fabAddCall.setVisibility(View.GONE);
                fabAddSms.setVisibility(View.VISIBLE);
            } else {
                loadFragment(new AlarmPageFragment());
                fabAddCall.setVisibility(View.VISIBLE);
                fabAddSms.setVisibility(View.VISIBLE);
            }
            return true;
        });

        fabAddCall.setOnClickListener(v -> {
            if (currentFragment instanceof CallsFragment) {
                ((CallsFragment) currentFragment).toggleAddCallView();
            }
        });

        fabAddSms.setOnClickListener(v -> {
            if (currentFragment instanceof SmsFragment) {
                ((SmsFragment) currentFragment).toggleSendSmsView();
            }
        });

        // Load default fragment
        loadFragment(new AlarmPageFragment());

        // Register receivers
        registerReceivers();
    }

    private void registerReceivers() {
        // Register SMS receiver
        smsReceiver = new SmsReceiver();
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, smsFilter);

        // Register Call receiver
        callReceiver = new CallReceiver(this);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(callReceiver, CallReceiver.LISTEN_CALL_STATE);
        }

        // Register System Monitor receiver
        systemMonitorReceiver = new SystemMonitorReceiver();
        IntentFilter systemFilter = new IntentFilter();
        systemFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        systemFilter.addAction(Intent.ACTION_SCREEN_ON);
        systemFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(systemMonitorReceiver, systemFilter);

        // Start System Monitor Service
        Intent systemServiceIntent = new Intent(this, SystemMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(systemServiceIntent);
        } else {
            startService(systemServiceIntent);
        }
    }

    private void requestPermission(){
        String[] permissions = {
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.POST_NOTIFICATIONS
        };

        boolean allPermissionsGranted =true;
        for (String permission : permissions){
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED){
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted){
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receivers
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }

        if (telephonyManager != null && callReceiver != null) {
            telephonyManager.listen(callReceiver, PhoneStateListener.LISTEN_NONE);
        }

        if (systemMonitorReceiver != null) {
            unregisterReceiver(systemMonitorReceiver);
        }
    }

    private void loadFragment(Fragment fragment){
        currentFragment =fragment;
        FragmentManager manager =getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.dayNigthMode){
            boolean dn=sharedPreferences.getBoolean(getString(R.string.dayNightTheme),true);
            if(dn) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                edit.putBoolean(getString(R.string.dayNightTheme),false).apply();
            }
            else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                edit.putBoolean(getString(R.string.dayNightTheme),true).apply();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                // Handle permission denial
            } else {
                // Register receivers after permissions are granted
                registerReceivers();
            }
        }
    }
}