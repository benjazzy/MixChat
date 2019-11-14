package com.benjazzy.mixchat;

/**
 * Hello world!
 *
 */
import com.mixer.api.MixerAPI;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.IncomingMessageEvent;
import com.mixer.api.resource.chat.events.UserJoinEvent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MixChat {
	// private static final File DATA_STORE_DIR = new
	// File(System.getProperty("user.home"), ".store/dailymotion_sample");
	
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		String token = "";
		MixOauth oauth = new MixOauth();
		System.out.println(oauth.getAccessToken());
		token = oauth.getAccessToken();

		// MixerAPI mixer = new
		// MixerAPI("e90d75653df737f7b50157fd6866d866a80c0f5ebe6ef14e",
		// "IG0hLLsqohPOP8KQVA5MZ2nHPwdcWEQzhJ66y3SVswD4mmBEognCK9oWB2WQD5Oq");
		MixerAPI mixer = new MixerAPI("3721d6b1332a6db44a22ab5b71ae8e34ae187ee995b38f1a", token);

		// Invoke the `UsersService.class` in order to access the methods within
		// that service. Then, assign a callback using Guava's FutureCallback
		// class so we can act on the response.
		/*
		  Futures.addCallback(mixer.use(UsersService.class).search("benjazzy"), new
		  ResponseHandler<UserSearchResponse>() { // Set up a handler for the response

		  @Override public void onSuccess(UserSearchResponse response) { for (MixerUser
		  user : response) { System.out.println(user.username); } } });
		 */

		MixerUser user = mixer.use(UsersService.class).getCurrent().get();
		MixerChat chat = mixer.use(ChatService.class).findOne(user.channel.id).get();
		MixerChatConnectable chatConnectable = chat.connectable(mixer);

		if (chatConnectable.connect()) {
			chatConnectable.send(AuthenticateMessage.from(user.channel, user, chat.authkey),
					new ReplyHandler<AuthenticationReply>() {
						public void onSuccess(AuthenticationReply reply) {
							chatConnectable.send(ChatSendMethod.of("Hello World!"));
						}

						public void onFailure(Throwable var1) {
							var1.printStackTrace();
						}
					});
		}

		chatConnectable.on(UserJoinEvent.class, event -> {
			chatConnectable.send(ChatSendMethod
					.of(String.format("Hi %s! I'm pingbot! Write !ping and I will pong back!", event.data.username)));
		});

		chatConnectable.on(IncomingMessageEvent.class, event -> {
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

			System.out.println("\u0040");

			for (MessageTextComponent m : event.data.message.message) {
				String format = "";
				if (m.text.contains("\u0040")) {
					format = ConsoleColors.BLUE_UNDERLINED;
				}
				message = String.format("%s%s%s%s", format, message, m.text, ConsoleColors.RESET);
			}

			String output = String.format("%s %s%s%s: %s", formatter.format(date), usernameFormat, username,
					ConsoleColors.RESET, message);
			System.out.println(output);
			if (event.data.message.message.get(0).text.startsWith("!ping")) {
				chatConnectable.send(ChatSendMethod.of(String.format("@%s PONG!", event.data.userName)));
			}
		});

	}
}