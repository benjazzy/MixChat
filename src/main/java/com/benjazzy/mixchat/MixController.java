package com.benjazzy.mixchat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MixController {
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

    @FXML
    private void ConnectMenu(ActionEvent event) throws InterruptedException, ExecutionException {

        // MixUI.connect("benjazzy");

        Pane root;
        try {
            FXMLLoader loader = new FXMLLoader();
            // Path to the FXML File
            String fxmlDocPath = "src/main/java/com/benjazzy/mixchat/ConnectWindow.fxml";
            FileInputStream fxmlStream = new FileInputStream(fxmlDocPath);

            // Create the Pane and all Details
            root = loader.load(fxmlStream);
            Stage stage = new Stage();
            stage.setTitle("Connect");
            stage.setScene(new Scene(root));
            stage.show();
            // Hide this current window (if this is what you want)
            // ((Node)(event.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * chatConnectable.on(UserJoinEvent.class, event -> {
         * chatConnectable.send(ChatSendMethod
         * .of(String.format("Hi %s! I'm pingbot! Write !ping and I will pong back!",
         * event.data.username))); });
         */

        // ((TextFlow) testNode).getChildren().add(testText);
    }

    @FXML
    private void ConnectWindowDone(ActionEvent event) throws InterruptedException, ExecutionException {

        String channelName = connectWindowField.getText();
        if (channelName != null) {

            try {
                MixUI.getInstance().chat.connect(channelName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Stage stage = (Stage) connectWindowDone.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void SendMessage(ActionEvent event) {
        String m = message.getText();
        if (MixUI.getInstance().chat.isConnected()) {

            message.setText("");
            MixUI.getInstance().chat.sendMessage(m);
        } else {
            System.out.println("Error not connected");
        }
    }

    @FXML
    private void handleMessageEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String m = message.getText();
            if (MixUI.getInstance().chat.isConnected()) {

                message.setText("");
                MixUI.getInstance().chat.sendMessage(m);
            } else {
                System.out.println("Error not connected");
            }
        }

    }

}
