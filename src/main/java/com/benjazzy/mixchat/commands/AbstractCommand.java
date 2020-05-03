package com.benjazzy.mixchat.commands;

import com.mixer.api.resource.chat.ws.MixerChatConnectable;

import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractCommand {
    public String name;
    public List<String> arguments;

    public abstract void run(MixerChatConnectable chatConnectable, @Nullable String argument);
}
