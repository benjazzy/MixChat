package com.benjazzy.mixchat;

/**
 * Hello world!
 */

import com.mixer.api.MixerAPI;
import com.mixer.api.http.SortOrderMap;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.channel.MixerChannel;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.IncomingMessageEvent;
import com.mixer.api.resource.chat.events.UserJoinEvent;
import com.mixer.api.resource.chat.events.UserLeaveEvent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ChatHistoryReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.response.channels.ShowChannelsResponse;
import com.mixer.api.services.impl.ChannelsService;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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

public class MixChat {
    // private final File DATA_STORE_DIR = new
    // File(System.getProperty("user.home"), ".store/dailymotion_sample");

    private boolean connected = false;
    private String token = "";
    private int chatId = 0;

    private List<MixChatUser> users = new LinkedList<MixChatUser>();

    private TextFlow chatBox;
    private TextFlow userList;
    private ScrollPane chatScrollPane;

    private MixerAPI mixer;
    private MixerChatConnectable chatConnectable;

    MixChat(Pane root) {
        System.out.println("Setting chatBox");
        chatBox = (TextFlow) root.lookup("#ChatBox");
        userList = (TextFlow) root.lookup("#UserList");
        chatScrollPane = (ScrollPane) root.lookup("#ChatScrollPane");

        chatScrollPane.vvalueProperty().bind(chatBox.heightProperty());
    }

    private void updateText(List<Text> text) {
        for (Text t : text) {
            chatBox.getChildren().add(t);
        }
        chatBox.getChildren().add(new Text(System.lineSeparator()));

    }

    public List<Text> formatChatBox(IncomingMessageEvent event) {
        List<Text> textList = new ArrayList<Text>();
        Text username = new Text(event.data.userName);
        List<Text> message = new LinkedList<Text>();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        Text dateText = new Text(formatter.format(date));

        // Iterate through message elements and add them to the message text
        for (MessageTextComponent i : event.data.message.message) {
            Text m = new Text(i.text);
            //message.setText(String.format("%s%s", message.getText(), i.text));
            if (m.getText().contains("\u0040")) {
                m.setFill(Color.BLUE);
                m.setUnderline(true);
            }
            message.add(m);

        }

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

        username.setText(String.format(" %s: ", username.getText()));

        textList.add(dateText);
        textList.add(username);
        for (Text m : message) {
            textList.add(m);
        }

        return textList;
    }

    public String formatTerminalChat(IncomingMessageEvent event) {
        String username = event.data.userName;
        String usernameFormat = "";
        String message = "";
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

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

        for (MessageTextComponent m : event.data.message.message) {
            String format = "";
            if (m.text.contains("\u0040")) {
                format = ConsoleColors.BLUE_UNDERLINED;
            }
            message = String.format("%s%s%s%s", format, message, m.text, ConsoleColors.RESET);
        }

        String output = String.format("%s %s%s%s: %s", formatter.format(date), usernameFormat, username,
                ConsoleColors.RESET, message);
        return output;

    }


    public void connect(String chatName) throws InterruptedException, ExecutionException, IOException {
        System.out.println(chatName);
        token = "";
        MixOauth oauth = new MixOauth();
        //System.out.println(oauth.getAccessToken());
        token = oauth.getAccessToken();

        mixer = new MixerAPI("3721d6b1332a6db44a22ab5b71ae8e34ae187ee995b38f1a", token);
        //final MixerUser[] user = {mixer.use(UsersService.class).getCurrent().get()};
        int id = 0;

        String result = null;
        try {
            result = getHTML(String.format("https://mixer.com/api/v1/channels/%s?fields=id", chatName));
            JSONObject obj = new JSONObject(result);
            id = obj.getInt("id");
            chatId = id;
            //System.out.println(id);
        } catch (Exception e) {
            e.printStackTrace();
        }


        //System.out.println(user[0].channel.id);

		/*Futures.addCallback(mixer.use(UsersService.class).search(chatName), new ResponseHandler<UserSearchResponse>() {
			// Set up a handler for the response
			@Override public void onSuccess(UserSearchResponse response) {
				for (MixerUser u : response) {
					System.out.println(u.username);
					if (u.username.equals(chatName)) {
					    System.out.println(String.format("found %s", u.username));
					    id[0] = u.channel.id;
                    }
				}
			}
		});*/

        MixerUser user = mixer.use(UsersService.class).getCurrent().get();
        MixerChat chat = mixer.use(ChatService.class).findOne(id).get();
        MixerChannel channel = mixer.use(ChannelsService.class).findOneByToken(chatName).get();

        int rank = 0;

        ShowChannelsResponse channels = getChannelsPage(0, mixer);
        //System.out.println(channels.size());

        //run(0, 0, 1, mixer);

        chatConnectable = chat.connectable(mixer);

        if (chatConnectable.connect()) {

            chatConnectable.send(AuthenticateMessage.from(channel, user, chat.authkey),
                    new ReplyHandler<AuthenticationReply>() {
                        public void onSuccess(AuthenticationReply reply) {
                            //chatConnectable.send(ChatSendMethod.of("Hello World!"));
                            //sendMessage("Hello World!");
                            System.out.println("Connected");
                            connected = true;
                        }

                        public void onFailure(Throwable var1) {
                            var1.printStackTrace();
                        }
                    });

            /*chatConnectable.send(AuthenticateMessage.from(channel, user, chat.authkey),
                    new ReplyHandler<ChatHistoryReply>() {
                        public void onSuccess(ChatHistoryReply reply) {
                            //chatConnectable.send(ChatSendMethod.of("Hello World!"));
                            //sendMessage("Hello World!");
                            //System.out.println(reply.messages);
                        }

                        public void onFailure(Throwable var1) {
                            var1.printStackTrace();
                        }
                    });*/
            updateUsers(chatId);
        } else {
            System.out.println("Failed to connect");
        }
        registerIncommingChat();
    }

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

    public int run(int page, int viewers, int rank, MixerAPI mixer) throws ExecutionException, InterruptedException {
        ShowChannelsResponse channels = getChannelsPage(page, mixer);
        for (int i = 0; i < channels.size(); i++) {
            MixerChannel channel = channels.get(i);
            if (channel.viewersTotal <= viewers) {
                System.out.format("Your rank on Mixer is %d!\n", rank);
                return rank;
            }
            System.out.format("Your rank is at least %d...\n", rank);
            rank++;
        }
        return run(page + 1, viewers, rank, mixer);
    }

    public ShowChannelsResponse getChannelsPage(int page, MixerAPI mixer) throws ExecutionException, InterruptedException {
        SortOrderMap<ShowChannelsResponse.Attributes, ShowChannelsResponse.Ordering> map = new SortOrderMap<>();
        map.put(ShowChannelsResponse.Attributes.VIEWERS_TOTAL, ShowChannelsResponse.Ordering.DESCENDING);
        return mixer.use(ChannelsService.class).show(map, page, 10).get();
    }

    public void sendMessage(String message) {
        chatConnectable.send(ChatSendMethod.of(message));
    }

    public void registerIncommingChat() {
        chatConnectable.on(IncomingMessageEvent.class, mEvent -> {
            Platform.runLater(() -> {
                updateText(formatChatBox(mEvent));
                //addUser(new MixChatUser(mEvent.data.userId, mEvent.data.userName, mEvent.data.userRoles));
                updateUsers(chatId);
            });
            String output = formatTerminalChat(mEvent);
            System.out.println(output);
            if (mEvent.data.message.message.get(0).text.startsWith("!ping")) {
                chatConnectable.send(ChatSendMethod.of(String.format("@%s PONG!", mEvent.data.userName)));
            }
        });
        chatConnectable.on(UserJoinEvent.class, jEvent -> {
            Platform.runLater(() -> {
                updateUsers(chatId);
            });
        });
        chatConnectable.on(UserLeaveEvent.class, jEvent -> {
            Platform.runLater(() -> {
                updateUsers(chatId);
            });
        });
    }

    public void updateUserList() {
        Text owner = new Text();
        List<Text> founder = new LinkedList<Text>();
        List<Text> staff = new LinkedList<Text>();
        List<Text> globalMod = new LinkedList<Text>();
        List<Text> pro = new LinkedList<Text>();
        List<Text> mod = new LinkedList<Text>();
        List<Text> user = new LinkedList<Text>();
        for (MixChatUser u : users) {
            //System.out.println(String.format("%s : %s", u.getUserName(), u.getPrimaryRole()));
            if (u.getPrimaryRole() == MixerUser.Role.OWNER) {
                owner = new Text(u.getUserName());
            } else if (u.getPrimaryRole() == MixerUser.Role.FOUNDER) {
                Text t = new Text(u.getUserName());
                t.setFill(Color.RED);
                founder.add(t);
            } else if (u.getPrimaryRole() == MixerUser.Role.STAFF) {
                Text t = new Text(u.getUserName());
                t.setFill(Color.YELLOW);
                staff.add(t);
            } else if (u.getPrimaryRole() == MixerUser.Role.GLOBAL_MOD) {
                Text t = new Text(u.getUserName());
                t.setFill(Color.TEAL);
                globalMod.add(t);
            } else if (u.getPrimaryRole() == MixerUser.Role.PRO) {
                Text t = new Text(u.getUserName());
                t.setFill(Color.DEEPPINK);
                pro.add(t);
            } else if (u.getPrimaryRole() == MixerUser.Role.MOD) {
                Text t = new Text(u.getUserName());
                t.setFill(Color.GREEN);
                mod.add(t);
            } else if (u.getPrimaryRole() == MixerUser.Role.USER) {
                Text t = new Text(u.getUserName());
                t.setFill(Color.SKYBLUE);
                user.add(t);
            }
        }

        userList.getChildren().clear();
        if (owner != new Text()) {
            userList.getChildren().add(new Text("OWNER"));
            userList.getChildren().add(new Text(System.lineSeparator()));
            userList.getChildren().add(owner);
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
        if (pro != null && !pro.isEmpty()) {
            userList.getChildren().add(new Text("PRO"));
            userList.getChildren().add(new Text(System.lineSeparator()));
            for (Text u : pro) {
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
        if (user != null && !user.isEmpty()) {
            userList.getChildren().add(new Text("USER"));
            userList.getChildren().add(new Text(System.lineSeparator()));
            for (Text u : user) {
                userList.getChildren().add(u);
                userList.getChildren().add(new Text(System.lineSeparator()));
            }
        }
    }

    public void updateUsers(int id) {
        try {
            String result = getHTML(String.format("https://mixer.com/api/v1/chats/%d/users", id));

            JSONArray c = new JSONArray(result);

            for (int i = 0; i < c.length(); i++) {
                JSONObject j = c.getJSONObject(i);
                int userId = j.getInt("userId");
                String name = j.getString("username");
                List<MixerUser.Role> userRoles = new LinkedList<MixerUser.Role>();
                JSONArray roles = j.getJSONArray("userRoles");

                for (int k = 0; k < roles.length(); k++) {
                    if (roles.getString(k).equals("Owner")) {
                        userRoles.add(MixerUser.Role.OWNER);
                    } else if (roles.getString(k).equals("User")) {
                        userRoles.add(MixerUser.Role.USER);
                    } else if (roles.getString(k).equals("Mod")) {
                        userRoles.add(MixerUser.Role.MOD);
                    } else if (roles.getString(k).equals("Founder")) {
                        userRoles.add(MixerUser.Role.FOUNDER);
                    } else if (roles.getString(k).equals("Pro")) {
                        userRoles.add(MixerUser.Role.PRO);
                    } else if (roles.getString(k).equals("Global_mod")) {
                        userRoles.add(MixerUser.Role.GLOBAL_MOD);
                    } else if (roles.getString(k).equals("Staff")) {
                        userRoles.add(MixerUser.Role.STAFF);
                    }
                }

                addUser(new MixChatUser(userId, name, userRoles));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addUser(MixChatUser user) {
        //MixerUser user = mixer.use(UsersService.class).findOne(id).get();

        if (users.contains(user)) {
            //System.out.println("user already in list");
        }
        for (MixChatUser u : users) {
            if (u.getUserName().equals(user.getUserName())) {
                //System.out.println("user already in list");
                updateUserList();
                //System.out.println(u.getUserName());
                return;
            }
        }

        users.add(user);

        updateUserList();
    }

	public void setToken(String t) {
		token = t;
	}

	public boolean isConnected() {
		return connected;
	}
}