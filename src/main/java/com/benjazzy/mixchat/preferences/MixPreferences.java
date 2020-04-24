package com.benjazzy.mixchat.preferences;

import com.benjazzy.mixchat.helper.MixKeys;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.prefs.Preferences;

public class MixPreferences {
    private Preferences preferences;

    public ObservableList<String> defaultChannels;

    public MixPreferences() {
        // Create a new Preferences object using this class name as the identifier.
        preferences = Preferences.userRoot().node(this.getClass().getName());
        defaultChannels = FXCollections.observableArrayList(getDefaultChannels());
        // Write the default channels to the preferences whenever defaultChannels is changed.
        defaultChannels.addListener((ListChangeListener<String>) change -> writeDefaultChannels());
    }

    /**
     * Write defaultChannels to preferences.
     */
    private void writeDefaultChannels() {
        try {
            preferences.put(MixKeys.UsersList, toString(new ArrayList<>(defaultChannels)));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets defaultChannels to channels.
     *
     * @param channels The observableList that defaultChannels will be set to.
     */
    public void setDefaultChannels(ObservableList<String> channels) {
        defaultChannels = channels;
    }

    /**
     * Returns a new observableList of default channels from preferences.
     *
     * @return A new list of default channels from preferences.
     */
    public ArrayList<String> getDefaultChannels() {
        try {
            return (ArrayList<String>)fromString(preferences.get(MixKeys.UsersList, ""));
        } catch (Exception e) {
            System.out.println(e);
            return new ArrayList<>();
        }
    }

    /**
     * Checks if the string channel is already in defaultChannles and if not then adds it to the list.
     *
     * @param channel   Channel name to add.
     * @return          Returns true if the channel was added and false if it wasn't.
     */
    public boolean addDefaultChannel(String channel) {
        if (!defaultChannels.contains(channel)) {
            defaultChannels.add(channel);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Checks if the string channel is already in defaultChannels and if so removes it from the list.
     * @param channel   Channel name to remove.
     * @return          Returns true if channel as removed and false if it wasn't.
     */
    public boolean removeDefaultChannel(String channel) {
        if (defaultChannels.contains(channel)) {
            defaultChannels.remove(channel);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Read the object to a Base64 string.
     *
     * @param s The string to deserialize.
     * @return  Returns the deserialized object.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Object fromString( String s ) throws IOException ,
            ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    /**
     * Write the object to a Base64 string.
     *
     * @param o The object to serialize.
     * @return  Returns the serialized object as a string.
     * @throws IOException
     */
    private static String toString( Serializable o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
