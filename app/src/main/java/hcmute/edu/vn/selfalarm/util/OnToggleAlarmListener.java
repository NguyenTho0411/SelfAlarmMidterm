package hcmute.edu.vn.selfalarm.util;

import android.view.View;

import hcmute.edu.vn.selfalarm.model.Alarm;


public interface OnToggleAlarmListener {
    void onToggle(Alarm alarm);
    void onDelete(Alarm alarm);
    void onItemClick(Alarm alarm,View view);
}
