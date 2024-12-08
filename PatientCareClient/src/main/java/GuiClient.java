import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.scene.layout.BorderPane;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GuiClient extends Application{

	HashMap<String, Scene> sceneMap; // maps all scenes

	Client clientConnection; // connection between the client and server

	ListView<String> listItems; // lists out all messages from all clients
	ListView<Button> listClientNames; // stores all connected clients as a button

	Label messageLabel;

	private Stage primaryStage;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		listItems = new ListView<>();
		listClientNames = new ListView<>();

		this.primaryStage = primaryStage;

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>(){ // completely closes program when window is closed
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		// create scenes and store in map
		sceneMap = new HashMap<String, Scene>();

		// initialize and store the scenes, ANY NEW SCENES SHOULD BE INITIALIZED AND ADDED HERE
		sceneMap.put("loginScene", createLoginScene(primaryStage));
		sceneMap.put("createUserScene", createNewUserScene(primaryStage));
//		sceneMap.put("patientRequests", createRequestAppointmentScene(primaryStage));
		sceneMap.put("addPatientToDoctor", addPatientToDoctor(null));
		sceneMap.put("viewEachRequestHealthcare", viewEachRequestHealthcare(null)); // pass on string appointment ID(?)


		// clients will start in the login screen, set settings for the Stage
		primaryStage.setWidth(400);
		primaryStage.setHeight(700);
		primaryStage.setResizable(false);
		//400x700

		primaryStage.setScene(sceneMap.get("loginScene"));
		primaryStage.setTitle("PatientCare+");
		primaryStage.show();

		//this is the connection between the GUI and client.java back-end
		clientConnection = new Client(data->{
				Platform.runLater( ()->{
					// read in USER objects
					if(data instanceof User) {
						// this handles the login screen, switching to the correct scene based on role
						System.out.println("Log-in success");
						System.out.println(clientConnection.client.getRole() == "patient");
						System.out.println(clientConnection.client.getRole());
						// switch to the patient UI
						if ("patient".equals(clientConnection.client.getRole())) {
							Scene mainMenu = buildPatientUI(clientConnection.clientName);
							switchWithProgressAndFade(primaryStage, mainMenu);
							sceneMap.put("patientMainMenu", mainMenu);
							System.out.println("Client");
						}
						// switch to the healthcare worker UI
						else if("doctor".equals(clientConnection.client.getRole())){
							Scene mainMenu = buildHealthCareWorkerUI(clientConnection.clientName);
							switchWithProgressAndFade(primaryStage, mainMenu);
							sceneMap.put("doctorMainMenu", mainMenu);
							System.out.println("Healthcare Worker");
						}
					}
					// If not a USER object
					else{
						messageLabel.setText("Error signing in. Please try again");
					}

				});
		});
		//start client connection to the server
		clientConnection.start();
	}

	// Function to create login scene and add to the sceneMap
	private Scene createLoginScene(Stage primaryStage) {
		Label headerLabel = new Label("PatientCare+");
		headerLabel.getStyleClass().add("header-label");

		TextField usernameField = new TextField();
		usernameField.setPromptText("Email");
		usernameField.getStyleClass().add("custom-text-field");

		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Password");
		passwordField.getStyleClass().add("custom-text-field");

		Button loginButton = new Button("Log In");
		loginButton.getStyleClass().add("login-button");

		Button createAccount = new Button("Create Account");
		createAccount.getStyleClass().add("create-account");

		messageLabel = new Label();
		messageLabel.getStyleClass().add("error-label");

		// Login button action
		loginButton.setOnAction(e -> {
			String email = usernameField.getText();
			String password = passwordField.getText();
			if (!email.isEmpty() && !password.isEmpty()) {
				try {
					clientConnection.sendLoginInfo(new User(email, password, 1));
				} catch (IOException ex) {
					messageLabel.setText("An error occurred. Please try again.");
					ex.printStackTrace();
				}
			} else {
				messageLabel.setText("Please enter both fields.");
			}
		});

		// Create Account button action
		createAccount.setOnAction(e -> {
			Scene createUserScene = createNewUserScene(primaryStage); // Pass primaryStage
			primaryStage.setScene(createUserScene);                  // Switch to the Create User scene
		});

		VBox layout = new VBox(20, headerLabel, usernameField, passwordField, loginButton, createAccount, messageLabel);
		layout.setPadding(new Insets(20));
		layout.setAlignment(Pos.CENTER);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

		return new Scene(layout);
	}

	private Scene createNewUserScene(Stage primaryStage) {
		Label headerLabel = new Label("Create New User");
		headerLabel.getStyleClass().add("header-label");

		// User Details
		Label nameLabel = new Label("Full Name:");
		TextField nameField = new TextField();
		nameField.setPromptText("Enter your full name");

		Label emailLabel = new Label("Email:");
		TextField emailField = new TextField();
		emailField.setPromptText("Enter your email");

		Label dobLabel = new Label("Date of Birth:");
		DatePicker dobPicker = new DatePicker();

		Label passwordLabel = new Label("Password:");
		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Enter your password");

		Label confirmPasswordLabel = new Label("Confirm Password:");
		PasswordField confirmPasswordField = new PasswordField();
		confirmPasswordField.setPromptText("Re-enter your password");

		// Role Selection
		Label roleLabel = new Label("Select Role:");
		ToggleGroup roleGroup = new ToggleGroup();
		RadioButton patientRadio = new RadioButton("Patient");
		patientRadio.setToggleGroup(roleGroup);
		patientRadio.setSelected(true); // Default selection

		RadioButton doctorRadio = new RadioButton("Doctor");
		doctorRadio.setToggleGroup(roleGroup);

		// Buttons
		Button createUserButton = new Button("Create User");
		Button backButton = new Button("Back");

		Label messageLabel = new Label();
		messageLabel.getStyleClass().add("error-label");

		// Layout
		GridPane gridPane = new GridPane();
		gridPane.setAlignment(Pos.CENTER);
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		gridPane.add(headerLabel, 0, 0, 2, 1); // Span across 2 columns
		gridPane.add(nameLabel, 0, 1);
		gridPane.add(nameField, 1, 1);
		gridPane.add(emailLabel, 0, 2);
		gridPane.add(emailField, 1, 2);
		gridPane.add(dobLabel, 0, 3);
		gridPane.add(dobPicker, 1, 3);
		gridPane.add(passwordLabel, 0, 4);
		gridPane.add(passwordField, 1, 4);
		gridPane.add(confirmPasswordLabel, 0, 5);
		gridPane.add(confirmPasswordField, 1, 5);
		gridPane.add(roleLabel, 0, 6);
		gridPane.add(patientRadio, 1, 6);
		gridPane.add(doctorRadio, 1, 7);
		gridPane.add(createUserButton, 1, 8);
		gridPane.add(backButton, 0, 8);
		gridPane.add(messageLabel, 1, 9);

		// Back Button Action
		backButton.setOnAction(e -> primaryStage.setScene(createLoginScene(primaryStage)));

		// Create User Button Action
		createUserButton.setOnAction(e -> {
			String name = nameField.getText();
			String email = emailField.getText();
			String password = passwordField.getText();
			String confirmPassword = confirmPasswordField.getText();
			String dob = (dobPicker.getValue() != null) ? dobPicker.getValue().toString() : "";
			String role = ((RadioButton) roleGroup.getSelectedToggle()).getText().toLowerCase(); // "patient" or "doctor"

			if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || dob.isEmpty()) {
				messageLabel.setText("All fields are required.");
			} else if (!password.equals(confirmPassword)) {
				messageLabel.setText("Passwords do not match.");
			} else {
				try {
					// Split the name into first and last
					String[] nameParts = name.split(" ");
					String firstName = nameParts.length > 0 ? nameParts[0] : ""; // First part
					String lastName = nameParts.length > 1 ? nameParts[1] : "";  // Second part (if exists)

					// Create a new User object
					User newUser = new User(email, firstName, lastName, role, confirmPassword);
					newUser.setProtocol(0);
					// Send the user data to the server
					clientConnection.sendLoginInfo(newUser);

					// Update UI to indicate success
					messageLabel.setText("User information sent to server!");
					nameField.clear();
					emailField.clear();
					passwordField.clear();
					confirmPasswordField.clear();
					dobPicker.setValue(null);
					patientRadio.setSelected(true); // Reset to default role

				} catch (IOException ex) {
					messageLabel.setText("Error sending user information to the server. Please try again.");
					ex.printStackTrace();
				}

			}
		});

		return new Scene(gridPane, 400, 500);
	}

	// function for creating the patient's interface
	private Scene buildPatientUI(String name) {
		Label welcomeLabel = new Label("Welcome " + name + "!");
		welcomeLabel.getStyleClass().add("header-label"); // Add the same class for styling

		// Creating scrollable buttons
		VBox buttonBox = new VBox(10); // Spacing between buttons
		buttonBox.setPadding(new Insets(10));
		buttonBox.setAlignment(Pos.CENTER);

		// Adding buttons specific for patients
		Button messageButton = new Button("Messages");
		messageButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(messageButton);

		Button appointmentButton = new Button("Request an Appointment");
		appointmentButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(appointmentButton);

		Button viewRequestsButton = new Button("View Appointments");
		viewRequestsButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(viewRequestsButton);

		Button medicalRecordsButton = new Button("Medical Records");
		medicalRecordsButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(medicalRecordsButton);


		messageButton.setOnAction(e->{
			clientConnection.sendString("Patient: Retrieve all ID's " + clientConnection.clientEmail);
			clientConnection.setOnResponseListener(response -> {
				if (response instanceof HashMap) {
					Platform.runLater(() -> {
						System.out.println(response);
						HashMap<String, Integer> receivedMap = (HashMap<String, Integer>) response; // Cast to HashMap
						System.out.println("Contact IDs added: " + receivedMap);
						loadContactsForMessaging(receivedMap);
					});
				}
			});
		});

		appointmentButton.setOnAction(e ->{
			clientConnection.sendString("Patient: Set up appointments " + clientConnection.clientEmail);

			clientConnection.setOnResponseListener(response -> {
				if (response instanceof HashMap) {
					Platform.runLater(() -> {
						System.out.println(response);
						HashMap<String, Integer> receivedMap = (HashMap<String, Integer>) response; // Cast to HashMap
						System.out.println("Contact IDs added: " + receivedMap);
						loadContactsForAppointments(receivedMap);
					});
				}
			});
		});

		viewRequestsButton.setOnAction(e ->{
			clientConnection.sendString("Patient retrieve appointments " + clientConnection.clientEmail);
			clientConnection.setOnResponseListener(response -> {
				if (response instanceof Map) {
					Platform.runLater(() -> {
						Map<Integer, String> receivedMap = (Map<Integer, String>) response; // Cast to Hashmap
						loadDoctorsAppointments(receivedMap);
					});
				}
			});
		});

		medicalRecordsButton.setOnAction(e->{
			clientConnection.sendString("Retrieve medical records: " + clientConnection.clientEmail);
			clientConnection.setOnResponseListener(response -> {
				if (response instanceof HashMap) {
					Platform.runLater(() -> {
						HashMap<String, File> receivedMap = (HashMap<String, File>) response; // Map date to PDF File
						System.out.println("Received medical records: " + receivedMap);
						loadMedicalRecords(receivedMap);
					});
				}
			});
		});

		ScrollPane scrollPane = new ScrollPane(buttonBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(welcomeLabel, scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}

	// function for creating the healthcare worker's interface
	private Scene buildHealthCareWorkerUI(String name) {
		Label welcomeLabel = new Label("Welcome " + name + "!");
		welcomeLabel.getStyleClass().add("header-label"); // Add the same class for styling

		// Creating scrollable buttons
		VBox buttonBox = new VBox(10); // Spacing between buttons
		buttonBox.setPadding(new Insets(10));
		buttonBox.setAlignment(Pos.CENTER);

		// Adding buttons specific for healthcare workers
		Button messageButton = new Button("Messages");
		messageButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(messageButton);

		Button appointmentsButton = new Button("View Appointments");
		appointmentsButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(appointmentsButton);

		Button medicalRecordsButton = new Button("Medical Records");
		medicalRecordsButton.getStyleClass().add("option-button");
		buttonBox.getChildren().add(medicalRecordsButton);

		Button addPatient = new Button("Add Patient");
		addPatient.getStyleClass().add("option-button");
		buttonBox.getChildren().add(addPatient);

		messageButton.setOnAction(e->{
			clientConnection.sendString("Doctor: Retrieve all ID's " + clientConnection.clientEmail);
			clientConnection.setOnResponseListener(response -> {
				if (response instanceof HashMap) {
					Platform.runLater(() -> {
						System.out.println(response);
						HashMap<String, Integer> receivedMap = (HashMap<String, Integer>) response; // Cast to HashMap
						System.out.println("Contact IDs added: " + receivedMap);
						loadContactsForMessaging(receivedMap);
					});
				}
			});
		});

		appointmentsButton.setOnAction(e->{
			clientConnection.sendString("Doctor retrieve appointments " + clientConnection.clientEmail);
			clientConnection.setOnResponseListener(response -> {
				if (response instanceof Map) {
					Platform.runLater(() -> {
						Map<Integer, String> receivedMap = (Map<Integer, String>) response; // Cast to Hashmap
						loadDoctorsAppointments(receivedMap);
					});
				}
			});
		});

		medicalRecordsButton.setOnAction(e->{
			clientConnection.sendString("Doctor: Retrieve all ID's " + clientConnection.clientEmail);
			clientConnection.setOnResponseListener(response -> {
				if (response instanceof HashMap) {
					Platform.runLater(() -> {
						System.out.println(response);
						HashMap<String, Integer> receivedMap = (HashMap<String, Integer>) response; // Cast to HashMap
						System.out.println("Contact IDs added: " + receivedMap);
						loadContactsForRecords(receivedMap);
					});
				}
			});
		});

		addPatient.setOnAction(e->{
			this.primaryStage.setScene(sceneMap.get("addPatientToDoctor")); // Access the primaryStage field
			this.primaryStage.setTitle("Adding a Patient"); // Update the title
		});

		ScrollPane scrollPane = new ScrollPane(buttonBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(welcomeLabel, scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}

	private Scene viewContacts(HashMap<String, Integer> contactsList) {
		Label welcomeLabel = new Label("Welcome!");
		welcomeLabel.getStyleClass().add("header-label"); // Add the same class for styling

		// Creating scrollable buttons
		VBox buttonBox = new VBox(10); // Spacing between buttons
		buttonBox.setPadding(new Insets(10));
		buttonBox.setAlignment(Pos.CENTER);

		// Adding a back button
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");
		backButton.setOnAction(e -> {
			if (clientConnection.Role.equals("doctor")) {
				this.primaryStage.setScene(sceneMap.get("doctorMainMenu")); // Access the primaryStage field
				this.primaryStage.setTitle("Main Menu"); // Update the title
			} else if (clientConnection.Role.equals("patient")) {
				this.primaryStage.setScene(sceneMap.get("patientMainMenu")); // Access the primaryStage field
				this.primaryStage.setTitle("Main Menu"); // Update the title
			}
		});

		// Adding buttons dynamically for each contact in contactsList
		contactsList.forEach((name, id) -> {
			// Create a new button for each contact name (key in the HashMap)
			Button contactButton = new Button("Contact: " + name);
			contactButton.getStyleClass().add("option-button");

			// Set an action for each button
			contactButton.setOnAction(e -> {
				messagingUser(clientConnection.clientName, name, id);
			});

			// Add the button to the VBox
			buttonBox.getChildren().add(contactButton);
		});

		ScrollPane scrollPane = new ScrollPane(buttonBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Add the back button at the top of the layout
		layout.getChildren().add(backButton);

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(welcomeLabel, scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}

	private Scene messageScreen(String sender, String receiver, Integer receiverID, Integer senderID, List<String> messageHistory) {

		Label welcomeLabel = new Label("Messages");
		welcomeLabel.getStyleClass().add("header-label"); // Style for the header

		// Creating a button for the top left
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");

		// HBox for the top layout containing the back button and label
		HBox topBox = new HBox();
		topBox.setPadding(new Insets(10));
		topBox.setAlignment(Pos.CENTER_LEFT); // Align contents to the left
		topBox.getChildren().addAll(backButton, welcomeLabel);

		// VBox to display messages
		VBox messageBox = new VBox(10); // Spacing between messages
		messageBox.setPadding(new Insets(10));

		// Scroll pane for the message area
		ScrollPane scrollPane = new ScrollPane(messageBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		// Initialize and display message history
		for (String historyMessage : messageHistory) {
			// Example format: "sender: message content"
			boolean isSentByUser = historyMessage.startsWith(sender + ":"); // Check if the sender matches the user

			HBox messageContainer = new HBox(); // Container for each message
			messageContainer.setPadding(new Insets(5));
			messageContainer.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); // Align right for user, left for receiver

			Label historyLabel = new Label(historyMessage);
			historyLabel.setWrapText(true); // Enable text wrapping for longer messages
			historyLabel.setMaxWidth(250); // Set a maximum width for the message bubbles
			historyLabel.getStyleClass().add(isSentByUser ? "message-label-right" : "message-label-left"); // Different styles for user and receiver

			messageContainer.getChildren().add(historyLabel); // Add message to the container
			messageBox.getChildren().add(messageContainer); // Add container to the VBox
		}

		// Ensure the ScrollPane starts at the bottom if there are existing messages
		if (!messageHistory.isEmpty()) {
			scrollPane.setVvalue(1.0);
		}

		// Creating a text field for input
		TextField messageField = new TextField();
		messageField.setPromptText("Type a message...");
		messageField.setPrefHeight(40); // Adjust height for better appearance
		HBox.setHgrow(messageField, Priority.ALWAYS); // Allow the text field to expand

		// Creating the send button
		Button sendButton = new Button("Send");
		sendButton.getStyleClass().add("send-button");

		sendButton.setOnAction(e -> {
			String msg = messageField.getText();
			if (!msg.isEmpty()) {
				Message message = new Message(receiver, sender, msg, receiverID, senderID); // Change ID
				clientConnection.sendMessage(message);

				// Add user's message dynamically
				HBox userMessageContainer = new HBox();
				userMessageContainer.setPadding(new Insets(5));
				userMessageContainer.setAlignment(Pos.CENTER_RIGHT); // Align to the right

				Label messageLabel = new Label(sender + ": " + msg);
				messageLabel.setWrapText(true); // Enable text wrapping for longer messages
				messageLabel.setMaxWidth(250); // Set a maximum width for the message bubbles
				messageLabel.getStyleClass().add("message-label-right"); // Style for user's messages
				userMessageContainer.getChildren().add(messageLabel); // Add label to container
				messageBox.getChildren().add(userMessageContainer); // Add container to VBox

				messageField.clear();

				// Scroll to the bottom to show the latest message
				scrollPane.setVvalue(1.0);
			}
		});

		backButton.setOnAction(e -> {
			if (clientConnection.Role.equals("doctor")) {
				this.primaryStage.setScene(sceneMap.get("doctorMainMenu")); // Access the primaryStage field
				this.primaryStage.setTitle("TESTING DOCTOR: Main Menu"); // Update the title
			} else if (clientConnection.Role.equals("patient")) {
				this.primaryStage.setScene(sceneMap.get("patientMainMenu")); // Access the primaryStage field
				this.primaryStage.setTitle("TESTING PATIENT: Main Menu"); // Update the title
			}
		});

		// Bottom layout containing text field and send button
		HBox bottomBox = new HBox(10); // Spacing between the text field and button
		bottomBox.setPadding(new Insets(10));
		bottomBox.setAlignment(Pos.CENTER); // Align contents vertically
		bottomBox.getChildren().addAll(messageField, sendButton);

		// Main layout for the scene
		BorderPane layout = new BorderPane();
		layout.setTop(topBox); // Header with back button and label
		layout.setCenter(scrollPane); // ScrollPane in the center
		layout.setBottom(bottomBox); // Text field and button at the bottom

		// Apply styles
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

		return new Scene(layout, 400, 700);
	}

	// function for creating the patient's interface
	private Scene patientViewMedicalRecords(HashMap<String, File> medicalRecords) {

		// Creating scrollable buttons
		VBox buttonBox = new VBox(10); // Spacing between buttons
		buttonBox.setPadding(new Insets(10));
		buttonBox.setAlignment(Pos.CENTER);

		// Adding buttons for each date in the medicalRecords map
		final File[] selectedFile = new File[1]; // To store the selected file

		for (Map.Entry<String, File> entry : medicalRecords.entrySet()) {
			String date = entry.getKey();
			File file = entry.getValue();

			Button dateButton = new Button(date);
			dateButton.getStyleClass().add("option-button");
			dateButton.setOnAction(e -> {
				selectedFile[0] = file; // Store the selected file in the variable
				System.out.println("Selected file: " + file.getPath()); // Debug output
				loadeachPDF(file);
			});

			buttonBox.getChildren().add(dateButton);
		}

		// Adding a back button
		Button backButton = new Button("Back");
		backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
		backButton.setOnAction(e -> {
			this.primaryStage.setScene(sceneMap.get("patientMainMenu"));
			this.primaryStage.setTitle("Main Menu");
		});

		// Adding back button to the VBox
		buttonBox.getChildren().add(backButton);

		ScrollPane scrollPane = new ScrollPane(buttonBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}


	private Scene viewEachPDF(File pdfFile) {
		Label headerLabel = new Label("Viewing Medical Record");
		headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10px;");
		headerLabel.setAlignment(Pos.CENTER);

		Button backButton = new Button("Back");
		backButton.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
		backButton.setOnAction(e -> {
			this.primaryStage.setScene(sceneMap.get("patientViewMedicalRecords"));
			this.primaryStage.setTitle("Medical Records");
		});

		Button openPdfButton = new Button("Open PDF");
		openPdfButton.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
		openPdfButton.setOnAction(e -> {
			if (pdfFile != null && pdfFile.exists()) {
				try {
					Desktop.getDesktop().open(pdfFile);
				} catch (IOException ex) {
					Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Error");
					alert.setHeaderText("Cannot Open File");
					alert.setContentText("There was an error opening the file: " + pdfFile.getName());
					alert.showAndWait();
				}
			} else {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("File Not Found");
				alert.setContentText("The file does not exist: " + pdfFile.getName());
				alert.showAndWait();
			}
		});

		VBox layout = new VBox(20);
		layout.setAlignment(Pos.CENTER);
		layout.setPadding(new Insets(20));
		layout.getChildren().addAll(headerLabel, openPdfButton, backButton);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

		return new Scene(layout, 400, 300);
	}


	private Scene viewContactsForRecords(HashMap<String, Integer> contactsList) {
		Label welcomeLabel = new Label("Welcome!");
		welcomeLabel.getStyleClass().add("header-label"); // Add the same class for styling

		// Creating scrollable buttons
		VBox buttonBox = new VBox(10); // Spacing between buttons
		buttonBox.setPadding(new Insets(10));
		buttonBox.setAlignment(Pos.CENTER);

		// Adding a back button
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");
		backButton.setOnAction(e -> {
				this.primaryStage.setScene(sceneMap.get("doctorMainMenu")); // Access the primaryStage field
				this.primaryStage.setTitle("Main Menu"); // Update the title
		});

		// Adding buttons dynamically for each contact in contactsList
		contactsList.forEach((name, id) -> {
			// Create a new button for each contact name (key in the HashMap)
			Button contactButton = new Button("Contact: " + name);
			contactButton.getStyleClass().add("option-button");

			// Set an action for each button
			contactButton.setOnAction(e -> {
				uploadingContacts(clientConnection.clientName, name, id);
			});

			// Add the button to the VBox
			buttonBox.getChildren().add(contactButton);
		});

		ScrollPane scrollPane = new ScrollPane(buttonBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Add the back button at the top of the layout
		layout.getChildren().add(backButton);

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(welcomeLabel, scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}

	private Scene uploadPDF(String sender, String receiver, Integer receiverID) {
		Label instructionLabel = new Label("Upload a PDF");
		instructionLabel.getStyleClass().add("header-label"); // Style for the header

		// Creating a back button
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");

		backButton.setOnAction(e -> {
			this.primaryStage.setScene(sceneMap.get("viewContactsForRecords")); // Access the primaryStage field
			this.primaryStage.setTitle("Patients"); // Update the title
		});

		// Creating a file chooser for PDF upload
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

		// Button to trigger the file chooser
		Button uploadButton = new Button("Upload PDF");
		uploadButton.getStyleClass().add("upload-button");

		Label statusLabel = new Label();
		statusLabel.getStyleClass().add("status-label");

		uploadButton.setOnAction(e -> {
			File selectedFile = fileChooser.showOpenDialog(this.primaryStage);
			if (selectedFile != null) {
				try {
					clientConnection.sendString("Retrieve ID by email:  " + clientConnection.clientEmail);
					clientConnection.setOnResponseListener(response -> {
						if (response instanceof String) {
							Platform.runLater(() -> {
								int id = Integer.parseInt((String) response);
								System.out.println("GOT THE ID!!!!!!!!!!!!!!!! " + id);
								// Send the transfer object
								clientConnection.sendPDFTransferObject(selectedFile, id, receiverID);
								statusLabel.setText("Uploaded: " + selectedFile.getName());
							});
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
					statusLabel.setText("Error uploading file.");
				}
			} else {
				statusLabel.setText("No file selected.");
			}
		});

		// Layout for the scene
		VBox layout = new VBox(20); // Spacing between elements
		layout.setPadding(new Insets(20));
		layout.setAlignment(Pos.CENTER);
		layout.getChildren().addAll(backButton, instructionLabel, uploadButton, statusLabel);

		// Apply styles
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

		return new Scene(layout, 400, 300);
	}



	private Scene createRequestAppointments(HashMap<String, Integer> usersList) {
		Label headerLabel = new Label("Request an Appointment");
		headerLabel.getStyleClass().add("header-label");

		// User Details
		Label doctorLabel = new Label("Select Doctor:");
		ComboBox<String> doctorComboBox = new ComboBox<>();
		doctorComboBox.setPromptText("Choose a doctor");

		// Populate Doctors ComboBox using usersList
		doctorComboBox.getItems().addAll(usersList.keySet());

		Label dateLabel = new Label("Select Date:");
		DatePicker appointmentDatePicker = new DatePicker();
		appointmentDatePicker.setDisable(true); // Initially disabled

		Label timeLabel = new Label("Available Times:");
		ComboBox<String> timeComboBox = new ComboBox<>();
		timeComboBox.setPromptText("Select a time");
		timeComboBox.setDisable(true); // Initially disabled

		for (int hour = 9; hour < 17; hour++) {
			timeComboBox.getItems().add(String.format("%02d:00", hour));
			timeComboBox.getItems().add(String.format("%02d:30", hour));
		}
		timeComboBox.getItems().add("17:00"); // Add 5:00 PM explicitly

		Label notesLabel = new Label("Add Notes (Optional):");
		TextField notesTextField = new TextField();
		notesTextField.setPromptText("Enter any additional details...");

		Button requestButton = new Button("Request Appointment");
		Button backButton = new Button("Back");

		Label messageLabel = new Label();
		messageLabel.getStyleClass().add("error-label");

		// Layout
		GridPane gridPane = new GridPane();
		gridPane.setAlignment(Pos.CENTER);
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		gridPane.add(headerLabel, 0, 0, 2, 1); // Span across 2 columns
		gridPane.add(doctorLabel, 0, 1);
		gridPane.add(doctorComboBox, 1, 1);
		gridPane.add(dateLabel, 0, 2);
		gridPane.add(appointmentDatePicker, 1, 2);
		gridPane.add(timeLabel, 0, 3);
		gridPane.add(timeComboBox, 1, 3);
		gridPane.add(notesLabel, 0, 4);
		gridPane.add(notesTextField, 1, 4);
		gridPane.add(requestButton, 1, 5);
		gridPane.add(backButton, 0, 5);
		gridPane.add(messageLabel, 0, 6, 2, 1); // Span across 2 columns

		// Enable DatePicker and TimeComboBox when a doctor is selected
		doctorComboBox.setOnAction(e -> {
			if (doctorComboBox.getValue() != null) {
				appointmentDatePicker.setDisable(false);
				timeComboBox.setDisable(true); // TimeComboBox remains disabled until a date is selected
			} else {
				appointmentDatePicker.setDisable(true);
				timeComboBox.setDisable(true);
			}
		});

		appointmentDatePicker.setOnAction(e -> {
			if (appointmentDatePicker.getValue() != null) {
				LocalDate selectedDate = appointmentDatePicker.getValue();
				Integer doctorID = usersList.get(doctorComboBox.getValue());

				// Send request to get unavailable time slots for the selected doctor and date
				clientConnection.sendString("Get doctor time slots " + usersList.get(doctorComboBox.getValue())); // Sending over doctorComboBox doctor's ID

				// Handle server response to populate available time slots
				clientConnection.setOnResponseListener(response -> {
					if (response instanceof List) {
						List<String> unavailableTimes = (List<String>) response;

						Platform.runLater(() -> {
							// Populate timeComboBox with available time slots
							timeComboBox.getItems().clear();

							// Extract unavailable times for the selected date
							List<String> unavailableTimeStrings = unavailableTimes.stream()
									.filter(timestamp -> {
										// Check if the date matches the selected date
										String datePart = timestamp.split(" ")[0]; // Extract date part (e.g., "2024-12-29")
										return datePart.equals(selectedDate.toString());
									})
									.map(timestamp -> timestamp.split(" ")[1].substring(0, 5)) // Extract time part (e.g., "10:30")
									.collect(Collectors.toList());

							for (int hour = 9; hour < 17; hour++) {
								String time1 = String.format("%02d:00", hour); // "09:00", "10:00", etc.
								String time2 = String.format("%02d:30", hour); // "09:30", "10:30", etc.
								if (!unavailableTimeStrings.contains(time1)) {
									timeComboBox.getItems().add(time1);
								}
								if (!unavailableTimeStrings.contains(time2)) {
									timeComboBox.getItems().add(time2);
								}
							}

							String lastTime = "17:00"; // Explicitly add 5:00 PM
							if (!unavailableTimeStrings.contains(lastTime)) {
								timeComboBox.getItems().add(lastTime);
							}

							// Enable the timeComboBox once it's populated
							if (!timeComboBox.getItems().isEmpty()) {
								timeComboBox.setDisable(false);
								timeComboBox.setStyle("");
								timeComboBox.setPromptText("Select a time");
							} else {
								timeComboBox.setPromptText("Day fully booked");
								timeComboBox.setDisable(true); // If no slots available, keep it disabled
								timeComboBox.setStyle("-fx-border-color: red; -fx-background-color: #FFCCCC;"); // Red border and light red background
							}
						});
					}
				});
			} else {
				timeComboBox.setDisable(true); // Disable if no date is selected
			}
		});

		// Back Button Action
		backButton.setOnAction(e -> primaryStage.setScene(buildPatientUI(clientConnection.clientName))); // Replace with your main menu scene

		// Request Appointment Button Action
		requestButton.setOnAction(e -> {
			String doctor = doctorComboBox.getValue();
			Integer doctorID = usersList.get(doctor);
			LocalDate date = appointmentDatePicker.getValue();
			String time = timeComboBox.getValue();
			String notes = notesTextField.getText();

			if (doctor == null || date == null || time == null) {
				messageLabel.setText("All fields are required.");
				return;
			}

			// Send appointment request to the server
			clientConnection.sendString("Sending appointment info to: " + doctorID
					+ " date: " + date.toString()
					+ " time: " + time
					+ " notes: " + notes
					+ " from: " + clientConnection.clientEmail);

			messageLabel.setText("Appointment request sent successfully.");
		});

		return new Scene(gridPane, 400, 600);
	}

	private Scene viewRequestAppointmentsHealthcare(Map<Integer, String> receivedMap) {
		Label welcomeLabel = new Label("Viewing Appointments");
		welcomeLabel.getStyleClass().add("header-label"); // Add the same class for styling

		// Creating scrollable buttons
		VBox buttonBox = new VBox(10); // Spacing between buttons
		buttonBox.setPadding(new Insets(10));
		buttonBox.setAlignment(Pos.CENTER);

		// Adding a back button
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");
		backButton.setOnAction(e -> {
			this.primaryStage.setScene(sceneMap.get("doctorMainMenu")); // Access the primaryStage field
			this.primaryStage.setTitle("Main Menu"); // Update the title
		});

		// Dynamically create buttons for each appointment
		for (Map.Entry<Integer, String> entry : receivedMap.entrySet()) {
			Integer appointmentId = entry.getKey();
			String appointmentDate = entry.getValue();

			Button appointmentButton = new Button(appointmentDate);
			appointmentButton.getStyleClass().add("option-button");

			// Set click event for each appointment button
			appointmentButton.setOnAction(e -> {
				if (clientConnection.Role.equals("doctor")) {
					clientConnection.sendString("Doctor Finding appointment details: " + appointmentId);
				}
				else if (clientConnection.Role.equals("patient")) {
					clientConnection.sendString("Patient Finding appointment details: " + appointmentId);
				}

				clientConnection.setOnResponseListener(response -> {
					if (response instanceof Map) {
						Platform.runLater(() -> {
							Map<String, Object> details = (Map<String, Object>) response;
							loadEachDoctorsAppointment(details);
						});
					}
				});
			});

			buttonBox.getChildren().add(appointmentButton);
		}

		ScrollPane scrollPane = new ScrollPane(buttonBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Add the back button at the top of the layout
		layout.getChildren().add(backButton);

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(welcomeLabel, scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}


	private Scene viewEachRequestHealthcare(Map<String, Object> details) {
		if (details == null) {
			VBox layout = new VBox(20);
			layout.setAlignment(Pos.CENTER);

			Button backButton = new Button("Back");
			backButton.getStyleClass().add("back-button");
			backButton.setOnAction(e -> {
				this.primaryStage.setScene(sceneMap.get("viewRequestAppointmentsHealthcare"));
				this.primaryStage.setTitle("Viewing Appointments");
			});

			layout.getChildren().add(backButton);
			layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
			return new Scene(layout);
		}

		String appointmentDate = details.get("appointment_date").toString();
		Label welcomeLabel = new Label(appointmentDate);
		welcomeLabel.getStyleClass().add("header-label"); // Add the same class for styling

		// Creating a VBox for displaying the details
		VBox detailsBox = new VBox(10); // Spacing between details
		detailsBox.setPadding(new Insets(10));
		detailsBox.setAlignment(Pos.CENTER_LEFT);

		// Adding details from the map
		for (Map.Entry<String, Object> entry : details.entrySet()) {
			Label detailLabel = new Label(entry.getKey() + ": " + entry.getValue());
			detailLabel.getStyleClass().add("detail-label");
			detailsBox.getChildren().add(detailLabel);
		}

		// Adding a back button
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");
		backButton.setOnAction(e -> {
			this.primaryStage.setScene(sceneMap.get("viewRequestAppointmentsHealthcare")); // Access the primaryStage field
			this.primaryStage.setTitle("Viewing Appointments"); // Update the title
		});

		ScrollPane scrollPane = new ScrollPane(detailsBox);
		scrollPane.setFitToWidth(true);
		scrollPane.setFocusTraversable(false);

		VBox layout = new VBox(20); // Outer VBox
		layout.setAlignment(Pos.TOP_CENTER); // Align the content to the top center

		// Add the back button at the top of the layout
		layout.getChildren().add(backButton);

		// Set VBox's properties
		VBox.setVgrow(scrollPane, Priority.ALWAYS); // Make the ScrollPane take up the remaining space

		layout.getChildren().addAll(welcomeLabel, scrollPane);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
		return new Scene(layout);
	}


	private Scene addPatientToDoctor(String message) {
		// Back button
		Button backButton = new Button("Back");
		backButton.getStyleClass().add("back-button");
		backButton.setOnAction(e -> {
			// Navigate to the previous scene (replace "doctorMainMenu" with your actual scene key)
			primaryStage.setScene(sceneMap.get("doctorMainMenu"));
			primaryStage.setTitle("Main Menu");
		});

		// Welcome label
		Label welcomeLabel = new Label("Please add a patient:");
		welcomeLabel.getStyleClass().add("header-label");

		// Message label for feedback
		Label messageLabel = new Label();
		messageLabel.setVisible(false); // Initially hidden

		// Text field for entering patient information
		TextField patientEmailField = new TextField();
		patientEmailField.setPromptText("Enter patient's name");
		patientEmailField.getStyleClass().add("custom-text-field");

		Button submitButton = new Button("Add Patient");
		submitButton.getStyleClass().add("option-button");

		// Action for the submit button
		submitButton.setOnAction(e -> {
			String patientEmail = patientEmailField.getText().trim();
			if (!patientEmail.isEmpty()) {
				System.out.println("Adding Patient " + patientEmail + " to " + clientConnection.clientEmail);
				// Check to server if email is invalid, try adding patient email to healthcare worker
				clientConnection.sendString("Adding Patient Email " + patientEmail + " to " + clientConnection.clientEmail);

				// Wait for a response (this depends on your client-server communication)
				clientConnection.setOnResponseListener(response -> {
					if (response instanceof String) {
						Platform.runLater(() -> { // Ensure UI updates are on the JavaFX thread
							messageLabel.setVisible(true);
							String msg = (String) response;
								if (msg.startsWith("Patient email already added: ")) {
									String[] parts = msg.split(" "); // Split the message by spaces
									// Extract patient email and doctor email using their respective positions
									String patientEmailRetrieved = parts[4]; // Position of the patient email
									String doctorEmail = parts[7];          // Position of the doctor email
									messageLabel.setText(patientEmailRetrieved + " was already added.");
									messageLabel.setStyle("-fx-text-fill: red;"); // Green for success
								}
								// patient found and not added, SUCCESS
								else if (msg.startsWith("Patient email found: ")) {
									System.out.println(msg);
									String[] parts = msg.split(" "); // Split by spaces
									String patientEmailRetrieved = parts[3]; // Extract patient email
									String doctorEmail = parts[6];  // Extract doctor's name
									System.out.println("Patient email: " + patientEmailRetrieved + ", Doctor email: " + doctorEmail);
									messageLabel.setText(patientEmailRetrieved + " added as a patient!");
									messageLabel.setStyle("-fx-text-fill: green;"); // Green for success
								}
								else if (msg.startsWith("Patient email not found:")) {
									System.out.println("Patient email not found. Please try again.");
									messageLabel.setText("Patient not found. Try again.");
									messageLabel.setStyle("-fx-text-fill: red;"); // Red for error
								}
						});
					}
				});
				// Clear the field for the next input
				patientEmailField.clear();
			// no patient name entered
			} else {
				System.out.println("No patient name entered!");
				messageLabel.setVisible(true);
				messageLabel.setText("No patient name entered!");
				messageLabel.setStyle("-fx-text-fill: red;"); // Red for error
			}
		});

		// Layout for input and button
		VBox inputBox = new VBox(10, patientEmailField, submitButton, messageLabel); // Spacing between input and button
		inputBox.setAlignment(Pos.CENTER);

		// Top layout containing back button
		HBox topBox = new HBox(backButton);
		topBox.setAlignment(Pos.TOP_LEFT);
		topBox.setPadding(new Insets(10));

		VBox layout = new VBox(20, topBox, welcomeLabel, inputBox); // Outer VBox
		layout.setPadding(new Insets(20));
		layout.setAlignment(Pos.CENTER);
		layout.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

		return new Scene(layout, 400, 500); // Set scene size
	}






	//
	// The functions below create a progress + fade transition between scenes.
	// switchWithProgressAndFade() is the main function that will be used to transition between scenes
	//
	public void switchWithProgressAndFade(Stage stage, Scene nextScene) {
		// Create a scene with a progress indicator
		ProgressIndicator progressIndicator = new ProgressIndicator();
		VBox layout = new VBox(progressIndicator);
		layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
		Scene progressScene = new Scene(layout, 300, 500);

		// Set the stage to show the progress indicator
		stage.setScene(progressScene);

		// Simulate a loading process
		new Thread(() -> {
			try {
				Thread.sleep(2000); // Simulate loading time (2 seconds)
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// After loading is done, run this on the JavaFX Application Thread
			Platform.runLater(() -> {
				// Apply the fade transition after loading is complete
				fadeOutAndSwitch(stage, nextScene);
			});
		}).start();
	}

	public void fadeOutAndSwitch(Stage stage, Scene newScene) {
		FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), stage.getScene().getRoot());
		fadeOut.setFromValue(1.0);
		fadeOut.setToValue(0.0);
		fadeOut.setOnFinished(event -> {
			stage.setScene(newScene);
			fadeIn(stage.getScene().getRoot());
		});
		fadeOut.play();
	}

	public void fadeIn(Node node) {
		FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), node);
		fadeIn.setFromValue(0.0);
		fadeIn.setToValue(1.0);
		fadeIn.play();
	}

	public void loadContactsForMessaging(HashMap<String, Integer> usersList){
		sceneMap.put("viewContacts", viewContacts(usersList));
		this.primaryStage.setScene(sceneMap.get("viewContacts")); // Access the primaryStage field
		this.primaryStage.setTitle("Patients"); // Update the title
	}

	public void loadContactsForAppointments(HashMap<String, Integer> usersList){
		sceneMap.put("createRequestAppointments", createRequestAppointments(usersList));
		this.primaryStage.setScene(sceneMap.get("createRequestAppointments")); // Access the primaryStage field
		this.primaryStage.setTitle("createRequestAppointments"); // Update the title
	}

	public void loadContactsForRecords(HashMap<String, Integer> usersList){
		sceneMap.put("viewContactsForRecords", viewContactsForRecords(usersList));
		this.primaryStage.setScene(sceneMap.get("viewContactsForRecords")); // Access the primaryStage field
		this.primaryStage.setTitle("Patients"); // Update the title
	}

	public void loadMedicalRecords(HashMap<String, File> receivedMap) {
		sceneMap.put("patientViewMedicalRecords", patientViewMedicalRecords(receivedMap));
		this.primaryStage.setScene(sceneMap.get("patientViewMedicalRecords")); // Access the primaryStage field
		this.primaryStage.setTitle("Medical Records"); // Update the title
	}

	public void loadeachPDF(File file) {
		sceneMap.put("viewEachPDF", viewEachPDF(file));
		this.primaryStage.setScene(sceneMap.get("viewEachPDF")); // Access the primaryStage field
		this.primaryStage.setTitle("Viewing Medical Record"); // Update the title
	}

	public void loadDoctorsAppointments(Map<Integer, String> receivedMap) {
		sceneMap.put("viewRequestAppointmentsHealthcare", viewRequestAppointmentsHealthcare(receivedMap));
		this.primaryStage.setScene(sceneMap.get("viewRequestAppointmentsHealthcare")); // Access the primaryStage field
		this.primaryStage.setTitle("Appointments"); // Update the title
	}

	public void loadEachDoctorsAppointment(Map<String, Object> details) {
		sceneMap.put("viewEachRequestHealthcare", viewEachRequestHealthcare(details));
		this.primaryStage.setScene(sceneMap.get("viewEachRequestHealthcare")); // Access the primaryStage field
		this.primaryStage.setTitle("Appointment Details"); // Update the title
	}

	public void uploadingContacts(String sender, String receiver, int id) {
		sceneMap.put("uploadPDF", uploadPDF(sender, receiver, id));
		this.primaryStage.setScene(sceneMap.get("uploadPDF"));
		this.primaryStage.setTitle("Uploading document to: " + receiver);
	}


	public void messagingUser(String sender, String receiver, int id) {
		clientConnection.sendString("Retrieve Client ID: " + clientConnection.clientEmail);
		// Use AtomicReference for thread-safe access to clientID
		AtomicReference<Integer> clientID = new AtomicReference<>();

		try {
			// Set a unified response listener
			clientConnection.setOnResponseListener(response -> {
				Platform.runLater(() -> {
					if (response instanceof String) {
						String message = (String) response;
						if (message.startsWith("ID: ")) {
							String[] parts = message.split(" ", 2); // Split into two parts
							String retrievedId = parts[1];          // Get the ID
							try {
								clientID.set(Integer.parseInt(retrievedId)); // Update the AtomicReference
								System.out.println("Retrieved Client ID: " + clientID.get());

								// Request messages after retrieving the ID
								clientConnection.sendString("Retrieve Messages for ID: " + clientID.get() + " " + id);
							} catch (NumberFormatException e) {
								System.err.println("Failed to parse client ID: " + retrievedId);
							}
						}
					} else if (response instanceof List) {
						List<String> messagesList = (List<String>) response;
//						System.out.println("Retrieved Messages List: " + messagesList);

						// Proceed to message screen once we have the messages
						sceneMap.put("messageScreen", messageScreen(sender, receiver, id, clientID.get(),messagesList));
						this.primaryStage.setScene(sceneMap.get("messageScreen"));
						this.primaryStage.setTitle("Messaging: " + receiver);
					}
				});
			});
		} catch (Exception e) {
			throw new RuntimeException("Error during messaging user setup", e);
		}
	}
}
