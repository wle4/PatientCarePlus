import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.List;
import java.util.Map;



public class Client extends Thread{

	// Declaring member variables for socket, input/output streams, and a callback function
	Socket socketClient;
	ObjectOutputStream out; // java library
	ObjectInputStream in; // java library

	String clientName;
	String clientEmail;
	User client;
	String Role; // "patient" - patient, "doctor" - healthcare worker
	Integer id;

	//callback is important for sending messages from the back-end to the front-end
	private Consumer<Serializable> callback;

	// Additional callback for dynamically handling responses (handle scene communication)
	private Consumer<Serializable> responseListener;

	//constructor for the client object
	Client(Consumer<Serializable> call){
		clientName = "temp";
		Role = "temp";
		callback = call;
	}

	public void run() {
		try {
			// Establish connection
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		} catch (Exception e) {
			e.printStackTrace(); // Print connection error
			return; // Exit if connection fails
		}

		// Reading objects dynamically
		while (true) {
			try {
				Object obj = in.readObject(); // Read an object from the server

				// Handle User object
				if (obj instanceof User) {
					User checkUser = (User) obj;
					if (checkUser != null) {
						client = checkUser;
						clientName = checkUser.getFirstName() + " " + checkUser.getLastName();
						clientEmail = checkUser.getEmail();
						Role = checkUser.getRole();
						callback.accept(client);
					} else {
						callback.accept(null); // Null user indicates invalid credentials
					}
				}
				// Handle String
				else if (obj instanceof String) {
					String message = (String) obj;
					System.out.println("Received string: " + message);
					if (responseListener != null) {
						responseListener.accept(message);
					} else {
						callback.accept(message);
					}
				}
				// Handle Message object
				else if (obj instanceof Message) {
					Message msg = (Message) obj;
					System.out.println("Received message from " + msg.getSender() + " to " + msg.getReceiver() + ": " + msg.getMsg());
				}
				// Handle List object
				else if (obj instanceof List) {
					List<?> messageList = (List<?>) obj; // Safely cast to List with wildcard type
					System.out.println("Received list: " + messageList);
					if (responseListener != null) {
						responseListener.accept((Serializable) messageList);
					} else {
						callback.accept((Serializable) messageList);
					}
				}
				// Handling Map objects
				else if (obj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Integer> map = (Map<String, Integer>) obj;
					System.out.println("Received map: " + map);
					if (responseListener != null) {
						responseListener.accept((Serializable) map);
					} else {
						callback.accept((Serializable) map);
					}
				}
//				// Handle File (PDF) object
//				else if (obj instanceof File) {
//					File pdfFile = (File) obj;
//					System.out.println("Received PDF file: " + pdfFile.getName());
//					if (responseListener != null) {
//						responseListener.accept(pdfFile);
//					} else {
//						callback.accept(pdfFile);
//					}
//				}
				// Handle PDFTransferObject object
				else if (obj instanceof PDFTransferObject) {
					PDFTransferObject pdfTransfer = (PDFTransferObject) obj;
					System.out.println("Received PDF from senderID: " + pdfTransfer.getSenderId() +
							" for receiverID: " + pdfTransfer.getReceiverId() +
							" with file name: " + pdfTransfer.getFileName());

					// Save the file locally
					File file = new File("downloads/" + pdfTransfer.getFileName());
					try (FileOutputStream fos = new FileOutputStream(file)) {
						fos.write(pdfTransfer.getPdfData());
						System.out.println("PDF saved as: " + file.getAbsolutePath());
					} catch (IOException e) {
						System.out.println("Failed to save PDF file.");
						e.printStackTrace();
					}
				}

				// Handle unknown object
				else {
					System.out.println("Received unknown object type: " + obj.getClass());
				}
			} catch (Exception e) {
				e.printStackTrace();
				break; // Exit the loop if there’s an error
			}
		}
	}

	// Send OUT STRING to the server
	public void sendString(String msg) {
		try {
			out.writeObject(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send OUT MESSAGE objects to the server
	public void sendMessage(Message message) {
		try {
			out.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Send OUT USER objects to server
	public void sendLoginInfo(User user) throws IOException {
		out.writeObject(user);
	}

	// Send a PDFTransferObject to the server
	public void sendPDFTransferObject(File pdfFile, int senderID, int receiverID) {
		try {
			// Read file into byte array
			byte[] pdfData = new byte[(int) pdfFile.length()];
			try (FileInputStream fis = new FileInputStream(pdfFile)) {
				fis.read(pdfData);
			}

			// Create and send the transfer object
			PDFTransferObject pdfTransferObject = new PDFTransferObject(senderID, receiverID, pdfData, pdfFile.getName());
			out.writeObject(pdfTransferObject);
			out.flush();
			System.out.println("Sent PDF file: " + pdfFile.getName() +
					" from senderID: " + senderID +
					" to receiverID: " + receiverID);
		} catch (IOException e) {
			System.out.println("Error sending PDF file.");
			e.printStackTrace();
		}
	}

//	// Send OUT PDF files to the server
//	public void sendPDF(File pdfFile) {
//		try (FileInputStream fis = new FileInputStream(pdfFile)) {
//			out.writeObject(pdfFile);
//			System.out.println("Sent PDF file: " + pdfFile.getName());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	// Setter for dynamic response listener (handle scene communication)
	public void setOnResponseListener(Consumer<Serializable> listener) {
		this.responseListener = listener;
	}
}