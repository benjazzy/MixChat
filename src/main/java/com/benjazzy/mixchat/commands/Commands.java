package com.benjazzy.mixchat.commands;

import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.chat.AbstractChatReply;
import com.mixer.api.resource.chat.methods.*;
import com.mixer.api.resource.chat.replies.*;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                sendWhisper(argument, chatConnectable);
            }
        });
        addCommand("/purge", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                sendPurge(argument, chatConnectable);
            }
        });
        addCommand("/timeout", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                sendTimeout(argument, chatConnectable);
            }
        });
        addCommand("/poll", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                sendPollStart(argument, chatConnectable);
            }
        });
        addCommand("/vote", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                sendPollVote(argument, chatConnectable);
            }
        });
        addCommand("/giveaway", new AbstractCommand() {
            @Override
            public void run(MixerChatConnectable chatConnectable, String argument) {
                chatConnectable.send(GiveawayStartMethod.of());
            }
        });
    }

    public boolean isCommand(String name) {
        return commands.stream().filter(o -> o.name.contains(name)).findFirst().isPresent();
    }

    public void runCommand(String name, MixerChatConnectable connectable, String argument) {
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
        return commands.stream().map((AbstractCommand o) -> o.name).collect(Collectors.toList());
    }

    public List<String> getCommands(String name) {
        return getCommands().stream().filter(o -> o.contains(name)).collect(Collectors.toList());
    }

    public String get(String name) {
        if (isCommand(name)) {
            AbstractCommand command = commands.stream().filter(o -> o.name.contains(name)).findFirst().orElse(null);
            return command.name;
        }
        return "";
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

            chatConnectable.send(PurgeMethod.of(messageComponents[1]));
        }
    }

    private void sendTimeout(String message, MixerChatConnectable chatConnectable) {
        String[] messageComponents = message.split(" ");
        if (messageComponents.length != 3 || !messageComponents[1].startsWith("@")) {
            System.out.println("Invalid timeout.");
        } else {
            messageComponents[1] = messageComponents[1].substring(1);
            MixerUser user = new MixerUser();
            user.username = messageComponents[1];

            chatConnectable.send(TimeoutMethod.builder().to(user).time(messageComponents[2]).build());
        }
    }

    private void sendPollStart(String message, MixerChatConnectable chatConnectable) {
        String[] messageComponents = message.split(" ");
        if (messageComponents.length < 5) {
            System.out.println("Invalid poll.");
        } else {
            chatConnectable.send(VoteStartMethod.from(messageComponents[1], Arrays.copyOfRange(messageComponents, 3, messageComponents.length), Integer.parseInt(messageComponents[2])));
        }
    }

    private void sendPollVote(String message, MixerChatConnectable chatConnectable) {
        String[] messageComponents = message.split(" ");
        if (messageComponents.length != 2) {
            System.out.println("Invalid vote.");
        } else {
            chatConnectable.send(CastVoteMessage.of(Integer.parseInt(messageComponents[1])));
        }
    }
}
