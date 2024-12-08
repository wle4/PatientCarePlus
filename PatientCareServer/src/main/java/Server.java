import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;

public class Server {
    private Database database;
    private Map<String, ClientThread> serverUsers = new HashMap<>(); // mapping usernames with their respective ClientThread

    TheServer server; // instance of the actual server
    private Consumer<Serializable> callback; // currently used to just print strings into the server GUI

    Server(Consumer<Serializable> call) {
        callback = call;
        database = new Database();
        server = new TheServer();
        server.start();
    }


    public class TheServer extends Thread {

        public void run() {

            try (ServerSocket mysocket = new ServerSocket(5555);) {

                System.out.println("Server is running!");


                while (true) { // tells us when a new client is trying to join
                    ClientThread c = new ClientThread(mysocket.accept());
                    callback.accept("User is attempting to connect...");
                    c.start();
                }

            }//end of try
            catch (Exception e) {
                callback.accept("Server socket did not launch");
            }

        }//end of while
    }

    // updates the list of online users
    public ArrayList<String> updateCurrentUsers() {
        return new ArrayList<>(serverUsers.keySet());
    }

    class ClientThread extends Thread {

        Socket connection; // socket connection between Server and Client
        ObjectInputStream in; // socket input stream
        ObjectOutputStream out; // socket output stream

        ClientThread(Socket s) {
            this.connection = s;
        }

        // anything related with the client-server interactions will be handled run()
        public void run() {
            try {
                //initializes socket information
                in = new ObjectInputStream(connection.getInputStream());
                out = new ObjectOutputStream(connection.getOutputStream());
                connection.setTcpNoDelay(true);

                String username; // individual client's username is stored here


                while (true) { // THIS WHILE LOOP IS SPECIFICALLY FOR JUST THE LOGIN SCREEN, continues to loop here until the client thread successfully logs in
                    try {
                        User loginCredentials = (User) in.readObject(); // unpacks incoming User objects

                        // Creating new user
                        if(loginCredentials.getProtocol() == 0){
                            System.out.println("New User info received");
                            User newUser = loginCredentials; // Cast to User

                            // Save the user to the database
                            Database database1 = new Database();
                            database1.addUser(newUser.getEmail(), newUser.getPassword(),
                                    newUser.getFirstName(), newUser.getLastName(),
                                    newUser.getRole());

                            // Send confirmation back to the client
//                            out.writeObject("User creation successful");
                            callback.accept("User created: " + newUser.getFirstName() + " " + newUser.getLastName());
                        }
                        else if(!database.checkCredentials(loginCredentials.getEmail(), loginCredentials.getPassword())){
                            out.writeObject("Incorrect credentials"); // sends null back if their login info is incorrect
                            callback.accept("User entered incorrect credentials.");
                        }
                        // User login credentials
                        else{
                            User checkingUser = loginCredentials;
                            System.out.println(loginCredentials.getFirstName());
                            System.out.println(database.getUser(checkingUser.getEmail()));
                            checkingUser = database.getUser(checkingUser.getEmail());
                            out.writeObject(checkingUser); // sends back a message saying that the login info is valid
                            username = checkingUser.getFirstName() + " " + checkingUser.getLastName();

//                            System.out.println("ID CHECK: " + database.getUserIdByEmail(loginCredentials.getEmail()));
                            break; // login successful, break out of login loop
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        callback.accept("User failed to join");
                    }
                }

                synchronized (serverUsers) { // // ensures thread safety when disconnecting an existing client from the serverUsers
                    serverUsers.put(username, this); // remove user from user list
                }

                callback.accept(username + " has joined"); // prints out on the server log that a new user has successfully logged in

                while (true) { // Loops for checking new Message objects coming from the client
                    try {
                        Object obj = in.readObject(); // Read the incoming object
                        // Instance of USER
                        if (obj instanceof User) { // Check if the object is a User
                            System.out.println("New User info received");
                            User newUser = (User) obj; // Cast to User

                            // Save the user to the database
                            Database database1 = new Database();
                            database1.addUser(newUser.getEmail(), newUser.getPassword(),
                                    newUser.getFirstName(), newUser.getLastName(),
                                    newUser.getRole());

                            // Send confirmation back to the client
//                            out.writeObject("User creation successful");
                            callback.accept("User created: " + newUser.getFirstName() + " " + newUser.getLastName());
                        }
                        // Instance of STRING
                        else if (obj instanceof String) { // Check if it’s a String
                            String msg = (String) obj;

                            // *------------------------Patient-Doctor Assignments---------------------*//

                            // Adding patient email to healthcare worker
                            if (msg.startsWith("Adding Patient Email")) {
                                System.out.println(msg);
                                String[] parts = msg.split(" "); // Split by spaces
                                String patientEmail = parts[3]; // Extract patient email
                                String doctorEmail = parts[5];  // Extract doctor's name\

                                callback.accept("Processing request to add patient email: " + patientEmail + " to doctor: " + doctorEmail);

                                // check credentials for email
                                // patient email not found
                                if (!database.checkEmailCredentials(patientEmail)) {
                                    // Email invalid
                                    callback.accept("Patient email not found");
                                    out.writeObject("Patient email not found: " + patientEmail + " to doctor: " + doctorEmail);
                                }
                                // patient email found
                                else {
                                    //
                                    callback.accept("Patient email found");
                                    // assign doctor to patient in database using ID
                                    // patient email not already added
                                    if (!database.doctorPatientExists(database.getUserIdByEmail(doctorEmail), database.getUserIdByEmail(patientEmail))) {
                                        out.writeObject("Patient email found: " + patientEmail + " to doctor: " + doctorEmail);
                                        System.out.println("ID CHECK: " + database.getUserIdByEmail(patientEmail));
                                        database.assignDoctorToPatient(database.getUserIdByEmail(doctorEmail), database.getUserIdByEmail(patientEmail));
                                    }
                                    // patient email already added
                                    else {
                                        out.writeObject("Patient email already added: " + patientEmail + " to doctor: " + doctorEmail);
                                    }
                                }

                            }

                            // *------------------------Patient Functions---------------------*//

                            // Retrieving all Patient's ID's
                            else if (msg.startsWith("Patient: Retrieve all ID's ")) {
                                // Doctor info map <Name, ID>
                                Map<String, Integer> patientInfo = new HashMap<>();
                                String email = msg.substring("Patient: Retrieve all ID's ".length());

                                // Retrieve doctor IDs
                                List <Integer> doctorIds = database.getDoctorIdsByPatient(database.getUserIdByEmail(email));

                                // Iterate over patient IDs and fetch full names
                                for (Integer doctorId : doctorIds) {
                                    String patientName = database.getUserFullNameById(doctorId);
                                    System.out.println("RETRIEVED NAME " + patientName);
                                    patientInfo.put(patientName, doctorId);
                                }

                                System.out.println("Patient Info Map: " + patientInfo);
                                out.writeObject(patientInfo); // Send the map as an object
                            }
                            // Retrieving all Patient's ID's (appointments)
                            else if (msg.startsWith("Patient: Set up appointments ")) {
                                // Doctor info map <Name, ID>
                                Map<String, Integer> patientInfo = new HashMap<>();
                                String email = msg.substring("Patient: Set up appointments ".length());

                                // Retrieve doctor IDs
                                List <Integer> doctorIds = database.getDoctorIdsByPatient(database.getUserIdByEmail(email));

                                // Iterate over patient IDs and fetch full names
                                for (Integer doctorId : doctorIds) {
                                    String patientName = database.getUserFullNameById(doctorId);
                                    System.out.println("RETRIEVED NAME " + patientName);
                                    patientInfo.put(patientName, doctorId);
                                }

                                System.out.println("Patient Info Map: " + patientInfo);
                                out.writeObject(patientInfo); // Send the map as an object
                            }
                            else if (msg.startsWith("Get doctor time slots ")) {
                                String doctorIDString = msg.substring("Get doctor time slots ".length());
                                int doctorID = Integer.parseInt(doctorIDString);
                                System.out.println(database.getAppointmentDatesByDoctorId(doctorID));
                                out.writeObject(database.getAppointmentDatesByDoctorId(doctorID));
                            }
                            else if (msg.startsWith("Sending appointment info to: ")) {
                                System.out.println(msg);

                                String[] parts = msg.split(" ");
                                int doctorID = Integer.parseInt(parts[4]);
                                String date = parts[6];
                                String time = parts[8];

                                // Find the starting index of the notes
                                int notesStartIndex = msg.indexOf("notes:") + "notes:".length();
                                int emailStartIndex = msg.indexOf("from:");
                                String notes = msg.substring(notesStartIndex, emailStartIndex).trim(); // Extract the notes content
                                String email = msg.substring(emailStartIndex + "from:".length()).trim(); // Extract the email

                                // Make in Timestamp format
                                String dateTimeString = date + " " + time + ":00";

                                System.out.println(dateTimeString);

                                // Fix timestamp issue
                                database.scheduleAppointment(doctorID, database.getUserIdByEmail(email), Timestamp.valueOf(dateTimeString), "scheduled", notes);
                            }
                            else if (msg.startsWith("Patient retrieve appointments ")) {
                                String email = msg.substring("Patient retrieve appointments ".length());
                                int id = database.getUserIdByEmail(email);
                                out.writeObject(database.getAppointmentsByPatientId(id));
                            }
                            else if (msg.startsWith("Patient Finding appointment details: ")) {
                                int appointmentID = Integer.parseInt(msg.substring("Patient Finding appointment details: ".length()));
                                Map<String, Object> appointmentDetails = database.getAppointmentDetailsByIdPatient(appointmentID);

                                System.out.println(appointmentDetails);

                                int doctor_id = (int) appointmentDetails.get("doctor_id");
                                String patientName = database.getUserFullNameById(doctor_id);

                                // Create a new map with patientName instead of patientId
                                Map<String, Object> updatedAppointmentDetails = new HashMap<>(appointmentDetails);
                                updatedAppointmentDetails.put("patient_name", patientName);
                                updatedAppointmentDetails.remove("doctor_id");

                                System.out.println("Updated Appointment Details: " + updatedAppointmentDetails);

                                out.writeObject(updatedAppointmentDetails);

                            }
                            else if (msg.startsWith("Retrieve medical records: ")) {
                                String email = msg.substring("Retrieve medical records: ".length());
                                int id = database.getUserIdByEmail(email);

                                Map<Integer, byte[]> pdfMap = database.getPDFsByPatientID(id);

                                Map<String, File> dateFileMap = new HashMap<>();

                                // Iterate over each entry in pdfMap
                                for (Map.Entry<Integer, byte[]> entry : pdfMap.entrySet()) {
                                    int pdfId = entry.getKey();
                                    byte[] pdfContent = entry.getValue();

                                    // Fetch the date corresponding to this PDF ID
                                    String createdAt = database.getPDFCreatedAtById(pdfId);

                                    if (createdAt != null) {
                                        try {
                                            // Sanitize the date string to remove invalid characters
                                            String sanitizedDate = createdAt.replaceAll("[\\\\/:*?\"<>|]", "_");

                                            // Create a temporary file for this PDF
                                            File tempFile = File.createTempFile("medical_record_" + sanitizedDate, ".pdf");
                                            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                                fos.write(pdfContent); // Write the byte array to the file
                                            }
                                            dateFileMap.put(createdAt, tempFile); // Map original date to the file
                                        } catch (IOException e) {
                                            e.printStackTrace(); // Handle any exceptions
                                        }
                                    }
                                }

                                // Process or print the date and file details
                                for (Map.Entry<String, File> entry : dateFileMap.entrySet()) {
                                    String date = entry.getKey();
                                    File file = entry.getValue();
                                    System.out.println("Date: " + date + ", File path: " + file.getAbsolutePath());
                                }
                                System.out.println(dateFileMap);

                                // Send the map of dates to files back to the client
                                out.writeObject(dateFileMap);
                            }

                            // *------------------------Doctor Functions---------------------*//

                            // Retrieving all Doctor's ID's
                            else if (msg.startsWith("Doctor: Retrieve all ID's ")) {
                                // Patient info map <Name, ID>
                                Map<String, Integer> patientInfo = new HashMap<>();
                                String email = msg.substring("Doctor: Retrieve all ID's ".length());

                                // Retrieve patient IDs
                                List<Integer> patientIds = database.getPatientIdsByDoctor(database.getUserIdByEmail(email));
//                                System.out.println("Retrieved Patient IDs: " + patientIds);

                                // Iterate over patient IDs and fetch full names
                                for (Integer patientId : patientIds) {
                                    String patientName = database.getUserFullNameById(patientId);
//                                    System.out.println("RETRIEVED NAME " + patientName);
                                    patientInfo.put(patientName, patientId);
                                }

                                System.out.println("Patient Info Map: " + patientInfo);
                                out.writeObject(patientInfo); // Send the map as an object
                            }
                            // Sending client ID
                            else if (msg.startsWith("Retrieve Client ID")) {
                                String[] parts = msg.split(": ", 2); // Split at ": "
                                String email = parts[1]; // The part after "Retrieve Client ID: "
                                System.out.println("Extracted Email: " + email);
                                Integer id = database.getUserIdByEmail(email);
                                System.out.println("ID: " + id);
                                out.writeObject("ID: " + id);
                            }
                            // Sending message history
                            else if (msg.startsWith("Retrieve Messages for ID: ")) {
                                String[] parts = msg.split(":");
                                String[] ids = parts[1].trim().split(" ");
                                Integer senderID = Integer.parseInt(ids[0]);
                                Integer receiverID = Integer.parseInt(ids[1]);

                                // Database message history
                                List<String> messageHistory = database.getMessageHistory(senderID, receiverID);
                                // Message history being sent
                                List<String> sendMessageHistory = new ArrayList<>();

                                // Initialize and display parsed message history
                                for (String historyMessage : messageHistory) {
                                    // Example message: "[2024-12-01 04:05:54.0] User 30: I'd like an update on my diagnostics please!"
                                    int dateEndIndex = historyMessage.indexOf("]"); // Find the closing bracket
                                    String date = historyMessage.substring(1, dateEndIndex); // Extract the date

                                    int idStartIndex = historyMessage.indexOf("User ") + 5; // Find where "User " starts and add its length
                                    int idEndIndex = historyMessage.indexOf(":", idStartIndex); // Find the colon after the user ID
                                    String userId = historyMessage.substring(idStartIndex, idEndIndex); // Extract the user ID

                                    String message = historyMessage.substring(idEndIndex + 2); // Extract the message (skip the ": " after ID)



                                    // Print the parsed components (or process them as needed)
                                    System.out.println("Date: " + date);
                                    System.out.println("User ID: " + userId);
                                    System.out.println("Message: " + message);

                                    String fullname = database.getUserFullNameById(Integer.parseInt(userId));

                                    // New message
                                    String reformattedMessage = fullname + ": " + message;

                                    sendMessageHistory.add(reformattedMessage);

                                }

                                out.writeObject(sendMessageHistory);

                            }
                            else if (msg.startsWith("Retrieve ID by email: ")) {
                                String email = msg.substring("Retrieve ID by email: ".length()).trim();
                                out.writeObject(String.valueOf(database.getUserIdByEmail(email)));
                            }
                            else if (msg.startsWith("Doctor retrieve appointments ")) {
                                String email = msg.substring("Doctor retrieve appointments ".length());
                                int id = database.getUserIdByEmail(email);
                                out.writeObject(database.getAppointmentsByDoctorId(id));
                            }
                            else if (msg.startsWith("Doctor Finding appointment details: ")) {
                                int appointmentID = Integer.parseInt(msg.substring("Doctor Finding appointment details: ".length()));
                                Map<String, Object> appointmentDetails = database.getAppointmentDetailsById(appointmentID);

                                int patientId = (int) appointmentDetails.get("patient_id");
                                String patientName = database.getUserFullNameById(patientId);

                                // Create a new map with patientName instead of patientId
                                Map<String, Object> updatedAppointmentDetails = new HashMap<>(appointmentDetails);
                                updatedAppointmentDetails.put("patient_name", patientName);
                                updatedAppointmentDetails.remove("patient_id");

                                System.out.println("Updated Appointment Details: " + updatedAppointmentDetails);

                                out.writeObject(updatedAppointmentDetails);

                            }

                        }
                        // *------------------------Messaging---------------------*//
                        // Instance of MESSAGE
                        else if (obj instanceof Message) { // Check if it's a message object
                            Message msg = (Message) obj;
                            System.out.println("Received message from " + msg.getSender() + " to " + msg.getReceiver() + ": " + msg.getMsg() + " SENT with receiver ID = " + msg.getReceiverId() + " with sender ID = " + msg.getSenderId());

                            database.sendMessage(msg.getSenderId(), msg.getReceiverId(), msg.getMsg());
                        }
                        // *------------------------PDF Files---------------------*//
                        // Instance of PDFTransferObject
                        else if (obj instanceof PDFTransferObject) {
                            PDFTransferObject pdfTransfer = (PDFTransferObject) obj;

                            // Extract information from the PDFTransferObject
                            int doctorId = pdfTransfer.getSenderId();
                            int patientId = pdfTransfer.getReceiverId();
                            byte[] pdfData = pdfTransfer.getPdfData();
                            String fileName = pdfTransfer.getFileName();

                            System.out.println("Received PDF from senderId: " + doctorId + " to receiverId: " + patientId + " with fileName: " + fileName);

                            // Save the PDF data to a temporary file
                            File tempFile = new File("temp_" + fileName);
                            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                fos.write(pdfData);
                                fos.flush();

                                // Call the database method to add the PDF
                                database.addPDF(doctorId, patientId, tempFile);

                            } catch (IOException e) {
                                System.out.println("Error saving or processing PDF data.");
                                e.printStackTrace();
                            } catch (Exception e) {
                                System.out.println("Error adding PDF to database.");
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) { // If a client disconnects
                        e.printStackTrace();
                        break;
                    }
                }


                callback.accept(username + " has disconnected"); // prints that this user has disconnected

                synchronized (serverUsers) { // // ensures thread safety when disconnecting an existing client from the serverUsers
                    serverUsers.remove(username); // remove user from user list
                }

                in.close();
                out.close();
                connection.close(); // close client socket
            } catch (Exception e) {
                //implement error message
                e.printStackTrace();
                callback.accept("User failed to join");
            }

        }//end of run

    }//end of client thread

//    public void saveDatabase(){
//        database.saveDatabase();
//    }
}

