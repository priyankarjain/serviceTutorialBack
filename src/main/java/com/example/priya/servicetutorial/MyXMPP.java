package com.example.priya.servicetutorial;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntries;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by priya on 1/15/2017.
 */

public class MyXMPP {
    private XMPPTCPConnection connection;
    public boolean connected = false;
    public boolean logedin = false;
    private MessagingService service;
    private String HOST = "192.168.43.106";
    //private String HOST = "192.168.56.1";
    private int PORT = 5222;
    private String RESOURCE = "Android";
    private String username;
    private String password;
    private ChatManager chatManager;
    //private Roster roster;
    public static String LOGIN_ACTION = "com.example.priya.LOGIN";
    private HashMap<String,String> chatThreads;
    //private Collection<RosterEntry> contacts;
    //private HashMap<String,Presence> presences;
    private ChatDataSource dataSource;
   // private RosterDataSource rosterDataSource;
    private MyRosterManager rosterManager;

    private ChatMessageListener chatMessageListener = new ChatMessageListener() {
        @Override
        public void processMessage(Chat chat, Message message) {
            Log.v("Message",message.toString());
            //store message in the database

            String source = message.getFrom();
            String body = message.getBody();
            String type = "RECV";
            if(body!=null){
                ChatModel cm = dataSource.insertChat(source.split("/")[0],type,body);
                if(cm!=null){
                    Log.v("Inserted",cm.toString());

                    if(service.isActivityForeground() && service.getMessageHandler()!=null){
                        android.os.Message m = new android.os.Message();

                        Bundle data = new Bundle();
                        data.putString("Type","message");
                        data.putSerializable("ChatMessage",cm);
                        m.setData(data);
                        service.getMessageHandler().sendMessage(m);
                    }
                }else{
                    Log.v("XMPP","Unable to insert in db");
                }
            }
        }
    };

    private ChatManagerListener chatlistener = new ChatManagerListener() {
        @Override
        public void chatCreated(Chat chat, boolean createdLocally) {
            if(!createdLocally){
                String username = chat.getParticipant().split("/")[0];
                chatThreads.put(username,chat.getThreadID());
                Log.v("Participant",chat.getParticipant());
                chat.addMessageListener(chatMessageListener);
            }
        }
    };

//    private RosterListener rosterListener = new RosterListener() {
//        @Override
//        public void entriesAdded(Collection<String> addresses) {
//            //store new entries in the database
//            for (String s: addresses ) {
//                Log.v("RosterAdd",s);
//                RosterEntry entry = roster.getEntry(s);
//                Log.v("RosterAdd",entry.getUser());
//                if(entry.getName()==null){
//                    try {
//                        entry.setName(entry.getUser().split("@")[0]);
//                        rosterDataSource.insertRoster(entry.getUser(),entry.getName());
//                    } catch (SmackException.NotConnectedException
//                            | SmackException.NoResponseException
//                            | XMPPException.XMPPErrorException e) {
//                        e.printStackTrace();
//                    }
//                }else{
//                    rosterDataSource.insertRoster(entry.getUser(),entry.getName());
//                }
//            }
//            update();
//            if(service.isActivityForeground() && service.getMessageHandler()!=null){
//                android.os.Message m = new android.os.Message();
//                Bundle data = new Bundle();
//                data.putString("Type","rosterAdd");
//                ArrayList<String> list = new ArrayList<>(addresses);
//                data.putStringArrayList("addresses",list);
//                m.setData(data);
//                service.getMessageHandler().sendMessage(m);
//            }
//        }
//
//        @Override
//        public void entriesUpdated(Collection<String> addresses) {
//            //update the database also
//            update();
//            Log.v("RosterUpdate",addresses.toString());
//        }
//
//        @Override
//        public void entriesDeleted(Collection<String> addresses) {
//            update();
//            if(service.isActivityForeground() && service.getMessageHandler()!=null){
//                android.os.Message m = new android.os.Message();
//                Bundle data = new Bundle();
//                data.putString("Type","rosterDel");
//                ArrayList<String> list = new ArrayList<>(addresses);
//                data.putStringArrayList("addresses",list);
//                m.setData(data);
//                service.getMessageHandler().sendMessage(m);
//            }
//            Log.v("RosterDelete",addresses.toString());
//        }
//
//        @Override
//        public void presenceChanged(Presence presence) {
//            Log.v("Roster",presence.toString());
//            Log.v("Activity",service.isActivityForeground()+"");
//            Log.v("MessageHandler",service.getMessageHandler()+"");
//            String bareJid = presence.getFrom().split("/")[0];
//            //check if online on other sources
//            Presence bestPresence = roster.getPresence(presence.getFrom());
//            presences.put(bareJid,bestPresence);
//
//            if(service.getMessageHandler()!=null){
//                android.os.Message m = new android.os.Message();
//                Bundle data = new Bundle();
//                data.putString("Type","presenceChange");
//                data.putString("From", bestPresence.getFrom().split("/")[0]);
//                data.putBoolean("Available", bestPresence.isAvailable());
//                Log.v("Av",bestPresence.isAvailable()+"");
//                m.setData(data);
//                service.getMessageHandler().sendMessage(m);
//            }
//        }
//    };

    private ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {
            connected = true;
            login();
            Log.v("MyXMPP","Connection Successful");
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
            logedin = true;
//            Intent i = new Intent(LOGIN_ACTION);
//            LocalBroadcastManager.getInstance(service).sendBroadcastSync(i);
            rosterManager = new MyRosterManager(service,connection);
            rosterManager.addRosterLoadedListener();
            //roster = Roster.getInstanceFor(connection);
            //roster.addRosterListener(rosterListener);
            //roster.addRosterLoadedListener(rosterLoadedListener);
            chatManager = ChatManager.getInstanceFor(connection);
            chatManager.addChatListener(chatlistener);
            Log.v("MyXMPP","User Authenticated");
        }

        @Override
        public void connectionClosed() {
            if(chatManager!=null){
                chatManager.removeChatListener(chatlistener);
                chatManager = null;
            }
            if(rosterManager!=null){
                rosterManager.removeRosterLoadedListener();
                rosterManager.removeRosterListener();
                rosterManager = null;
            }
//            if(roster!=null){
//                roster.removeRosterLoadedListener(rosterLoadedListener);
//                roster.removeRosterListener(rosterListener);
//                roster = null;
//            }
            if(service!=null){
                service.stopSelf();
                Log.v("MyXMPP","Connection Ended");
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            if(chatManager!=null){
                chatManager.removeChatListener(chatlistener);
                chatManager = null;
            }
            if(rosterManager!=null){
                rosterManager.removeRosterLoadedListener();
                rosterManager.removeRosterListener();
                rosterManager = null;
            }
            if(service!=null){
                service.stopSelf();
                Log.v("MyXMPP","Connection Closed on Error");
            }
        }

        @Override
        public void reconnectionSuccessful() {
            Log.v("MyXMPP","ReConnection Successful");
        }

        @Override
        public void reconnectingIn(int seconds) {

        }

        @Override
        public void reconnectionFailed(Exception e) {
            if(service!=null){
                service.stopSelf();
                Log.v("MyXMPP","ReconnectionFailed");
            }
        }
    };

//    private RosterLoadedListener rosterLoadedListener = new RosterLoadedListener() {
//        @Override
//        public void onRosterLoaded(Roster roster) {
//            Log.e("Roster","Loaded Successfully");
//            RosterEntries initialEntries = new RosterEntries() {
//                @Override
//                public void rosterEntires(Collection<RosterEntry> rosterEntries) {
//                    ArrayList initdb =
//                            (ArrayList) rosterDataSource.getAllIds();
//
//                    for (RosterEntry r :
//                            rosterEntries) {
//                        if(!initdb.contains(r.getUser())){
//                            if(r.getName()== null){
//                                try {
//                                    r.setName(r.getUser().split("@")[0]);
//                                    rosterDataSource.insertRoster(r.getUser(),r.getName());
//                                } catch (SmackException.NotConnectedException
//                                        | SmackException.NoResponseException
//                                        | XMPPException.XMPPErrorException e) {
//                                    e.printStackTrace();
//                                }
//                            }else{
//                                rosterDataSource.insertRoster(r.getUser(),r.getName());
//                            }
//                        }else{
//                            initdb.remove(r.getUser());
//                        }
//                    }
//
//                    if(!initdb.isEmpty()){
//                        Log.e("Roster",initdb +" users left");
//                    }
//                }
//            };
//
//            roster.getEntriesAndAddListener(rosterListener,initialEntries);
//            update();
//        }
//    };

    MyXMPP(String username, String password, Service service){
        this.username = username;
        this.password = password;
        this.service = (MessagingService) service;
        this.chatThreads = new HashMap<>();
//        this.contacts = new ArrayList<>();
//        this.presences = new HashMap<>();
        dataSource = this.service.getDataSource();
//        rosterDataSource = this.service.getRosterDataSource();

    }

    void init(){
        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username,password)
                .setHost(HOST)
                .setPort(PORT)
                .setServiceName(HOST)
                .setResource(RESOURCE)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

        connection = new XMPPTCPConnection(config.build());
        try {
            connection.addConnectionListener(connectionListener);
            connection.connect();
        } catch (IOException | SmackException |XMPPException e) {
            e.printStackTrace();
            Log.v("Connection","Error connecting to the service");
            service.stopSelf();
        }
    }

    private void login(){
        if(connected){
            try {
                connection.login();
            } catch (IOException | SmackException |XMPPException e) {
                e.printStackTrace();
                Log.v("Connection","Error loging to the service");
                service.stopSelf();
            }
        }
    }

    public void sendMessage(String source,String message){
        String threadId = chatThreads.get(source);
        Chat c = chatManager.getThreadChat(threadId);

        try {
            c.sendMessage(message);
        } catch (SmackException.NotConnectedException e) {
            //save the message in table unsend messages
            e.printStackTrace();
        }
    }

    public void createChatThread(String source){
        try{
            Chat c  = chatManager.createChat(source,chatMessageListener);
            Log.v("ChatThread",source);
            chatThreads.put(source,c.getThreadID());
            Log.v("ChatThreadget",chatManager.getThreadChat(c.getThreadID()).getParticipant());
        }catch (NullPointerException e){
            Toast.makeText(service,"Could not create chat now",Toast.LENGTH_SHORT).show();
        }
    }

//    private void requestReloadIfNeeded(){
//        Roster r = Roster.getInstanceFor(connection);
//        if(r!=null && !r.isLoaded()){
//            Log.v("Requested Reload","reload");
//            try {
//                r.reloadAndWait();
//            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void update(){
//        Collection<RosterEntry> newContacts = new ArrayList<>();
//        Set<RosterEntry> entries = roster.getEntries();
//        newContacts.addAll(entries);
//        contacts = newContacts;
//    }
//
    public Collection<RosterEntry> getRosters(){
//        requestReloadIfNeeded();
//        return Collections.unmodifiableCollection(contacts);
        return (rosterManager!=null) ? rosterManager.getRosters() : null;
    }

    public HashMap<String,Presence> getPresence(){
//        return presences;
        return (rosterManager!=null) ? rosterManager.getPresences() : null;
    }
}
