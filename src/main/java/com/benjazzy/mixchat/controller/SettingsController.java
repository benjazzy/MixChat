package com.benjazzy.mixchat.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class SettingsController {
    @FXML
    private ListView<String> DefaultChannels;

    private MixController mixController;

    SettingsController(MixController mixController) {
        this.mixController = mixController;
    }

    @FXML
    public void initialize() {
        DefaultChannels.setItems(mixController.getMixPreferences().defaultChannels);
    }
}
