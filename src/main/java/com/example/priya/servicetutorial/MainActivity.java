package com.example.priya.servicetutorial;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements SendMessageListener{
    //service class object
    private MessagingService MyService;
    private boolean bounded;
    //to hold the contacts data
    private ContactsData contactsData;
    ChatDataSource dataSource;
    RosterDataSource rosterDataSource;
    //main message handler for the service
    private final android.os.Handler messageHandler= new android.os.Handler(){
        @Override
        public void handleMessage(android.os.Message msg) {
            Bundle data = msg.getData();
            String type = data.getString("Type");
            Log.v("StringTye:",type);
            if(type == null){
                type="";
            }
            switch (type) {
                case "message":
                    ChatModel cm = (ChatModel) data.getSerializable("ChatMessage");
                    String from = cm.getSource();
                    ChatFragment chatFragment = (ChatFragment) getSupportFragmentManager()
                            .findFragmentByTag("chat:" + from);

                    if (chatFragment != null) {
                        Log.v("Hello", "Yes");
                        chatFragment.receiveMessages(cm);
                    } else {
                        Toast.makeText(MainActivity.this, from + " " + cm, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case "rosterAdd":
                    addRoster(data);
                    break;
                case "rosterDel":
                    removeRoster(data);
                    break;
                case "presenceChange":
                    changeAvailability(data);
                    break;
            }
        }
    };

    public void removeRoster(Bundle data){
        ArrayList<String> addresses = data.getStringArrayList("addresses");

        if (addresses != null) {
            RosterFragment fragment =
                    (RosterFragment) getSupportFragmentManager().findFragmentByTag("Roster");
            for (String address : addresses) {
                if (contactsData.contacts.contains(address)) {
                    int position = contactsData.contacts.indexOf(address);
                    contactsData.contacts.remove(address);
                    contactsData.presences.remove(address);

                    if (fragment != null) {
                        fragment.removeContact(position);
                    }
                }
            }
        }
    }

    public void addRoster(Bundle data){
        ArrayList<String> addresses = data.getStringArrayList("addresses");
        int i = 0;
        int prev = contactsData.contacts.size();

        if (addresses != null) {
            for (String address : addresses) {
                if (!contactsData.contacts.contains(address)) {
                    contactsData.contacts.add(address);
                    contactsData.presences.put(address, false);
                    i++;
                }
            }
            RosterFragment fragment =
                    (RosterFragment) getSupportFragmentManager().findFragmentByTag("Roster");
            if (fragment != null) {
                fragment.addContacts(prev, i);
            }
        }
    }

    public void changeAvailability(Bundle data){
        String source = data.getString("From");
        boolean availability = data.getBoolean("Available");
        contactsData.presences.put(source,availability);

        RosterFragment fragment =
                (RosterFragment) getSupportFragmentManager().findFragmentByTag("Roster");
        if (fragment != null) {
            int position = contactsData.contacts.indexOf(source);
            fragment.availabilityChanged(position);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v("MAINACTIVITY","SERVICE CONNECTED");
            bounded = true;

            MyService =((MessagingService.MyBinder) service).getService();
            MyService.setMessageHander(messageHandler);
            boolean isloggedin = MyService.isLoggedIn();

            //if user is already logged in
            if(isloggedin){
                //request contacts and statuses from the service
                //contactsData.contacts = MyService.loadRosters();
                contactsData.presences = MyService.loadPresences();
                Log.v("contacts", String.valueOf(contactsData.contacts));
                Log.v("presences",String.valueOf(contactsData.presences));
                //check if the fragment is already present
                RosterFragment fragment =
                        (RosterFragment) getSupportFragmentManager().findFragmentByTag("Roster");
                if (fragment != null) {
                    fragment.notifyAboutData();
                }
            }else{
                MyService.start();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MyService = null;
            bounded = false;
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        Intent i = new Intent(this,MessagingService.class);
        bindService(i,serviceConnection,BIND_AUTO_CREATE);
        Log.v("MAINACTIVITY","SERVICE BIND CALL");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        contactsData = new ContactsData();
        dataSource = new ChatDataSource(this);
        dataSource.open();
        rosterDataSource = new RosterDataSource(this);
        rosterDataSource.open();
        contactsData.addContacts(rosterDataSource.getAllIds());
        Toast.makeText(this,"starting frag",Toast.LENGTH_SHORT).show();
        RosterFragment fragment = new RosterFragment();
        fragment.setContactsData(contactsData);
        fragment.setDataSource(dataSource);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_main,fragment,"Roster").commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bounded){
            unbindService(serviceConnection);
            Log.v("MAINACTIVITY","SERVICE UNBIND CALL");
            bounded = false;
        }
    }

    @Override
    public void onCreateChat(String source) {
        if(MyService != null){
            MyService.createUserChat(source);
        }
    }

    @Override
    public void onSendMessage(ChatModel cm) {
        if(MyService != null){
            MyService.sendUserMessage(cm.getSource(),cm.getMessage());
        }
    }
}