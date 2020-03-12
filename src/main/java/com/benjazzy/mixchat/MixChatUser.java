package com.benjazzy.mixchat;

import com.mixer.api.resource.MixerUser;
import javafx.scene.paint.Color;

import java.util.Comparator;
import java.util.List;

/**
 * Used to store information on the connected users.
 */
public class MixChatUser {
    private int id;                         /** User id. */
    private String userName;                /** Username. */
    private List<MixerUser.Role> roleList;  /** List of user roles. */

    /**
     * @param id        Id of the new user.
     * @param userName  Username of the new user.
     * @param roleList  Roles of the new user.
     */
    MixChatUser(int id, String userName, List<MixerUser.Role> roleList) {
        this.id = id;
        this.userName = userName;
        this.roleList = roleList;
    }

    /**
     * Returns the highest level role from the list of roles.
     *
     * @return  Primary role.
     */
    public MixerUser.Role getPrimaryRole() {
        if (this.roleList.contains(MixerUser.Role.OWNER)) {
            return MixerUser.Role.OWNER;
        }
        else if (this.roleList.contains(MixerUser.Role.FOUNDER)) {
            return MixerUser.Role.FOUNDER;
        }
        else if (this.roleList.contains(MixerUser.Role.STAFF)) {
            return MixerUser.Role.STAFF;
        }
        else if (this.roleList.contains(MixerUser.Role.GLOBAL_MOD)) {
            return MixerUser.Role.GLOBAL_MOD;
        }
        else if (this.roleList.contains(MixerUser.Role.MOD)) {
            return MixerUser.Role.MOD;
        }
        else if (this.roleList.contains(MixerUser.Role.SUBSCRIBER)) {
            return MixerUser.Role.SUBSCRIBER;
        }
        else if (this.roleList.contains(MixerUser.Role.PRO)) {
            return MixerUser.Role.PRO;
        }
        else {
            return MixerUser.Role.USER;
        }
    }

    public Color getColor() {
        switch (getPrimaryRole())
        {
            case OWNER:
                return Color.BLACK;
            case FOUNDER:
                return Color.RED;
            case STAFF:
                return Color.GOLD;
            case GLOBAL_MOD:
                return Color.TEAL;
            case MOD:
                return Color.GREEN;
            case SUBSCRIBER:
                if (this.roleList.contains(MixerUser.Role.PRO))
                    return Color.DEEPPINK;
                else
                    return Color.SKYBLUE;
            case PRO:
                return Color.DEEPPINK;
            default:
                return Color.SKYBLUE;
        }
    }

    public int getId() {return this.id;}
    public String getUserName() {return this.userName;}
    public List<MixerUser.Role> getRoleList() {return this.roleList;}
}