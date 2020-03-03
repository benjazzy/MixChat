package com.benjazzy.mixchat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Controller class for the main UI and chat connect UI.
 */
public class MixController {
    @FXML
    private MenuItem connectMenu;           /** Menu item that contains the connect and disconect items. */
    @FXML
    private TextField connectWindowField;   /** TextField containing the name of the channel to connect to. */
    @FXML
    private Button connectWindowDone;       /** Button to connect to chat specified in connectWindowField. */
    @FXML
    private TextField message;              /** TextField containing the message the user wants to send. */
    @FXML
    private Button sendMessage;             /** Button to send message. */
    @FXML
    private Button connect;
    @FXML
    private TabPane connections;

    /**
     * Opens the connect menu.
     *
     * @param event
     */
    @FXML
    private void ConnectMenu(ActionEvent event) {
        Pane root;
        try {
            FXMLLoader loader = new FXMLLoader();
            // Path to the FXML File
            loader.setLocation(getClass().getResource("/ConnectWindow.fxml"));

            // Create the Pane and all Details
            root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Connect");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect to the chat in connectWindowField.
     *
     * @param event
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @FXML
    private void ConnectWindowDone(ActionEvent event) throws InterruptedException, ExecutionException {

        String channelName = connectWindowField.getText();
        /** Check if a channel name has been specified. */
        if (channelName != null) {
            Tab chat = new Tab(channelName);
            System.out.println(connections.getTabs());
            connections.getTabs().add(chat);

            Pane root;
            FXMLLoader loader = new FXMLLoader();
            // Path to the FXML File
            loader.setLocation(getClass().getResource("/MixChatWindow.fxml"));
            try {
                root = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }

            MixUI.getInstance().getChat().connect(channelName);

            Stage stage = (Stage) connectWindowDone.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Disconnect from the chat.
     *
     * @param event
     */
    @FXML
    private void Disconnect(ActionEvent event) {
        MixUI.getInstance().getChat().disconnect();
    }

    /**
     * Send the message in message to the Mixer chat.
     *
     * @param event
     */
    @FXML
    private void SendMessage(ActionEvent event) {
        String m = message.getText();
        if (MixUI.getInstance().getChat().isConnected()) {

            message.setText("");
            MixUI.getInstance().getChat().sendMessage(m);
        } else {
            System.out.println("Error not connected");
        }
    }

    /**
     * Called when any key is pressed in message.  If the key is enter send message.
     *
     * @param event
     */
    @FXML
    private void handleMessageEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String m = message.getText();
            if (MixUI.getInstance().getChat().isConnected()) {
                message.setText("");
                MixUI.getInstance().getChat().sendMessage(m);
            } else {
                System.out.println("Error not connected");
            }
        }
    }

    /**
     * Called when any key is pressed in the connect window field.  If the key is enter connect to the specified channel.
     *
     * @param event
     */
    @FXML
    private void handleConnectEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                ConnectWindowDone(new ActionEvent());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
