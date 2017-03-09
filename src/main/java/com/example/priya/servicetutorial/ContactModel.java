package com.example.priya.servicetutorial;

import java.io.Serializable;

/**
 * Created by priya on 3/7/2017.
 */

public class ContactModel implements Serializable{
    private long id;
    private String JID;
    private String name;

    public ContactModel() {
    }

    public ContactModel(long id, String JID, String name) {
        this.id = id;
        this.JID = JID;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJID() {
        return JID;
    }

    public void setJID(String JID) {
        this.JID = JID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
