package com.benjazzy.mixchat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

public class MixSocketClient extends WebSocketClient {
    private MixSocketReply reply;

    public MixSocketClient(URI serverUri, MixSocketReply reply) {
        super( serverUri );
        this.reply = reply;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String message) {
        reply.reply = new JSONObject(message);
        reply.onReply(reply.reply);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
}
