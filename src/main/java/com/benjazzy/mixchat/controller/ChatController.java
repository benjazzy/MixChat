package com.benjazzy.mixchat.controller;

import com.benjazzy.mixchat.MixChat;
import com.benjazzy.mixchat.MixUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

public class ChatController {
    private MixChat mixChat;

    @FXML
    private Pane root;
    @FXML
    private MenuItem connectMenu;           /** Menu item that contains the connect and disconect items. */
    @FXML
    private TextField message;              /** TextField containing the message the user wants to send. */
    @FXML
    private Button sendMessage;             /** Button to send message. */

    public void setMixChat(MixChat chat)
    {
        mixChat = chat;
    }

    /**
     * Send the message in message to the Mixer chat.
     *
     * @param event
     */
    @FXML
    private void SendMessage(ActionEvent event) {
        String m = message.getText();
        if (MixUI.getInstance().getChat().isConnected()) {

            message.setText("");
            MixUI.getInstance().getChat().sendMessage(m);
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
            if (MixUI.getInstance().getChat().isConnected()) {
                message.setText("");
                MixUI.getInstance().getChat().sendMessage(m);
            } else {
                System.out.println("Error not connected");
            }
        }
    }
}
