package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.preferences.MixPreferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class SettingsController {
    @FXML
    TableView DefaultChannels;

    MixPreferences mixPreferences;

    public SettingsController() {
        mixPreferences = new MixPreferences();
    }

    @FXML
    public void initialize() {
        ObservableList<String> userList = FXCollections.observableArrayList();
        userList.addAll(mixPreferences.getDefaultChannels());
        DefaultChannels.setItems(userList);
    }
}
