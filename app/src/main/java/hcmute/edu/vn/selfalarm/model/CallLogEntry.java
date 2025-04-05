package hcmute.edu.vn.selfalarm.model;

public class CallLogEntry {
    private String number;
    private long date;
    private int duration;
    private int type;

    public CallLogEntry(String number, long date, int duration, int type) {
        this.number = number;
        this.date = date;
        this.duration = duration;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public long getDate() {
        return date;
    }

    public int getDuration() {
        return duration;
    }

    public int getType() {
        return type;
    }
} 