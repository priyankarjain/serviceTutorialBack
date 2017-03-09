package com.example.priya.servicetutorial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by priya on 3/7/2017.
 */

public class RosterDataSource {
    private MySQLiteHelper dbhelper;
    private SQLiteDatabase database;
    private String[] allColumns = {MySQLiteHelper.COLUMN_ID,MySQLiteHelper.COLUMN_JID,
            MySQLiteHelper.COLUMN_NAME};

    public RosterDataSource(Context context){
        dbhelper = new MySQLiteHelper(context);
    }
    public void open() throws SQLException {
        database = dbhelper.getWritableDatabase();
    }

    public void close(){
        dbhelper.close();
    }

    public ContactModel insertRoster(String JID, String name){
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_JID,JID);
        values.put(MySQLiteHelper.COLUMN_NAME,name);
        long insertId = database.insert(MySQLiteHelper.TABLE_ROSTER,null,values);

        if(insertId!=-1) return new ContactModel(insertId, JID, name);
        else{
            Log.e("Insert","Error inserting in the database");
            return null;
        }
    }

    private ContactModel cursorToRoster(Cursor cursor) {
        ContactModel cm = new ContactModel();
        cm.setId(cursor.getLong(0));
        cm.setJID(cursor.getString(1));
        cm.setName(cursor.getString(2));
        return cm;
    }

    public void deleteRoster(ContactModel contact){
        long id = contact.getId();
        database.delete(MySQLiteHelper.TABLE_ROSTER,MySQLiteHelper.COLUMN_ID + "=" +id,null);
    }

    public List<ContactModel> getAllRosters(){
        List<ContactModel> rosterEntries = new ArrayList<>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_ROSTER,allColumns,null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            ContactModel cm = cursorToRoster(cursor);
            rosterEntries.add(cm);
            cursor.moveToNext();
        }
        cursor.close();
        return rosterEntries;
    }

    public List<String> getAllIds(){
        List<String> rosterEntries = new ArrayList<>();
        String[] columns = {MySQLiteHelper.COLUMN_JID};
        Cursor cursor = database.query(MySQLiteHelper.TABLE_ROSTER,columns,null,null,null,null,null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            rosterEntries.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return rosterEntries;
    }
}
