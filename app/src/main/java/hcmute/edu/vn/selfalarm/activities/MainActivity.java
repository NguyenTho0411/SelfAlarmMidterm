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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.fragments.AlarmPageFragment;
import hcmute.edu.vn.selfalarm.fragments.AlarmsListFragment;
import hcmute.edu.vn.selfalarm.fragments.BlacklistFragment;
import hcmute.edu.vn.selfalarm.fragments.CallsFragment;
import hcmute.edu.vn.selfalarm.fragments.MusicPlayerFragment;
import hcmute.edu.vn.selfalarm.fragments.SmsFragment;
import hcmute.edu.vn.selfalarm.receivers.CallReceiver;
import hcmute.edu.vn.selfalarm.receivers.SmsReceiver;
import hcmute.edu.vn.selfalarm.receivers.SystemReceiver;
import hcmute.edu.vn.selfalarm.service.BatteryMonitorService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor edit;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddCall, fabAddSms;
    private Fragment currentFragment;

    private SmsReceiver smsReceiver;
    private CallReceiver callReceiver;
    private TelephonyManager telephonyManager;
    private SystemReceiver systemReceiver;
    private boolean receiversRegistered = false;

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

        setupBottomNavigation();
        setupFabs();

        loadTheme();

        if (checkInitialPermissions()) {
            if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                loadFragment(new MusicPlayerFragment());
                bottomNavigationView.setSelectedItemId(R.id.nav_music);
            }
            registerReceiversIfNeeded();
            startBatteryMonitorService();
        } else {
            Log.w(TAG, "Initial permissions not granted. Waiting for user.");
        }
    }

    private void loadTheme() {
        boolean useLightTheme = sharedPreferences.getBoolean(getString(R.string.dayNightTheme), true);
        AppCompatDelegate.setDefaultNightMode(useLightTheme ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment = null;
            fabAddCall.setVisibility(View.GONE);
            fabAddSms.setVisibility(View.GONE);

            if (itemId == R.id.nav_music) {
                targetFragment = new MusicPlayerFragment();
            } else if (itemId == R.id.nav_calendar) {
                targetFragment = new AlarmPageFragment();
            } else if (itemId == R.id.nav_sms) {
                targetFragment = new SmsFragment();
                fabAddSms.setVisibility(View.VISIBLE);
            } else if (itemId == R.id.nav_call) {
                targetFragment = new CallsFragment();
                fabAddCall.setVisibility(View.VISIBLE);
            } else if (itemId == R.id.nav_blacklist) {
                targetFragment = new BlacklistFragment();
            }

            if (targetFragment != null) {
                loadFragment(targetFragment);
                return true;
            }
            return false;
        });
    }

    private void setupFabs() {
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
    }

    private void registerReceiversIfNeeded() {
        if (!receiversRegistered && checkInitialPermissions()) {
            Log.d(TAG, "Registering receivers...");
            smsReceiver = new SmsReceiver();
            IntentFilter smsFilter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            smsFilter.setPriority(999);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsReceiver, smsFilter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(smsReceiver, smsFilter);
            }

            callReceiver = new CallReceiver(this);
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try { telephonyManager.listen(callReceiver, PhoneStateListener.LISTEN_CALL_STATE); }
                catch (SecurityException e) { Log.e(TAG, "SecEx registering CallReceiver", e); }
            }

            systemReceiver = new SystemReceiver();
            IntentFilter systemFilter = new IntentFilter();
            systemFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            systemFilter.addAction(Intent.ACTION_SCREEN_ON);
            systemFilter.addAction(Intent.ACTION_SCREEN_OFF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemReceiver, systemFilter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(systemReceiver, systemFilter);
            }
            receiversRegistered = true;
        }
    }

    private void startBatteryMonitorService() {
        Intent systemServiceIntent = new Intent(this, BatteryMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { startForegroundService(systemServiceIntent); }
            else { startService(systemServiceIntent); }
        } catch (Exception e) { Log.e(TAG, "Error starting BatteryMonitorService", e); }
    }

    private void requestPermission() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        String[] requiredPermissions = {
                Manifest.permission.READ_SMS, Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG, Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.MODIFY_PHONE_STATE,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.POST_NOTIFICATIONS : null,
                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
        };

        for (String permission : requiredPermissions) {
            if (permission != null && ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "All required permissions already granted.");
            registerReceiversIfNeeded();
            startBatteryMonitorService();
        }
    }

    private boolean checkInitialPermissions() {
        String[] initialPermissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_SMS,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE
        };
        for (String p : initialPermissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Unregistering receivers...");
        try {
            if (smsReceiver != null && receiversRegistered) unregisterReceiver(smsReceiver);
            if (systemReceiver != null && receiversRegistered) unregisterReceiver(systemReceiver);
            if (telephonyManager != null && callReceiver != null && receiversRegistered) telephonyManager.listen(callReceiver, PhoneStateListener.LISTEN_NONE);
            receiversRegistered = false;
        } catch (IllegalArgumentException e) { Log.w(TAG, "Error unregistering receiver: " + e.getMessage()); }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment == null) return;
        Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());
        currentFragment = fragment;
        FragmentManager manager = getSupportFragmentManager();
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
        if (item.getItemId() == R.id.dayNigthMode) {
            boolean isLightCurrent = sharedPreferences.getBoolean(getString(R.string.dayNightTheme), true);
            if (isLightCurrent) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                edit.putBoolean(getString(R.string.dayNightTheme), false).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                edit.putBoolean(getString(R.string.dayNightTheme), true).apply();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "onRequestPermissionsResult received.");
            boolean allRequiredGranted = true;
            String[] initialPermissionsForCheck = {
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECEIVE_SMS,
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE
            };

            for (String initialPerm : initialPermissionsForCheck) {
                boolean found = false;
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(initialPerm)) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            allRequiredGranted = false;
                            Log.w(TAG, "Essential permission denied: " + permissions[i]);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found && ContextCompat.checkSelfPermission(this, initialPerm) != PackageManager.PERMISSION_GRANTED) {
                    allRequiredGranted = false;
                    Log.w(TAG, "Essential permission not previously granted and not in current request: " + initialPerm);
                }
            }

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                } else {
                    Log.d(TAG, "Permission granted: " + permissions[i]);
                }
            }

            if (allRequiredGranted) {
                Log.d(TAG, "Essential permissions granted. Registering receivers and loading initial fragment.");
                registerReceiversIfNeeded();
                startBatteryMonitorService();
                if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
                    loadFragment(new MusicPlayerFragment());
                    bottomNavigationView.setSelectedItemId(R.id.nav_music);
                }
            } else {
                Log.w(TAG, "Not all essential permissions were granted. App might not function correctly.");
                Toast.makeText(this, "Essential permissions denied. Please grant permissions in settings.", Toast.LENGTH_LONG).show();
            }
        }
    }
}