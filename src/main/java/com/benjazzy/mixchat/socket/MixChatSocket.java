package com.benjazzy.mixchat.socket;

import com.benjazzy.mixchat.MixSocketReply;
import com.benjazzy.mixchat.oauth.MixOauth;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MixChatSocket {
    private int id = 0;
    private URL baseURL;
    private String authKey;
    private MixSocketClient socketClient;

    public MixChatSocket(int chatId) {
        try {
            baseURL = new URL("https://mixer.com/api/v1/chats/" + chatId);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void auth(int channelId, int userId, MixSocketReply reply) {
        try {
            socketClient = getSocket(baseURL, reply);
            authKey = getAuthkey(baseURL);
            socketAuth(authKey, channelId, userId);
        } catch (KeyManagementException | NoSuchAlgorithmException | IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the authkey from the specified endpoint.
     *
     * @param url Url of the endpoint to get the authkey from.  https://mixer.com/api/v1/chats/{chatid}.
     * @return Returns the authkey from the endpoint.
     * @throws IOException
     */
    private String getAuthkey(URL url) throws IOException {
        MixOauth oauth = new MixOauth();                                    /* Get a hold of the Oauth token. */

        /*
         * Make http GET request to url and read it into response.
         */
        // Sending get request
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Authorization", "Bearer " + oauth.getAccessToken());
        //e.g. bearer token= eyJhbGciOiXXXzUxMiJ9.eyJzdWIiOiPyc2hhcm1hQHBsdW1zbGljZS5jb206OjE6OjkwIiwiZXhwIjoxNTM3MzQyNTIxLCJpYXQiOjE1MzY3Mzc3MjF9.O33zP2l_0eDNfcqSQz29jUGJC-_THYsXllrmkFnk85dNRbAw66dyEKBP5dVcFUuNTA8zhA83kk3Y41_qZYx43T

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("GET");


        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;

        StringBuffer response = new StringBuffer();
        while ((output = in.readLine()) != null) {
            response.append(output);
        }

        in.close();

        /*
         * Parse response as json and return authkey key.
         */
        JSONObject obj = new JSONObject(response.toString());
        return obj.getString("authkey");
    }

    /**
     * Get the websocket endpoint from the specified http endpoint.  https://mixer.com/api/v1/chats/{chatid}.
     *
     * @param url Url of the endpoint to get the websocket endpoint from.
     * @return The websocket endpoint from the http endpoint.
     * @throws IOException
     */
    private String getEndpoints(URL url) throws IOException {
        MixOauth oauth = new MixOauth();                    /* Get a hold of the Oauth token. */

        /*
         * Make http GET request to url and read it into response.
         */
        // Sending get request
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Authorization", "Bearer " + oauth.getAccessToken());
        //e.g. bearer token= eyJhbGciOiXXXzUxMiJ9.eyJzdWIiOiPyc2hhcm1hQHBsdW1zbGljZS5jb206OjE6OjkwIiwiZXhwIjoxNTM3MzQyNTIxLCJpYXQiOjE1MzY3Mzc3MjF9.O33zP2l_0eDNfcqSQz29jUGJC-_THYsXllrmkFnk85dNRbAw66dyEKBP5dVcFUuNTA8zhA83kk3Y41_qZYx43T

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("GET");


        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;

        StringBuffer response = new StringBuffer();
        while ((output = in.readLine()) != null) {
            response.append(output);
        }

        in.close();

        /*
         * Parse response as json and return first element in endpoints array.
         */
        JSONObject obj = new JSONObject(response.toString());
        JSONArray array = obj.getJSONArray("endpoints");
        return array.getString(0);
    }

    /**
     * Gets a new ssl websocket
     *
     * @param url   Websocket endpoint and port to connect to.
     * @param reply MixSocketReply to reply to.
     * @return Returns a MixSocket that can send messages to the Mixer API.
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    private MixSocketClient getSocket(URL url, MixSocketReply reply) throws KeyManagementException, NoSuchAlgorithmException, IOException, URISyntaxException, InterruptedException {
        MixSocketClient socket = new MixSocketClient(new URI(getEndpoints(url)), reply);    /* Create new MixSocket using url and MixSocketReply reply. */

        /*
         * Setup Socket to user ssl.
         */
        SSLContext sslContext = null;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);

        SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();

        socket.setSocket(factory.createSocket());


        socket.connectBlocking();   /* Connect to socket and block until connected. */

        return socket;
    }

    /**
     * Send the auth message to specified websocket.
     *
     * @param authkey   Authkey from getAuthkey() to pass with auth message.
     * @param channelID ID of the channel to authenticate with.
     * @param userID    ID of the user to authenticate as.
     */
    private void socketAuth(String authkey, int channelID, int userID) {
        /*
         * Format json request int JSONObject
         */
        JSONObject auth = new JSONObject("{\n" +
                "  \"type\": \"method\",\n" +
                "  \"method\": \"auth\",\n" +
                "  \"arguments\": [" + channelID + ", " + userID + ", \"" + authkey + "\"],\n" +
                "  \"id\": " + id + "\n" +
                "}");
        socketClient.send(auth.toString());
        id++;
    }

    /**
     * Send the delete message message over the websocket.  Must send the auth message first.
     *
     * @param uuid   UUID of the message to be deleted.
     */
    public void delete(String uuid) {
        JSONObject deleteKey = new JSONObject("{\n" +
                "  \"type\": \"method\",\n" +
                "  \"method\": \"deleteMessage\",\n" +
                "  \"arguments\": [\"" + uuid + "\"],\n" +
                "  \"id\": " + id + "\n" +
                "}");

        socketClient.send(deleteKey.toString());
        id++;
    }

    public void clear() {
        JSONObject clear = new JSONObject()
                .put("type", "method")
                .put("method", "clearMessages")
                .put("arguments", Arrays.asList())
                .put("id", id);
        socketClient.send(clear.toString());
        id++;
    }
}
