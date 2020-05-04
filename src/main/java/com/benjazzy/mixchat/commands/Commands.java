package com.benjazzy.mixchat.commands;

import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.chat.AbstractChatReply;
import com.mixer.api.resource.chat.events.ClearMessagesEvent;
import com.mixer.api.resource.chat.methods.ClearMessagesMethod;
import com.mixer.api.resource.chat.methods.PurgeMethod;
import com.mixer.api.resource.chat.methods.WhisperMethod;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import sun.awt.image.ImageWatched;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Commands {
    ArrayList<AbstractCommand> commands = new ArrayList<>();

    public Commands() {
        addCommand("/clear", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                chatConnectable.send(ClearMessagesMethod.of());
            }
        });
        addCommand("/whisper", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                if (argument != null) {
                    sendWhisper(argument, chatConnectable);
                }
            }
        });
        addCommand("/purge", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, @Nullable String argument) {
                if (argument != null) {
                    sendPurge(argument, chatConnectable);
                }
            }
        });
    }

    public boolean isCommand(String name) {
        return commands.stream().filter(o -> o.name.contains(name)).findFirst().isPresent();
    }

    public void runCommand(String name, MixerChatConnectable connectable, @Nullable String argument) {
        if (isCommand(name)) {
            AbstractCommand command = commands.stream().filter(o -> o.name.contains(name)).findFirst().orElse(null);
            if (command != null) {
                command.run(connectable, argument);
            }
        }
    }

    public void runCommand(String name, MixerChatConnectable connectable) {
        runCommand(name, connectable, null);
    }

    public void addCommand(String name, AbstractCommand command) {
        command.name = name;
        commands.add(command);
    }

    public List<String> getCommands() {
        return new LinkedList<>();
    }

    private void sendWhisper(String message, MixerChatConnectable chatConnectable) {
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

            chatConnectable.send(WhisperMethod.builder().to(user).send(rebuiltMessage).build());
        }
    }

    private void sendPurge(String message, MixerChatConnectable chatConnectable) {
        String[] messageComponents = message.split(" ");
        if (messageComponents.length != 2 || !messageComponents[1].startsWith("@")) {
            System.out.println("Invalid purge.");
        } else {
            messageComponents[1] = messageComponents[1].substring(1);

            chatConnectable.send((PurgeMethod.of(messageComponents[1])), new ReplyHandler<AbstractChatReply>() {
                @Override
                public void onSuccess(@Nullable AbstractChatReply abstractChatReply) {
                    System.out.println();
                }
            });
        }
    }
}
