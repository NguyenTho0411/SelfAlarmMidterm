package hcmute.edu.vn.selfalarm.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import hcmute.edu.vn.selfalarm.R;
import hcmute.edu.vn.selfalarm.data.BlacklistDbHelper;
import hcmute.edu.vn.selfalarm.providers.BlacklistContentProvider;

public class BlacklistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int BLACKLIST_LOADER_ID = 3;
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private BlacklistAdapter adapter;
    private List<String> blacklistNumbers;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blacklist, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewBlacklist);
        textEmpty = view.findViewById(R.id.textEmptyBlacklist);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddBlacklist);

        blacklistNumbers = new ArrayList<>();
        adapter = new BlacklistAdapter(getContext(), blacklistNumbers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddBlacklistDialog());

        LoaderManager.getInstance(this).initLoader(BLACKLIST_LOADER_ID, null, this);
        return view;
    }

    private void showAddBlacklistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add to Blacklist");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String number = input.getText().toString().trim();
            if (!number.isEmpty()) addNumberToBlacklist(number); else Toast.makeText(getContext(), "Number cannot be empty", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addNumberToBlacklist(String number) {
        ContentValues values = new ContentValues();
        values.put(BlacklistDbHelper.COLUMN_PHONE_NUMBER, number);
        try {
            Uri newUri = requireContext().getContentResolver().insert(BlacklistContentProvider.CONTENT_URI, values);
            if (newUri != null) Toast.makeText(getContext(), "Added: " + number, Toast.LENGTH_SHORT).show();
            else Toast.makeText(getContext(), "Failed (already exists?)", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(getContext(), "Error adding", Toast.LENGTH_SHORT).show(); }
    }

    @NonNull @Override public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == BLACKLIST_LOADER_ID) return new CursorLoader(requireContext(), BlacklistContentProvider.CONTENT_URI, new String[]{BlacklistDbHelper.COLUMN_PHONE_NUMBER}, null, null, null);
        throw new IllegalArgumentException("Unknown loader id: " + id);
    }

    @Override public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == BLACKLIST_LOADER_ID) {
            blacklistNumbers.clear();
            if (data != null && data.moveToFirst()) {
                do { blacklistNumbers.add(data.getString(data.getColumnIndexOrThrow(BlacklistDbHelper.COLUMN_PHONE_NUMBER))); } while (data.moveToNext());
            }
            adapter.notifyDataSetChanged();
            textEmpty.setVisibility(blacklistNumbers.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(blacklistNumbers.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) { if (loader.getId() == BLACKLIST_LOADER_ID) { blacklistNumbers.clear(); adapter.notifyDataSetChanged(); } }

    // --- Adapter ---
    private static class BlacklistAdapter extends RecyclerView.Adapter<BlacklistAdapter.ViewHolder> {
        private final List<String> numbers; private final Context context;
        BlacklistAdapter(Context ctx, List<String> nums) { this.context = ctx; this.numbers = nums; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blacklist, parent, false); return new ViewHolder(v); }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) { String num = numbers.get(position); holder.textNumber.setText(num); holder.buttonDelete.setOnClickListener(v -> showDeleteConfirmation(num)); }
        @Override public int getItemCount() { return numbers.size(); }

        private void showDeleteConfirmation(String number) {
            new AlertDialog.Builder(context)
                    .setTitle("Remove from Blacklist")
                    .setMessage("Are you sure you want to remove " + number + "?")
                    .setPositiveButton("Remove", (dialog, which) -> deleteNumber(number))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteNumber(String number) {
            Uri uri = Uri.withAppendedPath(BlacklistContentProvider.CONTENT_URI, "number/" + Uri.encode(number));
            int deleted = context.getContentResolver().delete(uri, null, null);
            if (deleted > 0) Toast.makeText(context, "Removed: " + number, Toast.LENGTH_SHORT).show();
            else Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show();
            // LoaderManager sẽ tự cập nhật list
        }

        static class ViewHolder extends RecyclerView.ViewHolder { TextView textNumber; ImageButton buttonDelete; ViewHolder(View v) { super(v); textNumber = v.findViewById(R.id.textBlacklistNumber); buttonDelete = v.findViewById(R.id.buttonDeleteBlacklist); } }
    }
}