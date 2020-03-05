package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.MixUI;
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
    private ConnectController connectController;

    @FXML
    private MenuItem connectMenu;           /** Menu item that contains the connect and disconect items. */
    @FXML
    private TextField message;              /** TextField containing the message the user wants to send. */
    @FXML
    private Button sendMessage;             /** Button to send message. */
    @FXML
    private Button connect;
    @FXML
    private TabPane connections;

    /**
     * Sets this instance of ConnectController.
     *
     * @param controller
     */
    public void setConnectController(ConnectController controller)
    {
        this.connectController = controller;
    }

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

            // Pass this instance of MixController to the ConnectController.
            setConnectController(loader.getController());
            connectController.setMixController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect to the chat in connectWindowField.
     *
     * @param channelName
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void Connect(String channelName) throws InterruptedException, ExecutionException {
        /** Check if a channel name has been specified. */
        if (channelName != null) {
            Tab chat = new Tab(channelName);
            System.out.println(connections.getTabs());
            connections.getTabs().add(0, chat);

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
}
