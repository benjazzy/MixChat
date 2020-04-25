package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.DataModel;
import com.benjazzy.mixchat.MixChat;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.concurrent.ExecutionException;

public class ChatController {
    private MixChat mixChat;
    private String channelName;
    private Text liveText;
    private DataModel dataModel;

    @FXML
    private VBox Root;
    @FXML
    private MenuItem connectMenu;           /** Menu item that contains the connect and disconnect items. */
    @FXML
    private TextField message;              /** TextField containing the message the user wants to send. */
    @FXML
    private Button sendMessage;             /** Button to send message. */
    @FXML
    private TextFlow ChatBox;
    @FXML
    private TextFlow UserList;
    @FXML
    private ScrollPane ChatScrollPane;
    @FXML
    private AnchorPane ChatAnchor;
    @FXML
    private HBox LiveBar;
    @FXML
    private Circle LiveCircle;

    public ChatController(DataModel dataModel) {
        this.dataModel = dataModel;
    }

    @FXML
    public void initialize()
    {
        ChatScrollPane.setVvalue(1);
        ChatScrollPane.vvalueProperty().bind(ChatBox.heightProperty());
        ChatBox.prefWidthProperty().bind(ChatScrollPane.widthProperty());
    }

    /**
     * Connects the MixChat to the selected channel
     *
     * @param name
     */
    public void connect(String name, String token)
    {
        channelName = name;
        try {
            mixChat = new MixChat(this, ChatBox, UserList, ChatScrollPane);
            mixChat.connect(channelName, token);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tells the MixChat to disconnect.
     */
    public void disconnect()
    {
        mixChat.disconnect();
    }

    public void deleteMessage(String uuid)
    {
        mixChat.deleteMessage(uuid);
    }

    public void setLive(boolean live)
    {
        if (live) {
            if (!LiveBar.getChildren().contains(liveText)) ;
            {
                liveText = new Text("Live!");
                liveText.setFill(Color.RED);
                LiveBar.getChildren().add(liveText);
                LiveCircle.setFill(Color.RED);
            }
        }
        else {
            if (liveText != null) {
                LiveBar.getChildren().remove(liveText);
                LiveCircle.setFill(Color.GREY);
            }
        }
    }

    /**
     * Send the message in message to the Mixer chat.
     *
     * @param event
     */
    @FXML
    private void SendMessage(ActionEvent event) {
        String m = message.getText();
        if (mixChat.isConnected()) {

            message.setText("");
            mixChat.sendMessage(m);
        } else {
            System.out.println("Error not connected");
        }
    }

    /**
     * Called when any key is pressed in message.  If the key is enter send message.
     *
     * @param event
     */
    @FXML
    private void handleMessageEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String m = message.getText();
            if (mixChat.isConnected()) {
                message.setText("");
                mixChat.sendMessage(m);
            } else {
                System.out.println("Error not connected");
            }
        }
    }
}
