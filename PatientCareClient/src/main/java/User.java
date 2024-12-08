import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    private static final long serialVersionUID = 42L;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String Role; // 0 - patient, 1 - healthcare worker
    private int protocol;
    ArrayList<String> designatedPatients = new ArrayList<>(); // Create a dynamic array

    public User(String email, String firstName, String lastName, String role, String password){
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.Role = role;
        this.password = password;
    }

    public User(String email, String password, int protocol){
        this.email = email;
        this.password = password;
        this.protocol = protocol;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole(){
        return Role;
    }

    public int getProtocol(){
        return this.protocol;
    }

    public void setProtocol(int Protocol){
        this.protocol = Protocol;
    }

    // Methods for Healthcare Worker ONLY, for designated patients

    // Add a patient email to the list
    public void addPatient(String patientEmail) {
        if (!designatedPatients.contains(patientEmail)) {
            designatedPatients.add(patientEmail);
        }
    }

    // Remove a patient email from the list
    public void removePatient(String patientEmail) {
        designatedPatients.remove(patientEmail);
    }

    // Get the list of designated patients
    public ArrayList<String> getDesignatedPatients() {
        return designatedPatients;
    }

    // Check if a patient email is in the list
    public boolean hasPatient(String patientEmail) {
        return designatedPatients.contains(patientEmail);
    }

}
