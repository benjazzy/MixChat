package com.benjazzy.mixchat;

import com.benjazzy.mixchat.controller.MixController;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class MixMessage extends Text {
    private String uuid;    /** Message uuid. */
    private String user;    /** User who sent the message. */
    private String whisperUser;

    private MixChat mixChat;

    MixMessage(String text, String id, String user, MixChat mixChat) {
        this.setText(text);
        this.uuid = id;
        this.user = user;
        this.mixChat = mixChat;

        this.setOnContextMenuRequested(contextMenuEvent -> updateContextMenu().show((Node) contextMenuEvent.getSource(), contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));

        /*
         * Register click listener.
         */
        EventHandler<MouseEvent> overHandler = mouseEvent -> setUnderline(true);
        this.addEventFilter(MouseEvent.MOUSE_ENTERED, overHandler);

        EventHandler<MouseEvent> offHandler = mouseEvent -> setUnderline(false);
        this.addEventFilter(MouseEvent.MOUSE_EXITED, offHandler);

    }

    MixMessage(String text, String id, String user, String whisperUser) {
        this.setText(text);
        this.uuid = id;
        this.user = user;
        this.whisperUser = whisperUser;

        /*
         * Add delete option to context menu.
         */
        ContextMenu menu = new ContextMenu();
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(event -> {
            Thread thread = new Thread(() -> {
                MixController controller = MixUI.getInstance().getLoader().getController();
                controller.deleteMessage(uuid);
            });
            thread.start();
        });

        menu.getItems().addAll(delete);

        this.setOnContextMenuRequested(contextMenuEvent -> menu.show((Node) contextMenuEvent.getSource(), contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));

        /*
         * Register click listener.
         */
        EventHandler<MouseEvent> overHandler = mouseEvent -> setUnderline(true);
        this.addEventFilter(MouseEvent.MOUSE_ENTERED, overHandler);

        EventHandler<MouseEvent> offHandler = mouseEvent -> setUnderline(false);
        this.addEventFilter(MouseEvent.MOUSE_EXITED, offHandler);

    }

    private ContextMenu updateContextMenu() {
        /*
         * Add delete option to context menu.
         */
        ContextMenu menu = new ContextMenu();

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(event -> {
            Thread thread = new Thread(() -> {
                MixController controller = MixUI.getInstance().getLoader().getController();
                controller.deleteMessage(uuid);
            });
            thread.start();
        });

        if (mixChat.canDeleteMessage(uuid)) {
            menu.getItems().addAll(delete);
        }

        return menu;
    }

    public String getUuid() {
        return this.uuid;
    }
    public String getUser() {
        return this.user;
    }
}
