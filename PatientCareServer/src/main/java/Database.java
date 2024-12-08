import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private static Connection connection;

    // Constructor to establish database connection
    public Database() {
        connectToDatabase();
    }

    // Method to establish a connection to MySQL database
    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://34.171.220.254/patient_care",
                    "wflor4",
                    "pass"
            );
            System.out.println("Database connected successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the database.");
        }
    }

    // *------------------------Security Functions---------------------*//

    // Check if the credentials are valid
    public static boolean checkCredentials(String email, String password) {
        String query = "SELECT * FROM Users WHERE email = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();

            // If a record is found, the credentials are valid
            if (resultSet.next()) {
                System.out.println("Login successful: User found in the database.");
                return true;
            } else {
                System.out.println("Login failed: Invalid email or password.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if email exists in database
    public static boolean checkEmailCredentials(String email) {
        String query = "SELECT * FROM Users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();

            // If a record is found, the credentials are valid
            if (resultSet.next()) {
                System.out.println("User Email Found");
                return true;
            } else {
                System.out.println("User Email NOT Found");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get user ID by email
    public Integer getUserIdByEmail(String email) {
        String query = "SELECT id FROM Users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            } else {
                System.out.println("No user found with the given email.");
                return null; // Return null if no user is found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null; // Return null in case of an exception
        }
    }

    // Retrieve the full name of a patient by patient ID
    public String getUserFullNameById(int patientId) {
        String query = "SELECT CONCAT(first_name, ' ', last_name) AS full_name " +
                "FROM Users " +
                "WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, patientId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("full_name"); // Return the concatenated full name
            } else {
                System.out.println("No user found with the given ID.");
                return null; // Return null if no user is found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null; // Return null in case of an exception
        }
    }


    // *------------------------Message Functions---------------------*//
    // Log a message in the database
    public void logMessage(String sender, String receiver, String content) {
        System.out.println("messages I guess");
    }


    // Send a message from one user to another
    public void sendMessage(int senderId, int receiverId, String content) {
        String query = "INSERT INTO Messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, senderId);
            statement.setInt(2, receiverId);
            statement.setString(3, content);
            statement.executeUpdate();
            System.out.println("Message sent successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to send message.");
            e.printStackTrace();
        }
    }

    // Retrieve message history between two users
    public List<String> getMessageHistory(int userId1, int userId2) {
        String query = "SELECT sender_id, receiver_id, content, sent_at " +
                "FROM Messages WHERE (sender_id = ? AND receiver_id = ?) " +
                "OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY sent_at ASC";
        List<String> messageHistory = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId1);
            statement.setInt(2, userId2);
            statement.setInt(3, userId2);
            statement.setInt(4, userId1);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int senderId = resultSet.getInt("sender_id");
                String content = resultSet.getString("content");
                Timestamp sentAt = resultSet.getTimestamp("sent_at");
                messageHistory.add("[" + sentAt + "] User " + senderId + ": " + content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messageHistory;
    }

    // Log all messages for analysis or record-keeping
    public void logMessages() {
        String query = "SELECT sender_id, receiver_id, content, sent_at FROM Messages";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                int senderId = resultSet.getInt("sender_id");
                int receiverId = resultSet.getInt("receiver_id");
                String content = resultSet.getString("content");
                Timestamp sentAt = resultSet.getTimestamp("sent_at");
                System.out.println("[" + sentAt + "] Sender: " + senderId +
                        ", Receiver: " + receiverId + ", Message: " + content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // *------------------------Manipulating Functions---------------------*//
    // Create: Add a new user
    public void addUser(String email, String password, String firstName, String lastName, String role) {
        String query = "INSERT INTO Users (email, password, first_name, last_name, role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            statement.setString(2, password);
            statement.setString(3, firstName);
            statement.setString(4, lastName);
            statement.setString(5, role);
            statement.executeUpdate();
            System.out.println("User added successfully.");
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // SQL state for unique constraint violation in MySQL
                System.out.println("Duplicate email. User not added.");
            } else {
                System.out.println("Failed to add user.");
                e.printStackTrace();
            }
        }
    }


    // Create: Add a new doctor
    public void addDoctor(int userId, String specialty) {
        String query = "INSERT INTO Doctors (user_id, specialty) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setString(2, specialty);
            statement.executeUpdate();
            System.out.println("Doctor added successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to add doctor.");
            e.printStackTrace();
        }
    }

    // Create: Add a new patient
    public void addPatient(int userId, Date dateOfBirth) {
        String query = "INSERT INTO Patients (user_id, date_of_birth) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setDate(2, dateOfBirth);
            statement.executeUpdate();
            System.out.println("Patient added successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to add patient.");
            e.printStackTrace();
        }
    }

    // Create: Assign a doctor to a patient
    public void assignDoctorToPatient(int doctorId, int patientId) {
        String query = "INSERT INTO DoctorPatient (doctor_id, patient_id, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            statement.setInt(2, patientId);
            statement.executeUpdate();
            System.out.println("Doctor assigned to patient successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to assign doctor to patient.");
            e.printStackTrace();
        }
    }

    // Check if a doctor-patient relationship already exists
    public boolean doctorPatientExists(int doctorId, int patientId) {
        String query = "SELECT * FROM DoctorPatient WHERE doctor_id = ? AND patient_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            statement.setInt(2, patientId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                System.out.println("Doctor-patient relationship already exists.");
                return true; // Relationship already exists
            }
        } catch (SQLException e) {
            System.out.println("Error checking doctor-patient relationship.");
            e.printStackTrace();
        }
        return false; // Relationship does not exist
    }

    // Retrieve: All patient IDs for a given doctor
    public List<Integer> getPatientIdsByDoctor(int doctorId) {
        String query = "SELECT patient_id FROM DoctorPatient WHERE doctor_id = ?";
        List<Integer> patientIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                patientIds.add(resultSet.getInt("patient_id"));
            }
            System.out.println("Patient IDs retrieved successfully for doctor ID: " + doctorId);
        } catch (SQLException e) {
            System.out.println("Failed to retrieve patient IDs for doctor ID: " + doctorId);
            e.printStackTrace();
        }
        return patientIds;
    }

    // Retrieve: All doctor IDs for a given patient
    public List<Integer> getDoctorIdsByPatient(int patientId) {
        String query = "SELECT doctor_id FROM DoctorPatient WHERE patient_id = ?";
        List<Integer> doctorIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, patientId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                doctorIds.add(resultSet.getInt("doctor_id"));
            }
            System.out.println("Doctor IDs retrieved successfully for patient ID: " + patientId);
        } catch (SQLException e) {
            System.out.println("Failed to retrieve doctor IDs for patient ID: " + patientId);
            e.printStackTrace();
        }
        return doctorIds;
    }


    // Create: Schedule an appointment
    public void scheduleAppointment(int doctorId, int patientId, Timestamp appointmentDate, String status, String notes) {
        String query = "INSERT INTO Appointments (doctor_id, patient_id, appointment_date, status, notes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            statement.setInt(2, patientId);
            statement.setTimestamp(3, appointmentDate);
            statement.setString(4, status);
            statement.setString(5, notes);
            statement.executeUpdate();
            System.out.println("Appointment scheduled successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to schedule appointment.");
            e.printStackTrace();
        }
    }

    // Create: Set doctor schedule
    public void setDoctorSchedule(int doctorId, String dayOfWeek, Time startTime, Time endTime) {
        String query = "INSERT INTO Schedules (doctor_id, day_of_week, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            statement.setString(2, dayOfWeek);
            statement.setTime(3, startTime);
            statement.setTime(4, endTime);
            statement.executeUpdate();
            System.out.println("Schedule set successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to set schedule.");
            e.printStackTrace();
        }
    }

    public List<String> getAppointmentDatesByDoctorId(int doctorId) {
        String query = "SELECT appointment_date FROM Appointments WHERE doctor_id = ?";
        List<String> appointmentDates = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Timestamp appointmentDate = resultSet.getTimestamp("appointment_date");
                appointmentDates.add(appointmentDate.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return appointmentDates;
    }

    public Map<Integer, String> getAppointmentsByDoctorId(int doctorId) {
        String query = "SELECT id, appointment_date FROM Appointments WHERE doctor_id = ?";
        Map<Integer, String> appointments = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int appointmentId = resultSet.getInt("id");
                Timestamp appointmentDate = resultSet.getTimestamp("appointment_date");

                // Add the appointment to the map with ID as the key and date as the value
                appointments.put(appointmentId, appointmentDate.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return appointments;
    }

    public Map<Integer, String> getAppointmentsByPatientId(int patientId) {
        String query = "SELECT id, appointment_date FROM Appointments WHERE patient_id = ?";
        Map<Integer, String> appointments = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, patientId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int appointmentId = resultSet.getInt("id");
                Timestamp appointmentDate = resultSet.getTimestamp("appointment_date");

                // Add the appointment to the map with ID as the key and date as the value
                appointments.put(appointmentId, appointmentDate.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return appointments;
    }

    // Function to retrieve appointment details by appointmentID
    public Map<String, Object> getAppointmentDetailsById(int appointmentId) {
        String query = "SELECT patient_id, appointment_date, status, notes FROM Appointments WHERE id = ?";
        Map<String, Object> appointmentDetails = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, appointmentId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                appointmentDetails.put("patient_id", resultSet.getInt("patient_id"));
                appointmentDetails.put("appointment_date", resultSet.getTimestamp("appointment_date"));
                appointmentDetails.put("status", resultSet.getString("status"));
                appointmentDetails.put("notes", resultSet.getString("notes"));
            } else {
                System.out.println("No appointment found with the given ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return appointmentDetails;
    }

    // Function to retrieve appointment details by appointmentID
    public Map<String, Object> getAppointmentDetailsByIdPatient(int appointmentId) {
        String query = "SELECT doctor_id, patient_id, appointment_date, status, notes FROM Appointments WHERE id = ?";
        Map<String, Object> appointmentDetails = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, appointmentId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                appointmentDetails.put("doctor_id", resultSet.getInt("doctor_id"));
                appointmentDetails.put("appointment_date", resultSet.getTimestamp("appointment_date"));
                appointmentDetails.put("status", resultSet.getString("status"));
                appointmentDetails.put("notes", resultSet.getString("notes"));
            } else {
                System.out.println("No appointment found with the given ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return appointmentDetails;
    }


    // Retrieve: Get a user by email
    public User getUser(String email) {
        String query = "SELECT * FROM Users WHERE email = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) { // Check if a user with the email exists
                int id = resultSet.getInt("id");
                String password = resultSet.getString("password");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String role = resultSet.getString("role");
//                Timestamp createdAt = resultSet.getTimestamp("created_at");
//                Timestamp updatedAt = resultSet.getTimestamp("updated_at");

                // Map to a User object
                return new User(email, firstName, lastName, role, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if no user is found or an exception occurs
    }


    // *------------------------Getter Functions---------------------*//
    // Retrieve: Get all appointments for a doctor by doctor ID
    public List<ResultSet> getDoctorAppointments(int doctorId) {
        String query = "SELECT * FROM Appointments WHERE doctor_id = ?";
        List<ResultSet> appointments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                appointments.add(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return appointments;
    }

    // Retrieve: Get all patients for a doctor by doctor ID (through appointments)
    public List<ResultSet> getDoctorPatients(int doctorId) {
        String query = "SELECT Patients.* FROM Patients " +
                "JOIN Appointments ON Patients.id = Appointments.patient_id " +
                "WHERE Appointments.doctor_id = ?";
        List<ResultSet> patients = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, doctorId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                patients.add(resultSet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    // Update: Update appointment status
    public void updateAppointmentStatus(int appointmentId, String newStatus) {
        String query = "UPDATE Appointments SET status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, newStatus);
            statement.setInt(2, appointmentId);
            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Appointment status updated successfully.");
            } else {
                System.out.println("Appointment not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete: Cancel an appointment
    public void cancelAppointment(int appointmentId) {
        String query = "DELETE FROM Appointments WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, appointmentId);
            int rowsDeleted = statement.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Appointment canceled successfully.");
            } else {
                System.out.println("Appointment not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Utility: List all users
    public void listUsers() {
        String query = "SELECT * FROM Users";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                System.out.println(
                        resultSet.getString("email") + " " +
                                resultSet.getString("password") + " " +
                                resultSet.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add this method to the Database class
    public void addPDF(int doctorId, int patientId, File pdfFile) {
        String query = "INSERT INTO PDFs (doctor_id, patient_id, pdf_file) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query);
             FileInputStream fileInputStream = new FileInputStream(pdfFile)) {

            // Set the parameters for the query
            statement.setInt(1, doctorId);
            statement.setInt(2, patientId);
            statement.setBinaryStream(3, fileInputStream, (int) pdfFile.length());

            // Execute the query
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("PDF added successfully to the database.");
            } else {
                System.out.println("Failed to add PDF to the database.");
            }

        } catch (SQLException e) {
            System.out.println("SQL Exception: Unable to add PDF.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO Exception: Unable to read PDF file.");
            e.printStackTrace();
        }
    }

    public Map<Integer, byte[]> getPDFsByPatientID(int patientId) {
        String query = "SELECT pdf_id, pdf_file FROM PDFs WHERE patient_id = ?";
        Map<Integer, byte[]> pdfMap = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, patientId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int pdfId = resultSet.getInt("pdf_id");
                byte[] pdfContent = resultSet.getBytes("pdf_file");
                pdfMap.put(pdfId, pdfContent); // Store pdf_id as key and pdf_file as value
            }

            if (pdfMap.isEmpty()) {
                System.out.println("No PDFs found for the given patient ID.");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving PDFs from the database.");
            e.printStackTrace();
        }

        return pdfMap;
    }

    public String getPDFCreatedAtById(int pdfId) {
        String query = "SELECT created_at FROM PDFs WHERE pdf_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, pdfId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getTimestamp("created_at").toString(); // Return the created_at date as a String
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving created_at for PDF ID: " + pdfId);
            e.printStackTrace();
        }
        return null; // Return null if not found
    }

    public List<schedule> getAllSchedules() {
        String query = "SELECT Appointments.id AS appointment_id, " +
                "Appointments.appointment_date, Appointments.status, Appointments.notes, " +
                "Doctors.id AS doctor_id, Doctors.specialty, " +
                "Users.first_name AS doctor_first_name, Users.last_name AS doctor_last_name, " +
                "Patients.id AS patient_id, Patients.date_of_birth, " +
                "PatientUsers.first_name AS patient_first_name, PatientUsers.last_name AS patient_last_name " +
                "FROM Appointments " +
                "JOIN Doctors ON Appointments.doctor_id = Doctors.id " +
                "JOIN Users ON Doctors.user_id = Users.id " +
                "JOIN Patients ON Appointments.patient_id = Patients.id " +
                "JOIN Users AS PatientUsers ON Patients.user_id = PatientUsers.id";

        List<schedule> schedules = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String doctorName = resultSet.getString("doctor_first_name") + " " + resultSet.getString("doctor_last_name");
                String patientName = resultSet.getString("patient_first_name") + " " + resultSet.getString("patient_last_name");
                String date = resultSet.getTimestamp("appointment_date").toString();
                String time = ""; // Add logic to extract time if needed
                String notes = resultSet.getString("notes");
                String status = resultSet.getString("status");

                schedule schedule = new schedule(doctorName, patientName, date, time, notes, status);
                schedules.add(schedule);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return schedules;
    }


}
