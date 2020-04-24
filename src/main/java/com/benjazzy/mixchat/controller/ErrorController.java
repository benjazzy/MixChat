package com.benjazzy.mixchat.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.concurrent.ExecutionException;

public class ErrorController {
    private MixController mixController;

    @FXML
    private Button errorWindowDone;

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
     * Close the errorWindowField when "done" is pressed.
     *
     * @param event Key press for "enter" button
     * @throws InterruptedException In case execution is interrupted
     * @throws ExecutionException In case execution is executioned
     */
    @FXML
    private void ErrorWindowDone(ActionEvent event) throws InterruptedException, ExecutionException {
        Stage stage = (Stage)errorWindowDone.getScene().getWindow();
        stage.close();
    }

    /**
     * Called when any key is pressed in the connect window field.  If the key is enter connect to the specified channel.
     *
     * @param event "Enter" key is pressed
     */
    @FXML
    private void handleConnectEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                ErrorWindowDone(new ActionEvent());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
