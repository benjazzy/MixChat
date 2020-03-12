package com.benjazzy.mixchat;

import com.mixer.api.resource.MixerUser;
import javafx.scene.text.Text;

public class MixChatUserText extends Text
{
    public boolean isLabel;
    public int id;
    public MixerUser.Role role;

    public MixChatUserText(String text, boolean isLabel, MixerUser.Role role)
    {
        this.isLabel = isLabel;
        this.role = role;
        this.id = id;
        super.setText(text);
    }
    public MixChatUserText(String text, int id)
    {
        this.isLabel = false;
        this.id = id;
        super.setText(text);
    }
}
