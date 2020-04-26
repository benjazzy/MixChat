package com.benjazzy.mixchat.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

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

    private void close() {
        Stage stage = (Stage)Done.getScene().getWindow();
        stage.close();
    }
}
