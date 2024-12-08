import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class schedule implements Serializable {
    private static final long serialVersionUID = 1L; // For serialization compatibility

    private String doctorName;
    private String patientName;
    private String date;
    private String time;
    private String notes;
    private String status; // e.g., "scheduled", "completed", "canceled"
    private List<schedule> relatedSchedules; // Holds related schedules

    // Constructor
    public schedule(String doctorName, String patientName, String date, String time, String notes, String status) {
        this.doctorName = doctorName;
        this.patientName = patientName;
        this.date = date;
        this.time = time;
        this.notes = notes;
        this.status = status;
        this.relatedSchedules = new ArrayList<>(); // Initialize the list
    }

    public schedule(List<schedule> schedules){
        this.doctorName = null;
        this.patientName = null;
        this.date = null;
        this.time = null;
        this.notes = null;
        this.status = null;
        this.relatedSchedules = schedules; // Initialize the list
    }

    // Getters
    public String getDoctorName() {
        return doctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public List<schedule> getRelatedSchedules() {
        return relatedSchedules;
    }

    // Setters
    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRelatedSchedules(List<schedule> relatedSchedules) {
        this.relatedSchedules = relatedSchedules;
    }

    // Add a schedule to the relatedSchedules list
    public void addSchedule(schedule schedule) {
        relatedSchedules.add(schedule);
    }

    // To String (for debugging purposes)
    @Override
    public String toString() {
        return "Schedule{" +
                "doctorName='" + doctorName + '\'' +
                ", patientName='" + patientName + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", notes='" + notes + '\'' +
                ", status='" + status + '\'' +
                ", relatedSchedules=" + relatedSchedules +
                '}';
    }
}
