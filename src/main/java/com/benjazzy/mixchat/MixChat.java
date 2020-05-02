package com.benjazzy.mixchat;

import com.benjazzy.mixchat.controller.ChatController;
import com.benjazzy.mixchat.helper.ConsoleColors;
import com.benjazzy.mixchat.socket.*;
import com.mixer.api.MixerAPI;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.channel.MixerChannel;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.*;
import com.mixer.api.resource.chat.events.data.IncomingMessageData;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.methods.*;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ChatHistoryReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.resource.constellation.MixerConstellation;
import com.mixer.api.resource.constellation.events.HelloEvent;
import com.mixer.api.resource.constellation.events.LiveEvent;
import com.mixer.api.resource.constellation.methods.LiveSubscribeMethod;
import com.mixer.api.resource.constellation.methods.data.LiveRequestData;
import com.mixer.api.resource.constellation.replies.LiveRequestReply;
import com.mixer.api.resource.constellation.ws.MixerConstellationConnectable;
import com.mixer.api.services.impl.ChannelsService;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * The MixChat class handles communication with the Mixer API
 * and displays the chat to the terminal and javafx textflow chatBox.
 */
public class MixChat {

    //region Local variables.
    /**
     * The connected variable is used to store whether the chat is connected.
     */
    private boolean connected = false;

    /**
     * The token variable is used to store the Oauth2 token.
     */
    private String token = "";

    /**
     * Username of the currently connected user.
     */
    private String mixerUsername = "";

    /**
     * ChatId stores the id of the currently connected chat.
     */
    private int chatId = 0;

    /**
     * UserId stores the id of the current user.
     */
    private int userId = 0;

    /**
     * Users stores a list of users currently connected to the chat.
     */
    private volatile List<MixChatUser> users = new LinkedList<>();

    /**
     * In larger channels mixer doesn't send out the user join/leave event.
     * This timer will update chat every 5 minutes if those events aren't triggered.
     */
    private Timer userListTimer = new Timer();

    /**
     * User and roles of the current user.
     */
    private MixChatUser currentUser;

    private MixChatSocket mixChatSocket;

    /**
     * True if the current user has moderator permission in the current chat.
     */
    private boolean hasPermissions = false;
    //endregion

    //region FXML variables.
    /**
     * The connetoller for the chat ui.
     */
    private ChatController chatController;

    /**
     * Chat box ui element.
     */
    private TextFlow chatBox;

    /**
     * UserList is the TextFlow where the list of users from users is displayed.
     */
    private TextFlow userList;

    /**
     * The only reason for chatScrollPane's existence is so that it can be set to always scroll to the bottom.
     * ChatBox is contained inside chatScrollPane.
     */
    private ScrollPane chatScrollPane;
    //endregion

    //region Mixer variables.
    /**
     * Mixer stores the main MixerAPI object
     */
    private MixerAPI mixer;

    /**
     * ChatConnectible is used interface with the connected chat.
     */
    private MixerChatConnectable chatConnectible;

    /**
     * constellation is the constellation object used to subscribe and get constellation events from mixer's api.
     */
    MixerConstellation constellation;

    /**
     * Connectable constellation.
     */
    MixerConstellationConnectable constellationConnectable;

    /**
     * Used to manually interface with the Mixer API
     */
    private MixSocketClient socket;
    //endregion

    /**
     * The constructor links the javafx variables to their Panes.
     *
     * @param chat
     * @param users
     * @param chatPane
     */
    public MixChat(ChatController controller, TextFlow chat, TextFlow users, ScrollPane chatPane) {
        System.out.println("Setting chatBox");
        chatController = controller;
        chatBox = chat;
        userList = users;
        chatScrollPane = chatPane;

        /* Sets chatScrollPane to always scroll to the bottom */
        //chatScrollPane.vvalueProperty().bind(chatBox.heightProperty());
        //chatBox.prefWidthProperty().bind(chatScrollPane.widthProperty());
    }

    //region Connect and disconnect.
    /**
     * Connects to the specified Mixer chat.
     *
     * @param chatName Name of the channel that is being connected to.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void connect(String chatName, String token) throws InterruptedException, ExecutionException {
        System.out.println(chatName);

        Platform.runLater(() -> chatBox.getChildren().clear());      /* Clears the chat for the next connection. */

        this.token = token;

        /* Authenticates with Mixer using the Oauth2 token. */
        mixer = new MixerAPI("3721d6b1332a6db44a22ab5b71ae8e34ae187ee995b38f1a", token);
        int id = 0;                         /* Stores the chat id. */

        // TODO replace with mixer.use().findOneByToken(chatName).get();
        /*
         * Resolves the channel name into chatId.
         */
        String result = "";
        try {
            result = getHTML(String.format("https://mixer.com/api/v1/channels/%s?fields=id", chatName));
            JSONObject obj = new JSONObject(result);
            id = obj.getInt("id");
            chatId = id;
            //System.out.println(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * Set the user, chat, and channel objects from channelName.
         */
        MixerUser user = mixer.use(UsersService.class).getCurrent().get();
        MixerChat chat = mixer.use(ChatService.class).findOne(id).get();
        MixerChannel channel = mixer.use(ChannelsService.class).findOneByToken(chatName).get();

        mixerUsername = user.username;
        userId = user.id;

        /* Set chatConnectable to use the current chat. */
        chatConnectible = chat.connectable(mixer);

        /*
         * Authenticate with mixer.
         * If successfully authenticated to the chat then:
         *      Print Connected,
         *      Get message history.
         */
        if (chatConnectible.connect()) {

            chatConnectible.send(AuthenticateMessage.from(channel, user, chat.authkey),
                    new ReplyHandler<AuthenticationReply>() {
                        public void onSuccess(AuthenticationReply reply) {
                            System.out.println("Connected");
                            connected = true;

                            mixChatSocket = new MixChatSocket(chatId);
                            mixChatSocket.auth(channel.id, userId, new MixSocketReply() {
                                @Override
                                public void onReply(JSONObject reply) {
                                    handleSocketReply(reply);
                                }
                            });

                            /* Get previous 50 messages and update the terminal and chatBox */
                            chatConnectible.send(GetHistoryMethod.forCount(50), new ReplyHandler<ChatHistoryReply>() {
                                @Override
                                public void onSuccess(@Nullable ChatHistoryReply result) {
                                    updateText(formatChatBox(result));
                                    System.out.println(formatTerminalChat(result));
                                }

                                @Override
                                public void onFailure(Throwable err) {
                                    err.printStackTrace();
                                    System.out.println("Failed to get chat history.");
                                }
                            });
                        }

                        public void onFailure(Throwable var1) {
                            var1.printStackTrace();
                        }
                    });

            /* Registers events for incoming messages as well as user join and leave */
            registerIncomingChat();

            // Link constellation and mixer.
            constellation = new MixerConstellation();
            constellation.connectable(mixer);

            // Get a constellation connectible using mixer and constellation.
            constellationConnectable = constellation.connectable(mixer);

            if (constellationConnectable.connect())
            {
                LiveSubscribeMethod method = new LiveSubscribeMethod();
                LiveRequestData data = new LiveRequestData();
                data.events = new ArrayList<>();
                data.events.add(String.format("channel:%d:update", channel.id));
                method.params = data;

                constellationConnectable.send(method, new com.mixer.api.resource.constellation.replies.ReplyHandler<LiveRequestReply>() {
                    @Override
                    public void onSuccess(@Nullable LiveRequestReply result) {
                    }

                    @Override
                    public void onFailure(Throwable err) {
                        System.out.println(err);
                    }
                });
                registerConstellation();
            }

            try {
                if (channel.online) {
                    Platform.runLater(() -> chatController.setLive(true));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Updates the list of current viewers. */
            updateUsers(chatId);

            /* Update the current user from the list of users. */
            for (MixChatUser mixChatUser : users) {
                if (user.id == mixChatUser.getId()) {
                    currentUser = mixChatUser;
                    if (currentUser.getRoleList().contains(MixerUser.Role.MOD) || currentUser.getRoleList().contains(MixerUser.Role.OWNER)) {
                        hasPermissions = true;
                    }
                    break;
                }
            }

        } else {
            System.out.println("Failed to connect");
        }
    }

    /**
     * Disconnect from the current mixer chat.
     */
    public void disconnect() {
        if (isConnected()) {
            chatConnectible.disconnect();
            connected = false;
        }
    }
    //endregion

    //region Format chat.
    /**
     * Update the chatBox with the new messages.
     *
     * @param text List of Text objects to be added to chatBox.
     */
    private void updateText(List<Node> text) {
        for (Node t : text) {
            Platform.runLater(() -> chatBox.getChildren().add(t));
        }
        Platform.runLater(() -> chatBox.getChildren().add(new Text(System.lineSeparator())));

    }

    /**
     * Format the text from an incoming message.
     *
     * @param event Incoming message event that contains all the data of the message.
     * @return Returns a list of formatted text objects to be displayed in the chatBox.
     */
    public List<Node> formatChatBox(IncomingMessageEvent event) {
        List<Node> textList = new ArrayList<>();                            /* List of Text objects to be returned. */
        MixMessage username = new MixMessage(event.data.userName, event.data.id, event.data.userName, this); /* Username of the user that sent the message. */
        List<Node> message = new LinkedList<>();                            /* List of message elements from event. */

        /*
         * Formats the current time to be added to textList.
         */
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        MixMessage dateText = new MixMessage(formatter.format(date), event.data.id, event.data.userName, this);

        /*
         * Iterates the message elements and adds them to the message list.
         */
        for (MessageTextComponent i : event.data.message.message) {
            message.addAll(formatMessageComponent(i, event.data));
        }

        /*
         * Colors the username based on the user role in chat.
         */
        // Add color to username by user role
        if (event.data.userRoles.contains(MixerUser.Role.MOD)) {
            username.setFill(Color.GREEN);
        } else if (event.data.userRoles.contains(MixerUser.Role.PRO)) {
            username.setFill(Color.DEEPPINK);
        } else if (event.data.userRoles.contains(MixerUser.Role.USER)) {
            username.setFill(Color.SKYBLUE);
        } else if (event.data.userRoles.contains(MixerUser.Role.OWNER)) {
        } else if (event.data.userRoles.contains(MixerUser.Role.FOUNDER)) {
            username.setFill(Color.RED);
        } else if (event.data.userRoles.contains(MixerUser.Role.STAFF)) {
            username.setFill(Color.GOLD);
        } else if (event.data.userRoles.contains(MixerUser.Role.GLOBAL_MOD)) {
            username.setFill(Color.TEAL);
        }

        if (event.data.message.meta.whisper) {
            username.setText(String.format(" %s ", username.getText(), mixerUsername));
            username.setFont(Font.font("Verdana", FontPosture.ITALIC, 12));
        } else {
            /* Adds spaces between the elements and adds a colon after the username. */
            username.setText(String.format(" %s: ", username.getText()));
        }

        /*
         * Adds together all the elements into one text list.
         */
        textList.add(dateText);
        textList.add(username);
        if (event.data.message.meta.whisper) {
            MixMessage to = new MixMessage(String.format(" %s: ", mixerUsername), event.data.id, event.data.userName, this);
            Text separator = new Text(">");
            separator.setFont(Font.font("Verdana", FontPosture.ITALIC, 12));
            to.setFont(Font.font("Verdana", FontPosture.ITALIC, 12));
            to.setFill(getRoleColor(getUserRole(mixerUsername)));
            textList.add(separator);
            textList.add(to);
        }
        for (Node m : message) {
            if (event.data.message.meta.whisper && m instanceof MixMessage)
                ((MixMessage) m).setFont(Font.font("Verdana", FontPosture.ITALIC, 12));
            textList.add(m);
        }

        return textList;
    }

    /**
     * Format the messages from previous messages.
     *
     * @param event Contains all the information on the past messages.
     * @return Returns a formatted list of messages to be displayed in chatBox.
     */
    public List<Node> formatChatBox(ChatHistoryReply event) {
        /* List of formatted Text objects to be returned. */
        List<Node> textList = new ArrayList<>();

        /*
         * Iterate through each message and format it.
         */
        for (IncomingMessageData messageEvent : event.messages) {
            /* Add a new line to the beginning to separate it from the previous line. */
            textList.add(new MixMessage(System.lineSeparator(), messageEvent.id, messageEvent.userName, this));
            /* Gets the username from messageEvent. */
            MixMessage username = new MixMessage(messageEvent.userName, messageEvent.id, messageEvent.userName, this);
            /* List of message elements from event. */
            List<Node> message = new LinkedList<>();

            /*
             * Iterate through message elements and add them to the message text.
             */
            for (MessageTextComponent i : messageEvent.message.message) {
                message.addAll(formatMessageComponent(i, messageEvent));
            }

            /*
             * Colors the username based on the user role in chat.
             */
            if (messageEvent.userRoles.contains(MixerUser.Role.MOD)) {
                username.setFill(Color.GREEN);
            } else if (messageEvent.userRoles.contains(MixerUser.Role.PRO)) {
                username.setFill(Color.DEEPPINK);
            } else if (messageEvent.userRoles.contains(MixerUser.Role.USER)) {
                username.setFill(Color.SKYBLUE);
            } else if (messageEvent.userRoles.contains(MixerUser.Role.OWNER)) {
            } else if (messageEvent.userRoles.contains(MixerUser.Role.FOUNDER)) {
                username.setFill(Color.RED);
            } else if (messageEvent.userRoles.contains(MixerUser.Role.STAFF)) {
                username.setFill(Color.GOLD);
            } else if (messageEvent.userRoles.contains(MixerUser.Role.GLOBAL_MOD)) {
                username.setFill(Color.TEAL);
            }

            /* Adds spaces between the elements and adds a colon after the username. */
            username.setText(String.format(" %s: ", username.getText()));

            /*
             * Adds each element to textList.
             */
            textList.add(username);
            for (Node m : message) {
                textList.add(m);
            }
        }
        return textList;
    }

    /**
     * Formats the message based on the message type.
     *
     * @param textComponent
     * @param event Added .md to include in jar.
     * @return
     */
    private List<Node> formatMessageComponent(MessageTextComponent textComponent, IncomingMessageData event) {
        List<Node> message = new LinkedList<>();

        // Link.
        if (textComponent.type.name().equals("LINK")) {
            Hyperlink m = new Hyperlink(textComponent.text);
            m.setOnAction(actionEvent -> {
                if (Desktop.isDesktopSupported()) {
                    new Thread(() -> {
                        try {
                            Desktop.getDesktop().browse(new URI(textComponent.url));
                        } catch (MalformedURLException | URISyntaxException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            });
            message.add(m);
        }
        // Sparks or other image.
        else if (textComponent.url != null) {
            try {
                message.add(new ImageView(new Image(textComponent.url, 50, 50, false, false)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Emote.
        else if (textComponent.type.name().equals("EMOTICON")) {
            Image image;
            int height = 24;
            int width = 24;
            if (textComponent.source.equals("builtin")) {
                image = new Image(String.format("https://mixer.com/_latest/emoticons/%s.png", textComponent.pack));
            } else {
                image = new Image(textComponent.pack);
                if (image.getHeight() % 56 == 0) {
                    height = 56;
                    width = 56;
                }
            }
            PixelReader reader = image.getPixelReader();
            ImageView imageView = new ImageView(new WritableImage(reader, textComponent.coords.x, textComponent.coords.y, height, width));
            imageView.setFitHeight(28);
            imageView.setFitWidth(28);
            message.add(imageView);
        }
        // Tag(@)
        else if (textComponent.type.name().equals("TAG")) {
            MixMessage m = new MixMessage(textComponent.text, event.id, event.userName, this);
            m.setFill(Color.BLUE);
            message.add(m);
        }
        // Text.
        else {
            MixMessage m = new MixMessage(textComponent.text, event.id, event.userName, this);
            if (!m.getText().contains("�\u200D♂") && !m.getText().contains("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️") && !m.getText().isEmpty())
                message.add(m);
        }
        return message;
    }

    /**
     * Formats messages from previous messages for the terminal.
     *
     * @param event Contains previous messages.
     * @return Returns a formatted String for the terminal.
     */
    public String formatTerminalChat(ChatHistoryReply event) {
        /* String r is what is returned by the function. */
        String r = "";

        /*
         * Iterate through each previous message and format it.
         */
        for (IncomingMessageData m : event.messages) {
            String username = m.userName;       /* Set the username from the message username. */
            String usernameFormat = "";         /* Contains the ANSI escape sequences to color the username. */
            String message = "";                /* Contains the chat message. */

            /*
             * Setup the date formatter to be displayed.
             */
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();

            /*
             * Set usernameFormat to the correct color based on the user role in chat.
             */
            if (m.userRoles.contains(MixerUser.Role.MOD)) {
                usernameFormat = ConsoleColors.GREEN_BOLD_BRIGHT;
            } else if (m.userRoles.contains(MixerUser.Role.PRO)) {
                usernameFormat = ConsoleColors.PURPLE_BOLD_BRIGHT;
            } else if (m.userRoles.contains(MixerUser.Role.USER)) {
                usernameFormat = ConsoleColors.CYAN_BOLD_BRIGHT;
            } else if (m.userRoles.contains(MixerUser.Role.OWNER)) {
                usernameFormat = ConsoleColors.WHITE_BOLD_BRIGHT;
            } else if (m.userRoles.contains(MixerUser.Role.FOUNDER)) {
                usernameFormat = ConsoleColors.RED_BOLD_BRIGHT;
            } else if (m.userRoles.contains(MixerUser.Role.STAFF)) {
                usernameFormat = ConsoleColors.YELLOW_BOLD_BRIGHT;
            } else if (m.userRoles.contains(MixerUser.Role.GLOBAL_MOD)) {
                usernameFormat = ConsoleColors.BLUE_BOLD_BRIGHT;
            }

            /*
             * Add each message component to the message string.
             */
            for (MessageTextComponent i : m.message.message) {
                String format = "";
                if (i.text.contains("\u0040")) {
                    format = ConsoleColors.BLUE_UNDERLINED;
                }
                message = String.format("%s%s%s%s", format, message, i.text, ConsoleColors.RESET);
            }

            /*
             * Put all the elements together and add a newline.
             */
            String output = String.format("%s %s%s%s: %s", formatter.format(date), usernameFormat, username,
                    ConsoleColors.RESET, message);
            r = String.format("%s\n%s", r, output);
        }
        return r;
    }

    /**
     * Formats messages from an incoming message for the terminal.
     *
     * @param event Contains incoming message.
     * @return Returns formatted String for the terminal.
     */
    public String formatTerminalChat(IncomingMessageEvent event) {
        String username = event.data.userName;      /* Set the username from the message username. */
        String usernameFormat = "";                 /* Contains the ANSI escape sequences to color the username. */
        String message = "";                        /* Contains the chat message. */

        /*
         * Setup the date formatter to be displayed.
         */
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        /*
         * Color the username based on the user role in chat.
         */
        if (event.data.userRoles.contains(MixerUser.Role.MOD)) {
            usernameFormat = ConsoleColors.GREEN_BOLD_BRIGHT;
        } else if (event.data.userRoles.contains(MixerUser.Role.PRO)) {
            usernameFormat = ConsoleColors.PURPLE_BOLD_BRIGHT;
        } else if (event.data.userRoles.contains(MixerUser.Role.USER)) {
            usernameFormat = ConsoleColors.CYAN_BOLD_BRIGHT;
        } else if (event.data.userRoles.contains(MixerUser.Role.OWNER)) {
            usernameFormat = ConsoleColors.WHITE_BOLD_BRIGHT;
        } else if (event.data.userRoles.contains(MixerUser.Role.FOUNDER)) {
            usernameFormat = ConsoleColors.RED_BOLD_BRIGHT;
        } else if (event.data.userRoles.contains(MixerUser.Role.STAFF)) {
            usernameFormat = ConsoleColors.YELLOW_BOLD_BRIGHT;
        } else if (event.data.userRoles.contains(MixerUser.Role.GLOBAL_MOD)) {
            usernameFormat = ConsoleColors.BLUE_BOLD_BRIGHT;
        }

        /*
         * Add each message component to the message string.
         */
        for (MessageTextComponent m : event.data.message.message) {
            String format = "";
            if (m.text.contains("\u0040")) {
                format = ConsoleColors.BLUE_UNDERLINED;
            }
            message = String.format("%s%s%s%s", format, message, m.text, ConsoleColors.RESET);
        }

        /*
         * Put all the elements together.
         */
        return String.format("%s %s%s%s: %s", formatter.format(date), usernameFormat, username,
                ConsoleColors.RESET, message);
    }


    //endregion

    //region Api methods.
    /**
     * Send a message to chat.getHTML
     *
     * @param message The message to be sent.
     */
    public void sendMessage(String message) {
        if (message.startsWith("/")) {
            String command = message.split(" ")[0];
            if ("/whisper".contains(command)) {
                sendWhisper(message);
            }
            else if ("/clear".contains(command)) {
                //mixChatSocket.clear();
                chatConnectible.send(ClearMessagesMethod.of());
            }
        } else
            chatConnectible.send(ChatSendMethod.of(message));
    }

    /**
     * Delete message from chatBox using uuid from DeleteMessageEvent.
     *
     * @param event DeleteMessageEvent that contains the uuid of the message to delete.
     */
    public void deleteMessage(DeleteMessageEvent event) {
        for (Node messageNode : chatBox.getChildren()) {
            if (messageNode instanceof MixMessage) {
                MixMessage message = (MixMessage) messageNode;
                if (message.getUuid().equals(event.data.id.toString())) {
                    Platform.runLater(() -> message.setStrikethrough(true));
                }
            }
        }
    }

    /**
     * Send delete message uuid to the Mixer API.
     *
     * @param uuid Id of the message to be deleted.
     */
    public void deleteMessage(String uuid) {
//        try {
//            URL url = new URL("https://mixer.com/api/v1/chats/" + chatId);      /* The url of the chat endpoint. */
//            String authkey = getAuthkey(url);                                      /* Get the authkey to be used to authenticate with the API. */
//
//            /*
//             * Create a new MixSocket where when successfully authenticated delete the message.
//             */
//            socket = getSocket(url, new MixSocketReply() {
//                @Override
//                public void onReply(JSONObject reply) {
//                    System.out.println(reply);
//                    if (reply.has("data")) {
//                        Object data = reply.get("data");
//                        if (data instanceof JSONObject) {
//                            JSONObject jData = (JSONObject) data;
//                            if (jData.has("authenticated")) {
//                                if (jData.getBoolean("authenticated")) {
//                                    socketDelete(socket, uuid);
//                                }
//                            }
//                        }
//                    }
//                }
//            });
//            socketAuth(socket, authkey, chatId, userId);    /* Authenticate with the API and delete the message if successful. */
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //mixChatSocket.delete(uuid);
        chatConnectible.send(DeleteMessageMethod.of(uuid));
    }

    /**
     * Checks if the current user can delete a specific message.
     *
     * @param uuid uuid of the selected message.
     * @return Returns if the current user is able to delete the message.
     */
    public boolean canDeleteMessage(String uuid) {
        for (Node messageNode : chatBox.getChildren()) {
            if (messageNode instanceof MixMessage) {
                MixMessage message = (MixMessage) messageNode;
                if (message.getUuid().equals(uuid)) {
                    if (message.getUser().equals(currentUser.getUserName())) {
                        return true;
                    }
                }
            }
        }
        return hasPermissions;
    }

    private void sendWhisper(String message) {
        String[] messageComponents = message.split(" ");
        if (messageComponents.length < 3 || !messageComponents[1].startsWith("@")) {
            System.out.println("Invalid whisper.");
        } else {
            /*
             * Format the whisper to send.
             */
            String rebuiltMessage = "";

            for (int i = 2; i < messageComponents.length; i++) {
                rebuiltMessage = rebuiltMessage + messageComponents[i] + " ";
            }

            messageComponents[1] = messageComponents[1].substring(1);

            MixerUser user = new MixerUser();
            user.username = messageComponents[1];

            /*
             * Format message to show on the client.
             */
            List<Node> messages = new LinkedList<>();

            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            Text dateText = new Text(formatter.format(date));

            // Send the whisper.
            chatConnectible.send(WhisperMethod.builder().send(rebuiltMessage).to(user).build());

            Text toUsername = new Text(String.format("%s: ", messageComponents[1]));
            Text fromUsername = new Text(String.format(" %s", mixerUsername));
            Text separator = new Text(" > ");
            Text rebuiltMessageText = new Text(rebuiltMessage);

            toUsername.setFill(getRoleColor(getUserRole(messageComponents[1])));
            fromUsername.setFill(getRoleColor(getUserRole(mixerUsername)));

            fromUsername.setFont(Font.font("Verdana", FontPosture.ITALIC, 12));
            toUsername.setFont(Font.font("Verdana", FontPosture.ITALIC, 12));
            rebuiltMessageText.setFont(Font.font("Verdana", FontPosture.ITALIC, 12));

            messages.add(dateText);
            messages.add(fromUsername);
            messages.add(separator);
            messages.add(toUsername);
            messages.add(rebuiltMessageText);
            updateText(messages);
        }
    }

    private MixerUser.Role getUserRole(String username) {
        JSONObject reply = getUserInChat(chatId, username);
        if (reply.length() == 0) {
            System.out.println("User is not in chat");
            return MixerUser.Role.USER;
        }
        JSONArray replyRoleList = reply.getJSONArray("userRoles");

        List<MixerUser.Role> roleList = new LinkedList<>();

        for (int i = 0; i < replyRoleList.length(); i++) {
            String role = replyRoleList.getString(i);
            roleList.add(MixerUser.Role.valueOf(role.toUpperCase()));
        }

        MixChatUser mixChatUser = null;
        try {
            mixChatUser = new MixChatUser(getUserId(username), username, roleList);
        } catch (Exception e) {
            System.out.println("Unable to send a whisper.  User is not in the chat");
            return MixerUser.Role.USER; // Return if the whisper user is not in chat.
        }

        return mixChatUser.getPrimaryRole();
    }

    private Color getRoleColor(MixerUser.Role role) {
        switch (role) {
            case MOD:
                return Color.GREEN;
            case PRO:
                return Color.DEEPPINK;
            case USER:
                return Color.SKYBLUE;
            case OWNER:
                return Color.BLACK;
            case FOUNDER:
                return Color.RED;
            case STAFF:
                return Color.GOLD;
            case SUBSCRIBER:
                return Color.TEAL;
            default:
                return Color.BLACK;
        }
    }
    //endregion

    //region Register events.
    /**
     * Register incoming chat messages as well as user join and leave events.
     */
    public void registerIncomingChat() {
        /* On IncomingMessageEvent update the chatBox and the terminal with the incoming message and update the users in the chat. */
        chatConnectible.on(IncomingMessageEvent.class, mEvent -> {
            String output = formatTerminalChat(mEvent);
            System.out.println(output);
            updateText(formatChatBox(mEvent));
        });
        /* On DeleteMessageEvent remove the message from chatBox with the uuid from dEvent */
        chatConnectible.on(DeleteMessageEvent.class, dEvent -> {
            deleteMessage(dEvent);
        });
        /* On UserJoinEvent update the users in chat. */
        chatConnectible.on(UserJoinEvent.class, jEvent -> {
            addUser(new MixChatUser(Integer.parseInt(jEvent.data.id), jEvent.data.username, jEvent.data.roles), false, true);
            userListTimer.cancel();
            userListTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateUsers(chatId);
                }
            }, 300000, 300000);
        });
        /* On UserLeaveEvent update the users in chat. */
        chatConnectible.on(UserLeaveEvent.class, lEvent -> {
            delUser(Integer.parseInt(lEvent.data.id), false);
            userListTimer.cancel();
            userListTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateUsers(chatId);
                }
            }, 300000, 300000);
        });
        chatConnectible.on(ClearMessagesEvent.class, cEvent -> {
            System.out.println("CLEAR!");
        });

        userListTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateUsers(chatId);
            }
        }, 300000, 300000);
    }

    private void registerConstellation() {
        constellationConnectable.on(HelloEvent.class, hEvent -> {

        });
        constellationConnectable.on(LiveEvent.class, lEvent -> {
            parseLiveEvent(lEvent);
        });
    }

    private void parseLiveEvent(LiveEvent event) {
        if (event.data.payload.has("online")) {
            if (event.data.payload.get("online").getAsBoolean()) {
                Platform.runLater(() -> chatController.setLive(true));
            }
            else {
                Platform.runLater(() -> chatController.setLive(false));

            }
        }
    }
    //endregion

    //region Socket.
    private void handleSocketReply(JSONObject reply) {
        System.out.println(reply);
        if (reply.has("type")) {
            switch (reply.get("type").toString()) {
                case "event":
                    switch (reply.get("event").toString()) {
                        case "ClearMessages":
                            handleClearMessages(reply);
                            break;

                    }
                    break;
            }
        }
    }

    private void handleClearMessages(JSONObject reply) {
        JSONObject data = reply.getJSONObject("data");

    }
    //endregion

    //region User list.
    /**
     * Update user
     */
    public void updateUserList() {
        /*
         * Lists of Text that contain the users in their respective primary roles.
         */
        Text owner = new Text();
        List<Text> founder = new LinkedList<>();
        List<Text> staff = new LinkedList<>();
        List<Text> globalMod = new LinkedList<>();
        List<Text> sub = new LinkedList<>();
        List<Text> pro = new LinkedList<>();
        List<Text> mod = new LinkedList<>();
        List<Text> user = new LinkedList<>();

        /*
         * For each user in users add that user to their primary list
         */
        for (MixChatUser u : users) {
            switch (u.getPrimaryRole()) {
                case OWNER:
                    owner = new MixChatUserText(u.getUserName(), u.getId());
                    break;
                case FOUNDER: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    founder.add(t);
                    break;
                }
                case STAFF: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    staff.add(t);
                    break;
                }
                case GLOBAL_MOD: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    globalMod.add(t);
                    break;
                }
                case MOD: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    mod.add(t);
                    break;
                }
                case SUBSCRIBER: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    sub.add(t);
                    break;
                }
                case PRO: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    pro.add(t);
                    break;
                }
                case USER: {
                    MixChatUserText t = new MixChatUserText(u.getUserName(), u.getId());
                    t.setFill(u.getColor());
                    user.add(t);
                    break;
                }
            }
        }

        // TODO add function to simplify code.
        /*
         * Add the users to userList.
         */
        Text finalOwner = owner;
        List<Node> newUserList = new LinkedList<>();
        newUserList.clear();
        if (finalOwner != new Text()) {
            newUserList.add(new MixChatUserText("OWNER", true, MixerUser.Role.OWNER));
            newUserList.add(new Text(System.lineSeparator()));
            newUserList.add(finalOwner);
            newUserList.add(new Text(System.lineSeparator()));
        }
        if (founder != null && !founder.isEmpty()) {
            newUserList.add(new MixChatUserText("FOUNDER", true, MixerUser.Role.FOUNDER));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : founder) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }
        if (staff != null && !staff.isEmpty()) {
            newUserList.add(new MixChatUserText("STAFF", true, MixerUser.Role.STAFF));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : staff) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }
        if (globalMod != null && !globalMod.isEmpty()) {
            newUserList.add(new MixChatUserText("GLOBAL MOD", true, MixerUser.Role.GLOBAL_MOD));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : globalMod) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }
        if (mod != null && !mod.isEmpty()) {
            newUserList.add(new MixChatUserText("MOD", true, MixerUser.Role.MOD));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : mod) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }
        if (sub != null && !sub.isEmpty()) {
            newUserList.add(new MixChatUserText("SUBSCRIBER", true, MixerUser.Role.SUBSCRIBER));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : sub) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }
        if (pro != null && !pro.isEmpty()) {
            newUserList.add(new MixChatUserText("PRO", true, MixerUser.Role.PRO));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : pro) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }
        if (user != null && !user.isEmpty()) {
            newUserList.add(new MixChatUserText("USER", true, MixerUser.Role.USER));
            newUserList.add(new Text(System.lineSeparator()));
            for (Text u : user) {
                newUserList.add(u);
                newUserList.add(new Text(System.lineSeparator()));
            }
        }

        Platform.runLater(() -> userList.getChildren().setAll(newUserList));
    }

    // TODO Use Mixer library to get list of users.

    /**
     * Update users from Mixer API chats/(id)/users.
     *
     * @param id Id of the channel to get users from.
     */
    public void updateUsers(int id) {
        try {
            /*
             * Get json array of users.
             */
            //String result = getHTML(String.format("https://mixer.com/api/v1/chats/%d/users", id));
            //JSONArray c = new JSONArray(result);
            JSONArray c = getUserList(String.format("https://mixer.com/api/v1/chats/%d/users?limit=100", id));

            /*
             * Iterate through array of users and add them to the list of users.
             */
            for (int i = 0; i < c.length(); i++) {
                JSONObject j = c.getJSONObject(i);
                int userId = j.getInt("userId");
                String name = j.getString("username");
                List<MixerUser.Role> userRoles = new LinkedList<>();
                JSONArray roles = j.getJSONArray("userRoles");

                /*
                 * Iterate through list of roles and add them to userRoles.
                 */
                for (int k = 0; k < roles.length(); k++) {
                    switch (roles.getString(k)) {
                        case "Owner":
                            userRoles.add(MixerUser.Role.OWNER);
                            break;
                        case "User":
                            userRoles.add(MixerUser.Role.USER);
                            break;
                        case "Mod":
                            userRoles.add(MixerUser.Role.MOD);
                            break;
                        case "Founder":
                            userRoles.add(MixerUser.Role.FOUNDER);
                            break;
                        case "Pro":
                            userRoles.add(MixerUser.Role.PRO);
                            break;
                        case "Global_mod":
                            userRoles.add(MixerUser.Role.GLOBAL_MOD);
                            break;
                        case "Staff":
                            userRoles.add(MixerUser.Role.STAFF);
                            break;
                        case "Subscriber":
                            userRoles.add(MixerUser.Role.SUBSCRIBER);
                            break;
                    }
                }
                /* Add the new user to the list of users. */
                addUser(new MixChatUser(userId, name, userRoles), false, false);
            }

            updateUserList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONArray getUserList(String url) {
        JSONArray result = null;
        try {
            result = new JSONArray(getHTML(url));
            String cont = getHTMLHeaderLink(url);
            if (cont != null) {
                if (cont.contains("continuationToken") && !url.contains(cont)) {
                    String requiredString = cont.substring(cont.indexOf("<") + 1, cont.indexOf(">"));
                    JSONArray jsonArray = getUserList(String.format("https://mixer.com%s", requiredString));
                    try {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            result.put(jsonObject);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == null)
            return new JSONArray();
        return result;
    }

    public JSONObject getUserInChat(int id, String username) {
        JSONObject result = new JSONObject();
        try {
            result = new JSONObject(getHTML(String.format("https://mixer.com/api/v2/chats/%d/users/%d", id, getUserId(username))));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public int getUserId(String username) throws Exception {
        JSONObject userId = new JSONObject(getHTML(String.format("https://mixer.com/api/v1/channels/%s?fields=userId", username)));
        return userId.getInt("userId");
    }

    /**
     * Check if user is already in users and if not add user to the list of users, users.
     *
     * @param user MixUser object to be added to the list of users
     */
    private void addUser(MixChatUser user, boolean updateList, boolean singleUpdate) {
        for (MixChatUser u : users) {
            if (u.getUserName().equals(user.getUserName())) {
                return;
            }
        }

        users.add(user);

        Collections.sort(users, new SortByUserName());

        if (updateList)
            updateUserList();
        else if (singleUpdate) {
            int start = 0;
            for (Node node : userList.getChildrenUnmodifiable()) {
                if (node instanceof MixChatUserText) {
                    if (((MixChatUserText) node).isLabel) {
                        if (((MixChatUserText) node).role == user.getPrimaryRole()) {
                            start = userList.getChildrenUnmodifiable().indexOf(node) + 1;
                            break;
                        }
                    }
                }
            }
            // If the proper label is not found, to simplify thing we just update the whole list of users.
            if (start == 0) {
                updateUserList();
            } else {
                boolean hasAdded = false;
                for (int i = start; i < userList.getChildren().size(); i++) {
                    if (userList.getChildren().get(i) instanceof MixChatUserText) {
                        if (((MixChatUserText) userList.getChildren().get(i)).isLabel) {
                            int finalI1 = i;
                            Platform.runLater(() -> {
                                MixChatUserText userText = new MixChatUserText(user.getUserName(), user.getId());
                                userText.setFill(user.getColor());
                                userList.getChildren().add(finalI1 - 1, userText);
                                userList.getChildren().add(finalI1, new Text(System.lineSeparator()));
                            });
                            hasAdded = true;
                            break;
                        } else {
                            if (((MixChatUserText) userList.getChildren().get(i)).getText().compareToIgnoreCase(user.getUserName()) > 0) {
                                List<Node> list = userList.getChildren();
                                int finalI = i;
                                Platform.runLater(() -> {
                                    MixChatUserText userText = new MixChatUserText(user.getUserName(), user.getId());
                                    userText.setFill(user.getColor());
                                    userList.getChildren().add(finalI, userText);
                                    userList.getChildren().add(finalI + 1, new Text(System.lineSeparator()));
                                });
                                hasAdded = true;
                                break;
                            }
                        }
                    }
                }
                if (!hasAdded) {
                    Platform.runLater(() -> {
                        MixChatUserText userText = new MixChatUserText(user.getUserName(), user.getId());
                        userText.setFill(user.getColor());
                        userList.getChildren().add(userText);
                        userList.getChildren().add(new Text(System.lineSeparator()));
                    });
                }
            }
        }
    }

    private void delUser(int id, boolean updateList) {
        for (MixChatUser u : users) {
            if (u.getId() == id) {
                users.remove(u);
                break;
            }
        }

        if (updateList)
            updateUserList();
        else {
            for (Node user : userList.getChildren()) {
                if (user instanceof MixChatUserText) {
                    if (((MixChatUserText) user).id == id) {
                        Platform.runLater(() -> {
                            int index = userList.getChildren().indexOf(user);
                            userList.getChildren().remove(index);
                            userList.getChildren().remove(index);
                            if (userList.getChildren().get(index - 2) instanceof MixChatUserText) {
                                if (((MixChatUserText) (userList.getChildren().get(index - 2))).isLabel) {
                                    userList.getChildren().remove(index - 1);
                                    userList.getChildren().remove(index - 2);
                                }
                            }
                        });
                    }
                }
            }
        }
    }
    //endregion

    //region Get HTML.
    /**
     * Used to send GET over http.
     *
     * @param urlToRead Url to send the GET to.
     * @return Returns the response from the GET.
     * @throws Exception
     */
    public String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    public String getHTMLHeaderLink(String urlToRead) throws Exception {
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return conn.getHeaderField("link");
    }
    //endregion

    /**
     * Checks whether or not the chat is currently connected.
     *
     * @return Connected.
     */
    public boolean isConnected() {
        return connected;
    }

    public String getMixerUsername() {
        return mixerUsername;
    }
}

class SortByUserName implements Comparator<MixChatUser> {
    public int compare(MixChatUser a, MixChatUser b) {
        return a.getUserName().compareToIgnoreCase(b.getUserName());
    }
}
