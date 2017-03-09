package com.example.priya.servicetutorial;

import android.app.Service;
import android.os.Bundle;
import android.util.Log;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntries;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterLoadedListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by priya on 3/9/2017.
 */

public class MyRosterManager{
    private Roster roster;
    private Collection<RosterEntry> rosters;
    private HashMap<String,Presence> presences;
    private RosterDataSource rosterDataSource;
    private MessagingService service;
    private XMPPConnection connection;
    private RosterLoadedListener rosterLoadedListener;
    private RosterListener rosterListener;

    public MyRosterManager(Service service, XMPPConnection connection){
        this.service = (MessagingService) service;
        this.rosters = new ArrayList<>();
        this.presences = new HashMap<>();
        this.connection = connection;
        this.rosterDataSource = this.service.getRosterDataSource();
        this.rosterLoadedListener = new MyRosterLoadedListener();
        this.rosterListener = new MyRosterListener();
    }

    public void addRosterLoadedListener(){
        this.roster = Roster.getInstanceFor(this.connection);
        this.roster.addRosterLoadedListener(rosterLoadedListener);
    }

    public void removeRosterLoadedListener() {
        if(roster!=null){
            roster.removeRosterLoadedListener(rosterLoadedListener);
        }
    }

    public void removeRosterListener(){
        if(roster!=null){
            roster.removeRosterListener(rosterListener);
        }
    }

    private void requestReloadIfNeeded(){
        Roster r = Roster.getInstanceFor(connection);
        if(r!=null && !r.isLoaded()){
            Log.v("Requested Reload","reload");
            try {
                r.reloadAndWait();
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update(){
        Collection<RosterEntry> newContacts = new ArrayList<>();
        Set<RosterEntry> entries = roster.getEntries();
        newContacts.addAll(entries);
        rosters = newContacts;
    }

    public Collection<RosterEntry> getRosters(){
        requestReloadIfNeeded();
        return Collections.unmodifiableCollection(rosters);
    }

    public HashMap<String,Presence> getPresences(){
        return presences;
    }

    private class MyRosterListener implements RosterListener {

        @Override
        public void entriesAdded(Collection<String> addresses) {
            //store new entries in the database
            for (String s: addresses ) {
                Log.v("RosterAdd",s);
                RosterEntry entry = roster.getEntry(s);
                Log.v("RosterAdd",entry.getUser());
                if(entry.getName()==null){
                    try {
                        entry.setName(entry.getUser().split("@")[0]);
                        rosterDataSource.insertRoster(entry.getUser(),entry.getName());
                    } catch (SmackException.NotConnectedException
                            | SmackException.NoResponseException
                            | XMPPException.XMPPErrorException e) {
                        e.printStackTrace();
                    }
                }else{
                    rosterDataSource.insertRoster(entry.getUser(),entry.getName());
                }
            }
            update();
            if(service.isActivityForeground() && service.getMessageHandler()!=null){
                android.os.Message m = new android.os.Message();
                Bundle data = new Bundle();
                data.putString("Type","rosterAdd");
                ArrayList<String> list = new ArrayList<>(addresses);
                data.putStringArrayList("addresses",list);
                m.setData(data);
                service.getMessageHandler().sendMessage(m);
            }
        }

        @Override
        public void entriesUpdated(Collection<String> addresses) {
            //update the database also
            update();
            Log.v("RosterUpdate",addresses.toString());
        }

        @Override
        public void entriesDeleted(Collection<String> addresses) {
            update();
            if(service.isActivityForeground() && service.getMessageHandler()!=null){
                android.os.Message m = new android.os.Message();
                Bundle data = new Bundle();
                data.putString("Type","rosterDel");
                ArrayList<String> list = new ArrayList<>(addresses);
                data.putStringArrayList("addresses",list);
                m.setData(data);
                service.getMessageHandler().sendMessage(m);
            }
            Log.v("RosterDelete",addresses.toString());
        }

        @Override
        public void presenceChanged(Presence presence) {
            Log.v("Roster",presence.toString());
            Log.v("Activity",service.isActivityForeground()+"");
            Log.v("MessageHandler",service.getMessageHandler()+"");
            String bareJid = presence.getFrom().split("/")[0];
            //check if online on other sources
            Presence bestPresence = roster.getPresence(presence.getFrom());
            presences.put(bareJid,bestPresence);

            if(service.getMessageHandler()!=null){
                android.os.Message m = new android.os.Message();
                Bundle data = new Bundle();
                data.putString("Type","presenceChange");
                data.putString("From", bestPresence.getFrom().split("/")[0]);
                data.putBoolean("Available", bestPresence.isAvailable());
                Log.v("Av",bestPresence.isAvailable()+"");
                m.setData(data);
                service.getMessageHandler().sendMessage(m);
            }
        }
    }
    private class MyRosterLoadedListener implements RosterLoadedListener{
        @Override
        public void onRosterLoaded(Roster roster) {
            Log.e("Roster","Loaded Successfully");
            RosterEntries initialEntries = new RosterEntries() {
                @Override
                public void rosterEntires(Collection<RosterEntry> rosterEntries) {
                    ArrayList initdb =
                            (ArrayList) rosterDataSource.getAllIds();

                    for (RosterEntry r :
                            rosterEntries) {
                        if(!initdb.contains(r.getUser())){
                            if(r.getName()== null){
                                try {
                                    r.setName(r.getUser().split("@")[0]);
                                    rosterDataSource.insertRoster(r.getUser(),r.getName());
                                } catch (SmackException.NotConnectedException
                                        | SmackException.NoResponseException
                                        | XMPPException.XMPPErrorException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                rosterDataSource.insertRoster(r.getUser(),r.getName());
                            }
                        }else{
                            initdb.remove(r.getUser());
                        }
                    }

                    if(!initdb.isEmpty()){
                        Log.e("Roster",initdb +" users left");
                    }
                }
            };

            roster.getEntriesAndAddListener(rosterListener,initialEntries);
            update();
        }
    }
}

