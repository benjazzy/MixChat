package com.benjazzy.mixchat.preferences;

import com.benjazzy.mixchat.helper.MixKeys;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.prefs.Preferences;

public class MixPreferences {
    private Preferences preferences;

    public MixPreferences() {
        preferences = Preferences.userRoot().node(this.getClass().getName());
    }

    public void setDefaultChannels(ArrayList<String> channels) {
        try {
            preferences.put(MixKeys.UsersList, toString(channels));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public ArrayList<String> getDefaultChannels() {
        try {
            return (ArrayList<String>) fromString(preferences.get(MixKeys.UsersList, ""));
        } catch (Exception e) {
            System.out.println(e);
            return new ArrayList<>();
        }
    }
    public boolean addDefaultChannel(String channel) {
        ArrayList<String> channels = getDefaultChannels();
        if (!channels.contains(channel)) {
            channels.add(channel);
            setDefaultChannels(channels);
            return true;
        }
        else {
            return false;
        }
    }
    public boolean removeDefaultChannel(String channel) {
        ArrayList<String> channels = getDefaultChannels();
        if (channels.contains(channel)) {
            channels.remove(channel);
            setDefaultChannels(channels);
            return true;
        }
        else {
            return false;
        }
    }

    private static Object fromString( String s ) throws IOException ,
            ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    /** Write the object to a Base64 string. */
    private static String toString( Serializable o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
