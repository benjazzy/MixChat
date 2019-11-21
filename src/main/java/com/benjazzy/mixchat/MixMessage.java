package com.benjazzy.mixchat;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class MixMessage extends Text  {
    private String uuid;    /** Message uuid. */
    private String user;    /** User who sent the message. */

    MixMessage(String text, String id, String user) {
        this.setText(text);
        this.uuid = id;
        this.user = user;

        /**
         * Add delete option to context menu if you are the owner of the message or a moderator.
         */
        ContextMenu menu = new ContextMenu();
        //if (this.user.equals(MixUI.getInstance().chat.getMixerUsername())) {
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(event -> {
                Thread thread = new Thread(() -> MixUI.getInstance().getChat().deleteMessage(uuid));
                thread.run();
            });

            menu.getItems().addAll(delete);
        //}

        this.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent contextMenuEvent) {
                menu.show((Node) contextMenuEvent.getSource(), contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
            }
        });

        /**
         * Register click listener.
         */
        EventHandler<MouseEvent> overHandler = mouseEvent -> setUnderline(true);
        this.addEventFilter(MouseEvent.MOUSE_ENTERED, overHandler);

        EventHandler<MouseEvent> offHandler = mouseEvent -> setUnderline(false);
        this.addEventFilter(MouseEvent.MOUSE_EXITED, offHandler);

    }

    public String getUuid() {
        return this.uuid;
    }
}
