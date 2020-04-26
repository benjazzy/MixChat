package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.oauth.MixOauth;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class LogoutController {
    MixController mixController;

    @FXML
    private Button Done;

    @FXML
    private Button Cancel;

    LogoutController(MixController mixController) {
        this.mixController = mixController;
    }

    @FXML
    private void Logout(ActionEvent event) {
        MixOauth oauth = new MixOauth();
        oauth.logout();
    }

    @FXML
    private void Cancel(ActionEvent event) {
        mixController.logout();
        Stage stage = (Stage)Done.getScene().getWindow();
        stage.close();
    }
}
