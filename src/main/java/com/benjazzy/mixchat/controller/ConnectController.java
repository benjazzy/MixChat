package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.MixUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ConnectController {
    private MixController mixController;

    @FXML
    private TextField connectWindowField;   /** TextField containing the name of the channel to connect to. */
    @FXML
    private Button connectWindowDone;       /** Button to connect to chat specified in connectWindowField. */

    /**
     * Sets this instance of MixController
     *
     * @param mix
     */
    public void setMixController(MixController mix)
    {
        this.mixController = mix;
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
            mixController.Connect(channelName);

            Stage stage = (Stage)connectWindowDone.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Called when any key is pressed in the connect window field.  If the key is enter connect to the specified channel.
     *
     * @param event
     */
    @FXML
    private void handleConnectEnter(KeyEvent event) {/*
        if (event.getCode() == KeyCode.ENTER) {
            try {
                ConnectWindowDone(new ActionEvent());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    */}
}
