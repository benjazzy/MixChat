package com.benjazzy.mixchat;

import com.benjazzy.mixchat.controller.MixController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Used to control the javafx ui and holds the instance of MixChat.
 */
public class MixUI extends Application {

    private static MixUI single_instance = null;

    private List<MixChat> chats = new LinkedList();
    private MixChat chat;                                   /** Handles the Mixer API. */
    private static Pane root;                               /** Root pane of the main menu. */
    private FXMLLoader loader;                              /** FXML loader of the root Pane. */

    /**
     * Set single_instance to itself.
     */
    public MixUI() {
        single_instance = this;
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start is called when the javafx ui starts.  Gets the root Pane and gives it to mixchat.
     *
     * @param stage
     * @throws IOException  Throws IOException if javafx is unable to find MixChat.fxml.
     */
    @Override
    public void start(Stage stage) throws IOException {

        /**
         * Loads the javafx configuration from the resources/MixChat.fxml.
         */
        loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/MixChat.fxml"));

        // Create the Pane and all Details
        root = loader.load();

        // Create the Scene
        Scene scene = new Scene(root);
        // Set the Scene to the Stage
        stage.setScene(scene);
        // Set the Title to the Stage
        stage.setTitle("MixChat");
        // Display the Stage
        stage.show();

        /**
         * Setup the program to disconnect from the Mixer API and close on exit.
         */
        stage.setOnCloseRequest(t -> {
            if (loader != null)
            {
                MixController controller = loader.getController();
                controller.disconnectAllTabs();
            }
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * GetInstance is used to get the single instance of MixUI.
     *
     * @return  Returns the single instance of itself.
     */
    public static MixUI getInstance() {
        return single_instance;
    }

    public FXMLLoader getLoader()
    {
        return loader;
    }
}
