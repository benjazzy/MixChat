package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.MixChat;
import com.benjazzy.mixchat.MixUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import javax.jws.soap.SOAPBinding;
import java.util.concurrent.ExecutionException;

public class ChatController {
    private MixChat mixChat;
    private String channelName;

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

    /**
     * Connects the MixChat to the selected channel
     *
     * @param name
     */
    public void Connect(String name)
    {
        channelName = name;
        try {
            mixChat = new MixChat(ChatBox, UserList, ChatScrollPane);
            mixChat.connect(channelName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void Disconnect()
    {
        mixChat.disconnect();
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