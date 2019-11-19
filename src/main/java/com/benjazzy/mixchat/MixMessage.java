package com.benjazzy.mixchat;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.UUID;

public class MixMessage extends Text {
    private String uuid;    /** Message uuid. */

    MixMessage(String text, String id) {
        this.setText(text);
        this.uuid = id;

        ContextMenu menu = new ContextMenu();
        MenuItem delete = new MenuItem("Delete");
        menu.getItems().addAll(delete);

        delete.setOnAction(event -> {
            Thread thread = new Thread(() -> MixUI.getInstance().chat.deleteMessage(uuid));
            thread.run();
        });

        this.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent contextMenuEvent) {
                menu.show((Node) contextMenuEvent.getSource(), contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
            }
        });

        /**
         * Register click listener.
         */
        /*EventHandler<MouseEvent> clickHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                System.out.println("Hello World");
                ContextMenu menu = new ContextMenu();
                VBox box = new VBox();

                MixUI.root.getChildren().add(menu);
                MenuItem delete = new MenuItem("Delete");
                menu.getItems().add(delete);
                //pane.relocate(e.getSceneX(), e.getScreenY());
                menu.show();
            }
        };
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, clickHandler);*/

        EventHandler<MouseEvent> overHandler = mouseEvent -> setUnderline(true);
        this.addEventFilter(MouseEvent.MOUSE_ENTERED, overHandler);

        EventHandler<MouseEvent> offHandler = mouseEvent -> setUnderline(false);
        this.addEventFilter(MouseEvent.MOUSE_EXITED, offHandler);

    }

    public String getUuid() {
        return this.uuid;
    }
}
