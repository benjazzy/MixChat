package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.oauth.MixOauth;
import com.benjazzy.mixchat.preferences.MixPreferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Controller class for the main UI and chat connect UI.
 */
public class MixController {
    private ConnectController connectController;
    private MixPreferences mixPreferences;

    private String token;

    @FXML
    private Button connect;
    @FXML
    private TabPane connections;
    @FXML
    private Tab AddTab;

    public MixController() {
        mixPreferences = new MixPreferences();
    }

    @FXML
    public void initialize() {
        for (String channelName : mixPreferences.getDefaultChannels()) {
            try {
                Connect(channelName);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets this instance of ConnectController.
     *
     * @param controller
     */
    public void setConnectController(ConnectController controller)
    {
        this.connectController = controller;
    }

    public void deleteMessage(String uuid)
    {
        for (Tab tab : connections.getTabs())
        {
            if (tab != AddTab)
            {
                getChatController(tab).deleteMessage(uuid);
            }
        }
    }

    /**
     * Disconnects all open tabs.
     */
    public void disconnectAllTabs()
    {
        for (Tab tab : connections.getTabs())
        {
            if (tab != AddTab)
            {
                getChatController(tab).disconnect();
            }
        }
    }

    /**
     * Closes a given tab.
     *
     * @param tab
     */
    private void closeTab(Tab tab) {
        if (tab != AddTab)
        {
            EventHandler<Event> handler = tab.getOnClosed();
            if (null != handler) {
                handler.handle(null);
            } else {
                tab.getTabPane().getTabs().remove(tab);
            }
        }
    }

    /**
     * Gets the ChatController for a given tab.
     *
     * @param tab
     * @return
     */
    private ChatController getChatController(Tab tab)
    {
        Scene scene = tab.getContent().getScene();
        FXMLLoader loader = (FXMLLoader)scene.getUserData();
        return loader.getController();
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
     * Disconnect from the chat.
     *
     * @param event
     */
    @FXML
    private void Disconnect(ActionEvent event) {
        Tab tab = connections.getSelectionModel().getSelectedItem();
        getChatController(tab).disconnect();
        closeTab(tab);

        mixPreferences.removeDefaultChannel(tab.getText());
    }

    @FXML
    public void Settings(ActionEvent event) {
        Pane root;
        try {
            FXMLLoader loader = new FXMLLoader();
            // Path to the FXML File
            loader.setLocation(getClass().getResource("/SettingsWindow.fxml"));
            //Manually set the javafx controller factory so that we can pass arguments to the constructor.
            Callback<Class<?>, Object> settingsFactory = type -> {
                if (type == SettingsController.class) {
                    return new SettingsController(this);
                } else {
                    try {
                        return type.newInstance() ; // default behavior - invoke no-arg construtor
                    } catch (Exception exc) {
                        System.err.println("Could not create controller for "+type.getName());
                        throw new RuntimeException(exc);
                    }
                }
            };
            loader.setControllerFactory(settingsFactory);
            // Create the Pane and all Details
            root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.show();
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
            connections.getTabs().add(connections.getTabs().size() - 1, chat);
            connections.getSelectionModel().select(chat);

            Pane root;
            FXMLLoader loader = new FXMLLoader();
            // Path to the FXML File
            loader.setLocation(getClass().getResource("/MixChatWindow.fxml"));
            try {
                root = loader.load();
                ChatController chatController = loader.getController();

                if (token == null) {
                    /** Gets an Oauth2 access token from MixOauth. */
                    MixOauth oauth = new MixOauth();
                    token = oauth.getAccessToken();
                }
                Thread thread = new Thread(() -> {
                    chatController.connect(channelName, token);
                });
                thread.start();

                // Queue up these tasks to run after the scene is setup.
                Platform.runLater(() -> {
                    Scene scene = root.getScene();
                    scene.setUserData(loader);
                });
                AnchorPane anchorPane = new AnchorPane();
                AnchorPane.setTopAnchor(root, 0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root, 0.0);
                AnchorPane.setRightAnchor(root, 0.0);
                anchorPane.getChildren().add(root);

                chat.setContent(anchorPane);

                // Add channelName to the list of channels to be opened on startup.
                mixPreferences.addDefaultChannel(channelName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public TabPane getConnections() {
        return connections;
    }

    public MixPreferences getMixPreferences() {
        return mixPreferences;
    }
}
