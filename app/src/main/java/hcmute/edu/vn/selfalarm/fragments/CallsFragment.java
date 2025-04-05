package hcmute.edu.vn.selfalarm.fragments;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.model.CallLogEntry;
import hcmute.edu.vn.selfalarm.providers.CallContentProvider;

public class CallsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CALLS_LOADER_ID = 2;
    
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private CallsAdapter adapter;
    private List<CallLogEntry> callLogList;
    private View addCallView;
    private boolean isAddCallViewVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calls, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerViewCalls);
        textEmpty = view.findViewById(R.id.textEmptyCalls);
        
        callLogList = new ArrayList<>();
        adapter = new CallsAdapter(callLogList);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Initialize add call view
        addCallView = inflater.inflate(R.layout.add_call, container, false);
        setupAddCallView();
        
        // Initialize the loader
        LoaderManager.getInstance(this).initLoader(CALLS_LOADER_ID, null, this);
        
        return view;
    }

    private void setupAddCallView() {
        TextInputEditText editPhoneNumber = addCallView.findViewById(R.id.editPhoneNumber);
        TextInputEditText editDuration = addCallView.findViewById(R.id.editDuration);
        RadioGroup radioGroupCallType = addCallView.findViewById(R.id.radioGroupCallType);
        Button buttonSaveCall = addCallView.findViewById(R.id.buttonSaveCall);

        buttonSaveCall.setOnClickListener(v -> {
            String phoneNumber = editPhoneNumber.getText().toString();
            String durationStr = editDuration.getText().toString();
            
            if (phoneNumber.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int duration = Integer.parseInt(durationStr);
            int callType = getCallType(radioGroupCallType.getCheckedRadioButtonId());
            
            CallLogEntry call = new CallLogEntry(phoneNumber, System.currentTimeMillis(), duration, callType);
            saveCallToProvider(call);
            
            // Clear fields
            editPhoneNumber.setText("");
            editDuration.setText("");
            radioGroupCallType.clearCheck();
            
            // Switch back to list view
            toggleAddCallView();
            
            Toast.makeText(getContext(), "Call saved successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private int getCallType(int radioButtonId) {
        if (radioButtonId == R.id.radioIncoming) {
            return CallLog.Calls.INCOMING_TYPE;
        } else if (radioButtonId == R.id.radioOutgoing) {
            return CallLog.Calls.OUTGOING_TYPE;
        } else if (radioButtonId == R.id.radioMissed) {
            return CallLog.Calls.MISSED_TYPE;
        }
        return CallLog.Calls.OUTGOING_TYPE; // Default
    }

    public void toggleAddCallView() {
        ViewGroup parent = (ViewGroup) getView();
        if (parent != null) {
            if (isAddCallViewVisible) {
                parent.removeView(addCallView);
                parent.addView(recyclerView);
                parent.addView(textEmpty);
            } else {
                parent.removeView(recyclerView);
                parent.removeView(textEmpty);
                parent.addView(addCallView);
            }
            isAddCallViewVisible = !isAddCallViewVisible;
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == CALLS_LOADER_ID) {
            return new CursorLoader(requireContext(),
                    CallContentProvider.CONTENT_URI,
                    null, null, null, "date DESC");
        }
        throw new IllegalArgumentException("Unknown loader id: " + id);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CALLS_LOADER_ID) {
            callLogList.clear();
            
            if (data != null && data.moveToFirst()) {
                do {
                    String number = data.getString(data.getColumnIndexOrThrow("number"));
                    long date = data.getLong(data.getColumnIndexOrThrow("date"));
                    int duration = data.getInt(data.getColumnIndexOrThrow("duration"));
                    int type = data.getInt(data.getColumnIndexOrThrow("type"));
                    
                    callLogList.add(new CallLogEntry(number, date, duration, type));
                } while (data.moveToNext());
                
                if (callLogList.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (loader.getId() == CALLS_LOADER_ID) {
            callLogList.clear();
            adapter.notifyDataSetChanged();
        }
    }

    // Method to save Call to our ContentProvider
    public void saveCallToProvider(CallLogEntry call) {
        ContentValues values = new ContentValues();
        values.put("number", call.getNumber());
        values.put("date", call.getDate());
        values.put("duration", call.getDuration());
        values.put("type", call.getType());
        
        requireContext().getContentResolver().insert(CallContentProvider.CONTENT_URI, values);
    }

    // Method to import calls from system
    public void importCallsFromSystem() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String[] projection = new String[]{
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
        };

        Cursor cursor = requireContext().getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String number = cursor.getString(0);
                long date = cursor.getLong(1);
                int duration = cursor.getInt(2);
                int type = cursor.getInt(3);

                CallLogEntry call = new CallLogEntry(number, date, duration, type);
                saveCallToProvider(call);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private static class CallsAdapter extends RecyclerView.Adapter<CallsAdapter.CallsViewHolder> {
        private final List<CallLogEntry> callLogList;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public CallsAdapter(List<CallLogEntry> callLogList) {
            this.callLogList = callLogList;
        }

        @NonNull
        @Override
        public CallsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_call, parent, false);
            return new CallsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CallsViewHolder holder, int position) {
            CallLogEntry callLog = callLogList.get(position);
            holder.textPhoneNumber.setText(callLog.getNumber());
            holder.textDate.setText(dateFormat.format(new Date(callLog.getDate())));
            holder.textDuration.setText(formatDuration(callLog.getDuration()));
            
            // Set call type icon
            int iconResId;
            switch (callLog.getType()) {
                case CallLog.Calls.INCOMING_TYPE:
                    iconResId = android.R.drawable.ic_menu_call;
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    iconResId = android.R.drawable.ic_menu_call;
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    iconResId = android.R.drawable.ic_menu_call;
                    break;
                default:
                    iconResId = android.R.drawable.ic_menu_call;
            }
            holder.imageCallType.setImageResource(iconResId);
        }

        @Override
        public int getItemCount() {
            return callLogList.size();
        }

        private String formatDuration(int seconds) {
            if (seconds < 60) {
                return seconds + "s";
            } else if (seconds < 3600) {
                return (seconds / 60) + "m";
            } else {
                return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
            }
        }

        static class CallsViewHolder extends RecyclerView.ViewHolder {
            ImageView imageCallType;
            TextView textPhoneNumber, textDate, textDuration;

            CallsViewHolder(View itemView) {
                super(itemView);
                imageCallType = itemView.findViewById(R.id.imageCallType);
                textPhoneNumber = itemView.findViewById(R.id.textPhoneNumber);
                textDate = itemView.findViewById(R.id.textDate);
                textDuration = itemView.findViewById(R.id.textDuration);
            }
        }
    }
} 