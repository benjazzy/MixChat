package com.benjazzy.mixchat;

import com.google.common.util.concurrent.FutureCallback;
import org.json.JSONObject;

public abstract class MixSocketReply {
    public JSONObject reply;

    public abstract void onReply(JSONObject reply);
}
