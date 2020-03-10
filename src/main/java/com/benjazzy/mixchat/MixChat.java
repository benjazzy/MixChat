package com.benjazzy.mixchat;

import com.benjazzy.mixchat.helper.ConsoleColors;
import com.benjazzy.mixchat.oauth.MixOauth;
import com.mixer.api.MixerAPI;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.channel.MixerChannel;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.DeleteMessageEvent;
import com.mixer.api.resource.chat.events.IncomingMessageEvent;
import com.mixer.api.resource.chat.events.UserJoinEvent;
import com.mixer.api.resource.chat.events.UserLeaveEvent;
import com.mixer.api.resource.chat.events.data.IncomingMessageData;
import com.mixer.api.resource.chat.events.data.MessageComponent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.methods.GetHistoryMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ChatHistoryReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.services.impl.ChannelsService;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * The MixChat class handles communication with the Mixer API
 * and displays the chat to the terminal and javafx textflow chatBox.
 */
public class MixChat {


    private boolean connected = false;                      /** The connected variable is used to store whether the chat is connected. */
    private String token = "";                              /** The token variable is used to store the Oauth2 token. */
    private String mixerUsername = "";                      /** Username of the currently connected user. */
    private int chatId = 0;                                 /** ChatId stores the id of the currently connected chat. */
    private int userId = 0;                                 /** UserId stores the id of the current user. */
    private List<MixChatUser> users = new LinkedList<>();   /** Users stores a list of users currently connected to the chat. */
    private Timer userListTimer = new Timer();

    private TextFlow chatBox;                               /** ChatBox is the TextFlow where the chat is displayed. */
    private TextFlow userList;                              /** UserList is the TextFlow where the list of users from users is displayed. */

    /**
     * The only reason for chatScrollPane's existence is so that it can be set to always scroll to the bottom.
     * ChatBox is contained inside chatScrollPane.
     */
    private ScrollPane chatScrollPane;

    private MixerAPI mixer;                         /** Mixer stores the main MixerAPI object */
    private MixerChatConnectable chatConnectible;   /** ChatConnectible is used interface with the connected chat. */
    private MixSocket socket;                       /** Used to manually interface with the Mixer API */

    /**
     * The constructor links the javafx variables to their Panes.
     *
     * @param chat
     * @param users
     * @param chatPane
     */
    public MixChat(TextFlow chat, TextFlow users, ScrollPane chatPane) {
        System.out.println("Setting chatBox");
        chatBox = chat;
        userList = users;
        chatScrollPane = chatPane;

        /** Sets chatScrollPane to always scroll to the bottom */
        chatScrollPane.vvalueProperty().bind(chatBox.heightProperty());
    }

    /**
     * Connects to the specified Mixer chat.
     *
     * @param chatName                  Name of the channel that is being connected to.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void connect(String chatName) throws InterruptedException, ExecutionException {
        System.out.println(chatName);

        Platform.runLater(() -> chatBox.getChildren().clear());      /** Clears the chat for the next connection. */

        /** Gets an Oauth2 access token from MixOauth. */
        MixOauth oauth = new MixOauth();
        token = oauth.getAccessToken();

        /** Authenticates with Mixer using the Oauth2 token. */
        mixer = new MixerAPI("3721d6b1332a6db44a22ab5b71ae8e34ae187ee995b38f1a", token);
        int id = 0;                         /** Stores the chat id. */

        // TODO replace with mixer.use().findOneByToken(chatName).get();
        /**
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

        /**
         * Set the user, chat, and channel objects from channelName.
         */
        MixerUser user = mixer.use(UsersService.class).getCurrent().get();
        MixerChat chat = mixer.use(ChatService.class).findOne(id).get();
        MixerChannel channel = mixer.use(ChannelsService.class).findOneByToken(chatName).get();

        mixerUsername = user.username;
        userId = user.id;


        /** Set chatConnectable to use the current chat. */
        chatConnectible = chat.connectable(mixer);
        /**
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

                            /** Get previous 50 messages and update the terminal and chatBox */
                            chatConnectible.send(GetHistoryMethod.forCount(50), new ReplyHandler<ChatHistoryReply>() {
                                @Override
                                public void onSuccess(@Nullable ChatHistoryReply result) {
                                    updateText(formatChatBox(result));
                                    System.out.println(formatTerminalChat(result));
                                }
                            });
                        }

                        public void onFailure(Throwable var1) {
                            var1.printStackTrace();
                        }
                    });
            /** Updates the list of current viewers. */
            new Thread(() -> updateUsers(chatId)).start();
        } else {
            System.out.println("Failed to connect");
        }

        /** Registers events for incoming messages as well as user join and leave */
        new Thread(() -> registerIncomingChat()).start();
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

    /**
     * Format the text from an incoming message.
     *
     * @param event     Incoming message event that contains all the data of the message.
     * @return          Returns a list of formatted text objects to be displayed in the chatBox.
     */
    public List<Node> formatChatBox(IncomingMessageEvent event) {
        List<Node> textList = new ArrayList<>();                            /** List of Text objects to be returned. */
        MixMessage username = new MixMessage(event.data.userName, event.data.id, event.data.userName); /** Username of the user that sent the message. */
        List<Node> message = new LinkedList<>();                            /** List of message elements from event. */

        /**
         * Formats the current time to be added to textList.
         */
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        MixMessage dateText = new MixMessage(formatter.format(date), event.data.id, event.data.userName);

        /**
         * Iterates the message elements and adds them to the message list.
         * Also checks if the message element contains an @ symbol and highlights it blue and adds
         * an underline if so.
         */
        for (MessageTextComponent i : event.data.message.message)
        {
            message.addAll(formatMessageComponent(i, event.data));
        }

        /**
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
            username.setFill(Color.YELLOW);
        } else if (event.data.userRoles.contains(MixerUser.Role.GLOBAL_MOD)) {
            username.setFill(Color.TEAL);
        }

        /** Adds spaces between the elements and adds a colon after the username. */
        username.setText(String.format(" %s: ", username.getText()));

        /**
         * Adds together all the elements into one text list.
         */
        textList.add(dateText);
        textList.add(username);
        for (Node m : message) {
            textList.add(m);
        }

        return textList;
    }

    /**
     * Format the messages from previous messages.
     *
     * @param event     Contains all the information on the past messages.
     * @return          Returns a formatted list of messages to be displayed in chatBox.
     */
    public List<Node> formatChatBox(ChatHistoryReply event) {
        /** List of formatted Text objects to be returned. */
        List<Node> textList = new ArrayList<>();

        /**
         * Iterate through each message and format it.
         */
        for (IncomingMessageData messageEvent : event.messages) {
            /** Add a new line to the beginning to separate it from the previous line. */
            textList.add(new MixMessage(System.lineSeparator(), messageEvent.id, messageEvent.userName));
            /** Gets the username from messageEvent. */
            MixMessage username = new MixMessage(messageEvent.userName, messageEvent.id, messageEvent.userName);
            /** List of message elements from event. */
            List<Node> message = new LinkedList<>();

            /**
             * Iterate through message elements and add them to the message text.
             */
            for (MessageTextComponent i : messageEvent.message.message) {
                message.addAll(formatMessageComponent(i, messageEvent));
            }

            /**
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
                username.setFill(Color.YELLOW);
            } else if (messageEvent.userRoles.contains(MixerUser.Role.GLOBAL_MOD)) {
                username.setFill(Color.TEAL);
            }

            /** Adds spaces between the elements and adds a colon after the username. */
            username.setText(String.format(" %s: ", username.getText()));

            /**
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
     * @param event
     * @return
     */
    private List<Node> formatMessageComponent(MessageTextComponent textComponent, IncomingMessageData event)
    {
        List<Node> message = new LinkedList<>();

        // Link.
        if (textComponent.type.name().equals("LINK"))
        {
            Hyperlink m = new Hyperlink(textComponent.text);
            m.setOnAction(actionEvent -> {
                if (Desktop.isDesktopSupported())
                {
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
        else if (textComponent.url != null)
        {
            try {
                message.add(new ImageView(new Image(textComponent.url, 50, 50, false, false)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Emote.
        else if (textComponent.type.name().equals("EMOTICON"))
        {
            Image image;
            if (textComponent.source.equals("builtin"))
            {
                image = new Image(String.format("https://mixer.com/_latest/emoticons/%s.png", textComponent.pack));
            }
            else
            {
                image = new Image(textComponent.pack);
            }
            PixelReader reader = image.getPixelReader();
            message.add(new ImageView(new WritableImage(reader, textComponent.coords.x, textComponent.coords.y, 24, 24)));
        }
        // Tag(@)
        else if (textComponent.type.name().equals("TAG"))
        {
            MixMessage m = new MixMessage(textComponent.text, event.id, event.userName);
            m.setFill(Color.BLUE);
            message.add(m);
        }
        // Text.
        else {
            MixMessage m = new MixMessage(textComponent.text, event.id, event.userName);
            message.add(m);
        }
        return message;
    }

    /**
     * Formats messages from previous messages for the terminal.
     *
     * @param event     Contains previous messages.
     * @return          Returns a formatted String for the terminal.
     */
    public String formatTerminalChat(ChatHistoryReply event) {
        /** String r is what is returned by the function. */
        String r = "";

        /**
         * Iterate through each previous message and format it.
         */
        for (IncomingMessageData m : event.messages) {
            String username = m.userName;       /** Set the username from the message username. */
            String usernameFormat = "";         /** Contains the ANSI escape sequences to color the username. */
            String message = "";                /** Contains the chat message. */

            /**
             * Setup the date formatter to be displayed.
             */
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();

            /**
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

            /**
             * Add each message component to the message string.
             */
            for (MessageTextComponent i : m.message.message) {
                String format = "";
                if (i.text.contains("\u0040")) {
                    format = ConsoleColors.BLUE_UNDERLINED;
                }
                message = String.format("%s%s%s%s", format, message, i.text, ConsoleColors.RESET);
            }

            /**
             * Put all the elements together and add a newline.
             */
            String output = String.format("%s %s%s%s: %s", formatter.format(date), usernameFormat, username,
                    ConsoleColors.RESET, message);
            r = String.format("%s\n%s",r ,output);
        }
        return r;
    }

    /**
     * Formats messages from an incoming message for the terminal.
     *
     * @param event     Contains incoming message.
     * @return          Returns formatted String for the terminal.
     */
    public String formatTerminalChat(IncomingMessageEvent event) {
        String username = event.data.userName;      /** Set the username from the message username. */
        String usernameFormat = "";                 /** Contains the ANSI escape sequences to color the username. */
        String message = "";                        /** Contains the chat message. */

        /**
         * Setup the date formatter to be displayed.
         */
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        /**
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

        /**
         * Add each message component to the message string.
         */
        for (MessageTextComponent m : event.data.message.message) {
            String format = "";
            if (m.text.contains("\u0040")) {
                format = ConsoleColors.BLUE_UNDERLINED;
            }
            message = String.format("%s%s%s%s", format, message, m.text, ConsoleColors.RESET);
        }

        /**
         * Put all the elements together.
         */
        return String.format("%s %s%s%s: %s", formatter.format(date), usernameFormat, username,
                ConsoleColors.RESET, message);
    }

    /**
     * Send a message to chat.getHTML
     *
     * @param message   The message to be sent.
     */
    public void sendMessage(String message) {
        chatConnectible.send(ChatSendMethod.of(message));
    }

    /**
     * Register incoming chat messages as well as user join and leave events.
     */
    public void registerIncomingChat() {
        /** On IncomingMessageEvent update the chatBox and the terminal with the incoming message and update the users in the chat. */
        chatConnectible.on(IncomingMessageEvent.class, mEvent -> {
            String output = formatTerminalChat(mEvent);
            System.out.println(output);
            updateText(formatChatBox(mEvent));
        });
        /** On DeleteMessageEvent remove the message from chatBox with the uuid from dEvent */
        chatConnectible.on(DeleteMessageEvent.class, dEvent -> {
            deleteMessage(dEvent);
        });
        /** On UserJoinEvent update the users in chat. */
        chatConnectible.on(UserJoinEvent.class, jEvent -> {
                addUser(new MixChatUser(Integer.parseInt(jEvent.data.id), jEvent.data.username, jEvent.data.roles), true);
                userListTimer.cancel();
                userListTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        updateUsers(chatId);
                    }
                }, 300000, 300000);
        });
        /** On UserLeaveEvent update the users in chat. */
        chatConnectible.on(UserLeaveEvent.class, lEvent -> {
            delUser(Integer.parseInt(lEvent.data.id), true);
            userListTimer.cancel();
            userListTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateUsers(chatId);
                }
            }, 300000, 300000);
        });

        userListTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateUsers(chatId);
            }
        }, 300000, 300000);
    }

    /**
     * Delete message from chatBox using uuid from DeleteMessageEvent.
     *
     * @param event     DeleteMessageEvent that contains the uuid of the message to delete.
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
     * @param uuid  Id of the message to be deleted.
     */
    public void deleteMessage(String uuid) {
        try {
            URL url = new URL("https://mixer.com/api/v1/chats/" + chatId);      /** The url of the chat endpoint. */
            String authkey = getAuthkey(url);                                      /** Get the authkey to be used to authenticate with the API. */

            /**
             * Create a new MixSocket where when successfully authenticated delete the message.
             */
            socket = getSocket(url, new MixSocketReply() {
                @Override
                public void onReply(JSONObject reply) {
                    if (reply.has("data")) {
                        Object data = reply.get("data");
                        if (data instanceof JSONObject) {
                            JSONObject jData = (JSONObject) data;
                            if (jData.has("authenticated")) {
                                if (jData.getBoolean("authenticated")) {
                                    socketDelete(socket, uuid);
                                }
                            }
                        }
                    }
                }
            });
            socketAuth(socket, authkey, chatId, userId);    /** Authenticate with the API and delete the message if successful. */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the authkey from the specified endpoint.
     *
     * @param url           Url of the endpoint to get the authkey from.  https://mixer.com/api/v1/chats/{chatid}.
     * @return              Returns the authkey from the endpoint.
     * @throws IOException
     */
    private String getAuthkey(URL url) throws IOException {
        MixOauth oauth = new MixOauth();                                    /** Get a hold of the Oauth token. */

        /**
         * Make http GET request to url and read it into response.
         */
        // Sending get request
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Authorization","Bearer "+ oauth.getAccessToken());
        //e.g. bearer token= eyJhbGciOiXXXzUxMiJ9.eyJzdWIiOiPyc2hhcm1hQHBsdW1zbGljZS5jb206OjE6OjkwIiwiZXhwIjoxNTM3MzQyNTIxLCJpYXQiOjE1MzY3Mzc3MjF9.O33zP2l_0eDNfcqSQz29jUGJC-_THYsXllrmkFnk85dNRbAw66dyEKBP5dVcFUuNTA8zhA83kk3Y41_qZYx43T

        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestMethod("GET");


        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;

        StringBuffer response = new StringBuffer();
        while ((output = in.readLine()) != null) {
            response.append(output);
        }

        in.close();

        /**
         * Parse response as json and return authkey key.
         */
        JSONObject obj = new JSONObject(response.toString());
        return obj.getString("authkey");
    }

    /**
     * Get the websocket endpoint from the specified http endpoint.  https://mixer.com/api/v1/chats/{chatid}.
     *
     * @param url           Url of the endpoint to get the websocket endpoint from.
     * @return              The websocket endpoint from the http endpoint.
     * @throws IOException
     */
    private String getEndpoints(URL url) throws IOException {
        MixOauth oauth = new MixOauth();                    /** Get a hold of the Oauth token. */

        /**
         * Make http GET request to url and read it into response.
         */
        // Sending get request
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Authorization","Bearer "+ oauth.getAccessToken());
        //e.g. bearer token= eyJhbGciOiXXXzUxMiJ9.eyJzdWIiOiPyc2hhcm1hQHBsdW1zbGljZS5jb206OjE6OjkwIiwiZXhwIjoxNTM3MzQyNTIxLCJpYXQiOjE1MzY3Mzc3MjF9.O33zP2l_0eDNfcqSQz29jUGJC-_THYsXllrmkFnk85dNRbAw66dyEKBP5dVcFUuNTA8zhA83kk3Y41_qZYx43T

        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestMethod("GET");


        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;

        StringBuffer response = new StringBuffer();
        while ((output = in.readLine()) != null) {
            response.append(output);
        }

        in.close();

        /**
         * Parse response as json and return first element in endpoints array.
         */
        JSONObject obj = new JSONObject(response.toString());
        JSONArray array = obj.getJSONArray("endpoints");
        return array.getString(0);
    }

    /**
     * Gets a new ssl websocket
     *
     * @param url       Websocket endpoint and port to connect to.
     * @param reply     MixSocketReply to reply to.
     * @return          Returns a MixSocket that can send messages to the Mixer API.
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    private MixSocket getSocket(URL url, MixSocketReply reply) throws KeyManagementException, NoSuchAlgorithmException, IOException, URISyntaxException, InterruptedException {
        MixSocket socket = new MixSocket(new URI(getEndpoints(url)), reply);    /** Create new MixSocket using url and MixSocketReply reply. */

        /**
         * Setup Socket to user ssl.
         */
        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( null, null, null );

        SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();

        socket.setSocketFactory( factory );

        socket.connectBlocking();   /** Connect to socket and block until connected. */

        return socket;
    }

    /**
     * Send the auth message to specified websocket.
     *
     * @param socket        Websocket to send the auth message over.
     * @param authkey       Authkey from getAuthkey() to pass with auth message.
     * @param channelID     ID of the channel to authenticate with.
     * @param userID        ID of the user to authenticate as.
     */
    private void socketAuth(MixSocket socket, String authkey, int channelID, int userID) {
        /**
         * Format json request int JSONObject
         */
        JSONObject auth = new JSONObject("{\n" +
                "  \"type\": \"method\",\n" +
                "  \"method\": \"auth\",\n" +
                "  \"arguments\": [" + channelID + ", " + userID +", \"" + authkey + "\"],\n" +
                "  \"id\": 0\n" +
                "}");
        socket.send(auth.toString());
    }

    /**
     * Send the delete message message over the websocket.  Must send the auth message first.
     *
     * @param socket    Websocket to send the delete message over.
     * @param uuid      UUID of the message to be deleted.
     */
    private void socketDelete(MixSocket socket, String uuid) {
        JSONObject deleteKey = new JSONObject("{\n" +
                "  \"type\": \"method\",\n" +
                "  \"method\": \"deleteMessage\",\n" +
                "  \"arguments\": [\"" + uuid + "\"],\n" +
                "  \"id\": 10\n" +
                "}");

        socket.send(deleteKey.toString());
    }

    /**
     * Update the chatBox with the new messages.
     *
     * @param text  List of Text objects to be added to chatBox.
     */
    private void updateText(List<Node> text) {
        for (Node t : text) {
            Platform.runLater(() -> chatBox.getChildren().add(t));
        }
        Platform.runLater(() -> chatBox.getChildren().add(new Text(System.lineSeparator())));

    }

    /**
     * Update user
     */
    public void updateUserList() {
        /**
         * Lists of Text that contain the users in their respective primary roles.
         */
        Text owner = new Text();
        List<Text> founder = new LinkedList<>();
        List<Text> staff = new LinkedList<>();
        List<Text> globalMod = new LinkedList<>();
        List<Text> pro = new LinkedList<>();
        List<Text> mod = new LinkedList<>();
        List<Text> user = new LinkedList<>();

        /**
         * For each user in users add that user to their primary list
         */
        for (MixChatUser u : users) {
            switch (u.getPrimaryRole()) {
                case OWNER:
                    owner = new Text(u.getUserName());
                    break;
                case FOUNDER: {
                    Text t = new Text(u.getUserName());
                    t.setFill(Color.RED);
                    founder.add(t);
                    break;
                }
                case STAFF: {
                    Text t = new Text(u.getUserName());
                    t.setFill(Color.YELLOW);
                    staff.add(t);
                    break;
                }
                case GLOBAL_MOD: {
                    Text t = new Text(u.getUserName());
                    t.setFill(Color.TEAL);
                    globalMod.add(t);
                    break;
                }
                case MOD: {
                    Text t = new Text(u.getUserName());
                    t.setFill(Color.GREEN);
                    mod.add(t);
                    break;
                }
                case PRO: {
                    Text t = new Text(u.getUserName());
                    t.setFill(Color.DEEPPINK);
                    pro.add(t);
                    break;
                }
                case USER: {
                    Text t = new Text(u.getUserName());
                    t.setFill(Color.SKYBLUE);
                    user.add(t);
                    break;
                }
            }
        }

        // TODO add function to simplify code.
        /**
         * Add the users to userList.
         */
        Text finalOwner = owner;
        Platform.runLater(() -> {
            userList.getChildren().clear();
            if (finalOwner != new Text()) {
                userList.getChildren().add(new Text("OWNER"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                userList.getChildren().add(finalOwner);
                userList.getChildren().add(new Text(System.lineSeparator()));
            }
            if (founder != null && !founder.isEmpty()) {
                userList.getChildren().add(new Text("FOUNDER"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                for (Text u : founder) {
                    userList.getChildren().add(u);
                    userList.getChildren().add(new Text(System.lineSeparator()));
                }
            }
            if (staff != null && !staff.isEmpty()) {
                userList.getChildren().add(new Text("STAFF"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                for (Text u : staff) {
                    userList.getChildren().add(u);
                    userList.getChildren().add(new Text(System.lineSeparator()));
                }
            }
            if (globalMod != null && !globalMod.isEmpty()) {
                userList.getChildren().add(new Text("GLOBAL MOD"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                for (Text u : globalMod) {
                    userList.getChildren().add(u);
                    userList.getChildren().add(new Text(System.lineSeparator()));
                }
            }
            if (mod != null && !mod.isEmpty()) {
                userList.getChildren().add(new Text("MOD"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                for (Text u : mod) {
                    userList.getChildren().add(u);
                    userList.getChildren().add(new Text(System.lineSeparator()));
                }
            }
            if (pro != null && !pro.isEmpty()) {
                userList.getChildren().add(new Text("PRO"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                for (Text u : pro) {
                    userList.getChildren().add(u);
                    userList.getChildren().add(new Text(System.lineSeparator()));
                }
            }
            if (user != null && !user.isEmpty()) {
                userList.getChildren().add(new Text("USER"));
                userList.getChildren().add(new Text(System.lineSeparator()));
                for (Text u : user) {
                    userList.getChildren().add(u);
                    userList.getChildren().add(new Text(System.lineSeparator()));
                }
            }
        });
    }

    // TODO Use Mixer library to get list of users.

    /**
     * Update users from Mixer API chats/(id)/users.
     *
     * @param id    Id of the channel to get users from.
     */
    public void updateUsers(int id) {
        try {
            /**
             * Get json array of users.
             */
            //String result = getHTML(String.format("https://mixer.com/api/v1/chats/%d/users", id));
            //JSONArray c = new JSONArray(result);
            JSONArray c = getUserList(String.format("https://mixer.com/api/v1/chats/%d/users", id));

            /**
             * Iterate through array of users and add them to the list of users.
             */
            for (int i = 0; i < c.length(); i++) {
                JSONObject j = c.getJSONObject(i);
                int userId = j.getInt("userId");
                String name = j.getString("username");
                List<MixerUser.Role> userRoles = new LinkedList<>();
                JSONArray roles = j.getJSONArray("userRoles");

                /**
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
                    }
                }
                /** Add the new user to the list of users. */
                addUser(new MixChatUser(userId, name, userRoles), false);
            }

            updateUserList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONArray getUserList(String url)
    {
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

    /**
     * Check if user is already in users and if not add user to the list of users, users.
     *
     * @param user  MixUser object to be added to the list of users
     */
    private void addUser(MixChatUser user, boolean updateList)
    {
        for (MixChatUser u : users) {
            if (u.getUserName().equals(user.getUserName())) {
                //Platform.runLater(() -> updateUserList());
                return;
            }
        }

        users.add(user);

        Collections.sort(users, new SortByUserName());

        if (updateList)
            updateUserList();
    }

    private void delUser(int id, boolean updateList)
    {
        for (MixChatUser u : users)
        {
            if (u.getId() == id)
            {
                users.remove(u);
                break;
            }
        }

        if (updateList)
            updateUserList();
    }

    /**
     * Used to send GET over http.
     *
     * @param urlToRead     Url to send the GET to.
     * @return              Returns the response from the GET.
     * @throws Exception
     */
    public String getHTML(String urlToRead) throws Exception
    {
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

    public String getHTMLHeaderLink(String urlToRead) throws Exception
    {
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return conn.getHeaderField("link");
    }

    /**
     * Checks whether or not the chat is currently connected.
     *
     * @return  Connected.
     */
	public boolean isConnected() {
		return connected;
	}

	public String getMixerUsername() {
	    return mixerUsername;
    }
}

class SortByUserName implements Comparator<MixChatUser>
{
    public int compare(MixChatUser a, MixChatUser b)
    {
        return a.getUserName().compareTo(b.getUserName());
    }
}
