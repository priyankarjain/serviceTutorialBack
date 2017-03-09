package com.example.priya.servicetutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by priya on 1/17/2017.
 */

public class ContactsData {
    public ArrayList<String> contacts;
    public HashMap<String,Boolean> presences;

    public ContactsData(){
        contacts = new ArrayList<>();
        presences = new HashMap<>();
    }

    public void addContacts(List<String> addresses){
        for (String address :
                addresses) {
            presences.put(address,false);
            contacts.add(address);
        }
    }
}
