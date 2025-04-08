package hcmute.edu.vn.selfalarm.model;

public class Task {
    private int id;
    private String title;
    private String description;
    private long dueTime;

    public Task(int id, String title, String description, long dueTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueTime = dueTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDueTime() {
        return dueTime;
    }

    public void setDueTime(long dueTime) {
        this.dueTime = dueTime;
    }
}
