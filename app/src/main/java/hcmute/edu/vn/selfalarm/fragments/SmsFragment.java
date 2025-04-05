package hcmute.edu.vn.selfalarm.fragments;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import hcmute.edu.vn.selfalarm.model.Sms;
import hcmute.edu.vn.selfalarm.providers.SmsContentProvider;

public class SmsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int SMS_LOADER_ID = 1;
    private static final int CONTACTS_LOADER_ID = 2;
    
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private SmsAdapter adapter;
    private List<Sms> smsList;
    private View sendSmsView;
    private boolean isSendSmsViewVisible = false;
    private String selectedPhoneNumber = "";
    private ContactsAdapter contactsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sms, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerViewSms);
        textEmpty = view.findViewById(R.id.textEmptySms);
        
        smsList = new ArrayList<>();
        adapter = new SmsAdapter(smsList);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Initialize send SMS view
        sendSmsView = inflater.inflate(R.layout.send_sms, container, false);
        setupSendSmsView();
        
        // Initialize the loader
        LoaderManager.getInstance(this).initLoader(SMS_LOADER_ID, null, this);
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restart the loader to refresh data when fragment becomes visible
        LoaderManager.getInstance(this).restartLoader(SMS_LOADER_ID, null, this);
    }

    private void setupSendSmsView() {
        TextInputEditText editSearchContacts = sendSmsView.findViewById(R.id.editSearchContacts);
        TextInputEditText editMessage = sendSmsView.findViewById(R.id.editMessage);
        TextView textSelectedContact = sendSmsView.findViewById(R.id.textSelectedContact);
        Button buttonSendSms = sendSmsView.findViewById(R.id.buttonSendSms);
        RecyclerView recyclerViewContacts = sendSmsView.findViewById(R.id.recyclerViewContacts);

        // Setup contacts RecyclerView
        contactsAdapter = new ContactsAdapter(new ArrayList<>(), phoneNumber -> {
            selectedPhoneNumber = phoneNumber;
            textSelectedContact.setText("Selected Contact: " + phoneNumber);
        });
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewContacts.setAdapter(contactsAdapter);

        // Load contacts
        loadContacts();

        buttonSendSms.setOnClickListener(v -> {
            if (selectedPhoneNumber.isEmpty()) {
                Toast.makeText(getContext(), "Please select a contact", Toast.LENGTH_SHORT).show();
                return;
            }

            String message = editMessage.getText().toString();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "SMS permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                SmsManager.getDefault().sendTextMessage(selectedPhoneNumber, null, message, null, null);
                
                // Save SMS to our provider
                Sms sms = new Sms(selectedPhoneNumber, message, System.currentTimeMillis(), true);
                saveSmsToProvider(sms);
                
                // Clear fields
                editMessage.setText("");
                selectedPhoneNumber = "";
                textSelectedContact.setText("Selected Contact:");
                
                // Switch back to list view
                toggleSendSmsView();
                
                // Restart loader to refresh the list
                LoaderManager.getInstance(this).restartLoader(SMS_LOADER_ID, null, this);
                
                Toast.makeText(getContext(), "SMS sent successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadContacts() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<Contact> contacts = new ArrayList<>();
        Cursor cursor = requireContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String number = cursor.getString(1);
                contacts.add(new Contact(name, number));
            } while (cursor.moveToNext());
            cursor.close();
        }

        contactsAdapter.updateContacts(contacts);
    }

    public void toggleSendSmsView() {
        ViewGroup parent = (ViewGroup) getView();
        if (parent != null) {
            if (isSendSmsViewVisible) {
                parent.removeView(sendSmsView);
                parent.addView(recyclerView);
                parent.addView(textEmpty);
            } else {
                parent.removeView(recyclerView);
                parent.removeView(textEmpty);
                parent.addView(sendSmsView);
            }
            isSendSmsViewVisible = !isSendSmsViewVisible;
        }
    }

    private static class Contact {
        String name;
        String phoneNumber;

        Contact(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }
    }

    private static class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
        private List<Contact> contacts;
        private final OnContactSelectedListener listener;

        interface OnContactSelectedListener {
            void onContactSelected(String phoneNumber);
        }

        ContactsAdapter(List<Contact> contacts, OnContactSelectedListener listener) {
            this.contacts = contacts;
            this.listener = listener;
        }

        void updateContacts(List<Contact> newContacts) {
            this.contacts = newContacts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact, parent, false);
            return new ContactViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
            Contact contact = contacts.get(position);
            holder.textContactName.setText(contact.name);
            holder.textPhoneNumber.setText(contact.phoneNumber);
            holder.itemView.setOnClickListener(v -> listener.onContactSelected(contact.phoneNumber));
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        static class ContactViewHolder extends RecyclerView.ViewHolder {
            TextView textContactName;
            TextView textPhoneNumber;

            ContactViewHolder(View itemView) {
                super(itemView);
                textContactName = itemView.findViewById(R.id.textContactName);
                textPhoneNumber = itemView.findViewById(R.id.textPhoneNumber);
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == SMS_LOADER_ID) {
            return new CursorLoader(requireContext(),
                    SmsContentProvider.CONTENT_URI,
                    null, null, null, "date DESC");
        }
        throw new IllegalArgumentException("Unknown loader id: " + id);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == SMS_LOADER_ID) {
            smsList.clear();
            
            if (data != null && data.moveToFirst()) {
                do {
                    String address = data.getString(data.getColumnIndexOrThrow("address"));
                    String body = data.getString(data.getColumnIndexOrThrow("body"));
                    long date = data.getLong(data.getColumnIndexOrThrow("date"));
                    int type = data.getInt(data.getColumnIndexOrThrow("type"));
                    
                    smsList.add(new Sms(address, body, date, type));
                } while (data.moveToNext());
                
                if (smsList.isEmpty()) {
                    textEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    textEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            } else {
                // No data found
                textEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (loader.getId() == SMS_LOADER_ID) {
            smsList.clear();
            adapter.notifyDataSetChanged();
        }
    }

    // Method to save SMS to our ContentProvider
    public void saveSmsToProvider(Sms sms) {
        ContentValues values = new ContentValues();
        values.put("address", sms.getAddress());
        values.put("body", sms.getBody());
        values.put("date", sms.getDate());
        values.put("type", sms.getType());
        
        requireContext().getContentResolver().insert(SmsContentProvider.CONTENT_URI, values);
    }

    // Method to import SMS from system
    public void importSmsFromSystem() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = requireContext().getContentResolver().query(
                uri,
                new String[]{"address", "body", "date", "type"},
                null,
                null,
                "date DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(0);
                String body = cursor.getString(1);
                long date = cursor.getLong(2);
                int type = cursor.getInt(3);

                Sms sms = new Sms(address, body, date, type);
                saveSmsToProvider(sms);
            } while (cursor.moveToNext());
            cursor.close();
            
            // Restart loader to refresh the list
            LoaderManager.getInstance(this).restartLoader(SMS_LOADER_ID, null, this);
        }
    }

    private static class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {
        private final List<Sms> smsList;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public SmsAdapter(List<Sms> smsList) {
            this.smsList = smsList;
        }

        @NonNull
        @Override
        public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sms, parent, false);
            return new SmsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
            Sms sms = smsList.get(position);
            holder.textPhoneNumber.setText(sms.getAddress());
            holder.textMessage.setText(sms.getBody());
            holder.textDate.setText(dateFormat.format(new Date(sms.getDate())));
        }

        @Override
        public int getItemCount() {
            return smsList.size();
        }

        static class SmsViewHolder extends RecyclerView.ViewHolder {
            TextView textPhoneNumber, textMessage, textDate;

            SmsViewHolder(View itemView) {
                super(itemView);
                textPhoneNumber = itemView.findViewById(R.id.textPhoneNumber);
                textMessage = itemView.findViewById(R.id.textMessage);
                textDate = itemView.findViewById(R.id.textDate);
            }
        }
    }
} 