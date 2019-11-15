package com.benjazzy.mixchat;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MixUI extends Application {

    private static MixUI single_intstance = null;

    public MixChat chat;


    @FXML
    private MenuItem connectMenu;
    @FXML
    private TextField connectWindowField;
    @FXML
    private Button connectWindowDone;
    @FXML
    private TextField message;
    @FXML
    private Button sendMessage;

    public MixUI() {
        single_intstance = this;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException, InterruptedException, ExecutionException {

        // Create the FXMLLoader
        FXMLLoader loader = new FXMLLoader();
        // Path to the FXML File
        //String fxmlDocPath = "src/main/java/com/benjazzy/mixchat/MixChat.fxml";
        loader.setLocation(getClass().getResource("/MixChat.fxml"));
        //FileInputStream fxmlStream = new FileInputStream(fxmlDocPath);

        // Create the Pane and all Details
        Pane root = loader.load();

        // Create the Scene
        Scene scene = new Scene(root);
        // Set the Scene to the Stage
        stage.setScene(scene);
        // Set the Title to the Stage
        stage.setTitle("MixChat");
        // Display the Stage
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                chat.disconnect();
                Platform.exit();
                System.exit(0);
            }
        });

        chat = new MixChat(root);
        // testNode.setText("Hard coded text");

    }

    public static MixUI getInstance() {
        return single_intstance;
    }
}
