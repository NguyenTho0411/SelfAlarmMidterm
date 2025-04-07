package hcmute.edu.vn.selfalarm.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.selfalarm.databinding.ItemAlarmBinding;
import hcmute.edu.vn.selfalarm.model.Alarm;
import hcmute.edu.vn.selfalarm.util.OnToggleAlarmListener;

public class AlarmRecyclerViewAdapter extends RecyclerView.Adapter<AlarmViewHolder> {
    private List<Alarm> alarms;
    private OnToggleAlarmListener listener;
    private ItemAlarmBinding itemAlarmBinding;
    public AlarmRecyclerViewAdapter(OnToggleAlarmListener listener) {
        this.alarms = new ArrayList<Alarm>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        itemAlarmBinding=ItemAlarmBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new AlarmViewHolder(itemAlarmBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarms.get(position);
        holder.bind(alarm, listener);
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    public void setAlarms(List<Alarm> alarms) {
        this.alarms = alarms;
        notifyDataSetChanged();
    }
}
