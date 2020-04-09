package com.benjazzy.mixchat;

import com.benjazzy.mixchat.helper.MixKeys;

import java.io.*;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class MixPreferences {
    private Preferences preferences;

    public MixPreferences() {
        preferences = Preferences.userRoot().node(this.getClass().getName());

        try {
            ArrayList<String> channels = new ArrayList<>();
            channels.add("benjazzy");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = null;
            so = new ObjectOutputStream(bo);
            so.writeObject(channels);
            so.flush();
            preferences.put(MixKeys.UsersList, so.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultChannels(ArrayList<String> channels) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(channels);
            so.flush();
            preferences.put(MixKeys.UsersList, so.toString());

        }
        catch (IOException e) {
            e.printStackTrace();
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
    public ArrayList<String> getDefaultChannels() {
        try {
            if (preferences.nodeExists(MixKeys.UsersList)) {
                try {
                    byte[] b = preferences.get(MixKeys.UsersList, "").getBytes();
                    ByteArrayInputStream bi = new ByteArrayInputStream(b);
                    ObjectInputStream si = new ObjectInputStream(bi);
                    return (ArrayList<String>) si.readObject();
                } catch (Exception e) {
                    System.out.println(e);
                    return new ArrayList<>();
                }
            } else {
                return new ArrayList<>();
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
