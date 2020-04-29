package com.benjazzy.mixchat.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class LogoutController {
    private MixController mixController;

    @FXML
    private Button Done;

    @FXML
    private Button Cancel;

    LogoutController(MixController mixController) {
        this.mixController = mixController;
    }

    @FXML
    private void Logout(ActionEvent event) {
        mixController.getMixOauth().logout();
        mixController.disconnectAllTabs(true);
        mixController.nullToken();
        close();
    }

    @FXML
    private void Cancel(ActionEvent event) {
        close();
    }

    @FXML
    private void LogoutHandleKey(KeyEvent event) {
	if (event.getCode() == KeyCode.ENTER) {
	   Logout(new ActionEvent()); 
        }	
    }

    @FXML
    private void CancelHandleKey(KeyEvent event) {
	if (event.getCode() == KeyCode.ENTER) {
	   close(); 
        }	
    }

    private void close() {
        Stage stage = (Stage)Done.getScene().getWindow();
        stage.close();
    }
}
