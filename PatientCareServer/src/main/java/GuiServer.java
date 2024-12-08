
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application {

    HashMap<String, Scene> sceneMap;
    Server serverConnection;

    ListView<String> listItems;
    ListView<String> serverUsers;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        serverConnection = new Server(data -> {
            Platform.runLater(() -> {
                listItems.getItems().add(data.toString());

                if (data.toString().contains("has joined") || data.toString().contains("has disconnected"))
                    updateServerUsersList();
            });
        });

        listItems = new ListView<>();
        serverUsers = new ListView<>();

        sceneMap = new HashMap<String, Scene>();

        sceneMap.put("server", createServerGui());

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
//                serverConnection.saveDatabase();
                Platform.exit();
                System.exit(0);
            }
        });

        primaryStage.setScene(sceneMap.get("server"));
        primaryStage.setTitle("PatientCare+ Server");
        primaryStage.show();

    }

    private void updateServerUsersList() {
        // Clear the current items and add all the updated server users
        Platform.runLater(() -> {
            serverUsers.getItems().clear();
            serverUsers.getItems().addAll(serverConnection.updateCurrentUsers());
        });
    }

    public Scene createServerGui() {
        Label headerLabel = new Label("PatientCare+ Server Logs");
        TextField userHeader = new TextField("Online Users");
        TextField activityHeader = new TextField("Activity");

        userHeader.setEditable(false);
        userHeader.setMouseTransparent(true);

        userHeader.setStyle("-fx-background-color: transparent; " +
                "-fx-border-width: 0; " +
                "-fx-border-color: transparent;" +
                "-fx-font-size: 16px");

        activityHeader.setEditable(false);
        activityHeader.setMouseTransparent(true);
        activityHeader.setStyle("-fx-background-color: transparent; " +
                "-fx-border-width: 0; " +
                "-fx-border-color: transparent;" +
                "-fx-font-size: 16px");

        headerLabel.setMouseTransparent(true);
        headerLabel.setStyle("-fx-background-color: transparent; " +
                "-fx-border-width: 0; " +
                "-fx-border-color: transparent;" +
                "-fx-font-size: 24px");

        VBox userColumn = new VBox(10, userHeader, serverUsers);
        VBox activityColumn = new VBox(10, activityHeader, listItems);

        HBox userAndActivity = new HBox(20, userColumn, activityColumn);
        VBox server = new VBox(20, headerLabel, userAndActivity);

        serverUsers.setPrefWidth(300);
        listItems.setPrefWidth(600);

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(70));
        server.setStyle("-fx-background-color: #E8F0FE");

        pane.setCenter(server);
        pane.requestFocus();
        pane.setStyle("-fx-font-family: 'serif'; -fx-background-color: #E8F0FE");
        return new Scene(pane, 700, 400);


    }


}
