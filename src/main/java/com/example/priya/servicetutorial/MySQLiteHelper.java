package com.example.priya.servicetutorial;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by priya on 1/24/2017.
 */

public class MySQLiteHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "chats.db";
    private static final int DB_VERSION = 2;

    static final String TABLE_CHATS = "chats";
    static final String COLUMN_ID = "_id";
    static final String COLUMN_SOURCE = "source";
    static final String COLUMN_TYPE = "type";
    static final String COLUMN_MESSAGE = "message";
    private static final String CHATS_TABLE_CREATE = "create table "+TABLE_CHATS+"( "+COLUMN_ID
            +" integer primary key autoincrement, "+
            COLUMN_SOURCE + " text not null, "+
            COLUMN_TYPE + " text not null, "+
            COLUMN_MESSAGE + " text not null);";

    static final String TABLE_ROSTER = "rosters";
    static final String COLUMN_NAME = "Name";
    static final String COLUMN_JID = "JID";
    private static final String ROSTER_TABLE_CREATE = "create table "+TABLE_ROSTER+"( "+COLUMN_ID
            +" integer primary key autoincrement, "+
    COLUMN_JID + " text not null, "+
    COLUMN_NAME + " text not null);";

    public MySQLiteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CHATS_TABLE_CREATE);
        db.execSQL(ROSTER_TABLE_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_CHATS);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_ROSTER);
        onCreate(db);
    }
}
