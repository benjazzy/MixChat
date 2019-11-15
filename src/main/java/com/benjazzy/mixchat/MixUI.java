package com.benjazzy.mixchat;

import com.mixer.api.MixerAPI;
import com.mixer.api.http.SortOrderMap;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.channel.MixerChannel;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.IncomingMessageEvent;
import com.mixer.api.resource.chat.events.UserJoinEvent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.response.channels.ShowChannelsResponse;
import com.mixer.api.services.impl.ChannelsService;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MixUI extends Application {

    private static MixUI single_intstance = null;

    public MixChat chat;


    @FXML
    private MenuItem connectMenu;
    @FXML
    private TextField connectWindowField;
    @FXML
    private Button connectWindowDone;
    @FXML
    private TextField message;
    @FXML
    private Button sendMessage;

    public MixUI() {
        single_intstance = this;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException, InterruptedException, ExecutionException {

        // Create the FXMLLoader
        FXMLLoader loader = new FXMLLoader();
        // Path to the FXML File
        String fxmlDocPath = "src/main/java/com/benjazzy/mixchat/MixChat.fxml";
        FileInputStream fxmlStream = new FileInputStream(fxmlDocPath);

        // Create the Pane and all Details
        Pane root = loader.load(fxmlStream);

        // Create the Scene
        Scene scene = new Scene(root);
        // Set the Scene to the Stage
        stage.setScene(scene);
        // Set the Title to the Stage
        stage.setTitle("MixChat");
        // Display the Stage
        stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });

        chat = new MixChat(root);
        // testNode.setText("Hard coded text");

    }

    public static MixUI getInstance() {
        return single_intstance;
    }


    ///////////////////////////////
    @FXML
    private void ConnectMenu(ActionEvent event) throws InterruptedException, ExecutionException {

        // MixUI.connect("benjazzy");

        Pane root;
        try {
            FXMLLoader loader = new FXMLLoader();
            // Path to the FXML File
            String fxmlDocPath = "src/main/java/com/benjazzy/mixchat/ConnectWindow.fxml";
            FileInputStream fxmlStream = new FileInputStream(fxmlDocPath);

            // Create the Pane and all Details
            root = loader.load(fxmlStream);
            Stage stage = new Stage();
            stage.setTitle("Connect");
            stage.setScene(new Scene(root));
            stage.show();
            // Hide this current window (if this is what you want)
            // ((Node)(event.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * chatConnectable.on(UserJoinEvent.class, event -> {
         * chatConnectable.send(ChatSendMethod
         * .of(String.format("Hi %s! I'm pingbot! Write !ping and I will pong back!",
         * event.data.username))); });
         */

        // ((TextFlow) testNode).getChildren().add(testText);
    }

    @FXML
    private void ConnectWindowDone(ActionEvent event) throws InterruptedException, ExecutionException {

        String channelName = connectWindowField.getText();
        if (channelName != null) {

            try {
                chat.connect(channelName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Stage stage = (Stage) connectWindowDone.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void SendMessage(ActionEvent event) {
        String m = message.getText();
        if (chat.isConnected()) {

            message.setText("");
            chat.sendMessage(m);
        } else {
            System.out.println("Error not connected");
        }
    }

    @FXML
    private void handleMessageEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String m = message.getText();
            if (chat.isConnected()) {

                message.setText("");
                chat.sendMessage(m);
            } else {
                System.out.println("Error not connected");
            }
        }

    }
}
