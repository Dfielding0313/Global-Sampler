package edu.miami.cs.donaldfielding.screamer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SampleDB {
    public static final String DATABASE_NAME = "Samples.db";
    private static final int DATABASE_VERSION = 7;

    private static final String SAMPLE_TABLE_NAME = "Samples";
    private static final String CREATE_RATED_MUSIC_TABLE =
            "CREATE TABLE IF NOT EXISTS " + SAMPLE_TABLE_NAME +
                    "(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "audio_media_id INTEGER NOT NULL UNIQUE, " +
                    "sample_name TEXT," + "sample_loc TEXT," +"uri TEXT" +
                    ");";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase theDB;
    //-----------------------------------------------------------------------------
    public SampleDB(Context theContext) {
        dbHelper = new DatabaseHelper(theContext);
        theDB = dbHelper.getWritableDatabase();
    }
    //-----------------------------------------------------------------------------
    public void close() {

        dbHelper.close();
        theDB.close();
    }
    //-----------------------------------------------------------------------------
    public boolean addSong(ContentValues songData) {
        return(theDB.insert(SAMPLE_TABLE_NAME,null,songData) >= 0);
    }
    //-----------------------------------------------------------------------------
    public boolean updateSong(long songID,ContentValues songData) {

        return(theDB.update(SAMPLE_TABLE_NAME,songData,
                "_id =" + songID,null) > 0);
    }
    //-----------------------------------------------------------------------------
    public boolean deleteSong(long songID) {

        return(theDB.delete(SAMPLE_TABLE_NAME,"_id =" + songID,
                null) > 0);
    }
    //-----------------------------------------------------------------------------
    public Cursor fetchAllSongs(String sortField) {

        String[] fieldNames = {"_id","audio_media_id","sample_name, sample_loc, uri"};
        return(theDB.query(SAMPLE_TABLE_NAME,fieldNames,null,null, null,null,sortField));
    }
    //-----------------------------------------------------------------------------
    public ContentValues getSongById(long songId) {

        Cursor cursor;
        ContentValues songData;

        cursor = theDB.query(SAMPLE_TABLE_NAME,null,
                "_id = \"" + songId + "\"",null,null,null,null);
        songData = songDataFromCursor(cursor);
        cursor.close();
        return(songData);
    }
    public ContentValues getSongByName(String name) {

        Cursor cursor;
        ContentValues songData;

        cursor = theDB.query(SAMPLE_TABLE_NAME,null,
                "sample_name = \"" + name + "\"",null,null,null,null);
        songData = songDataFromCursor(cursor);
        cursor.close();
        return(songData);
    }
    //-----------------------------------------------------------------------------
    public ContentValues getSongByAudioMediaId(long audioMediaId) {

        Cursor cursor;
        ContentValues songData;

        cursor = theDB.query(SAMPLE_TABLE_NAME,null,
                "audio_media_id = " + audioMediaId,null,null,null,null);
        songData = songDataFromCursor(cursor);
        cursor.close();
        return(songData);
    }
    //-----------------------------------------------------------------------------
    private ContentValues songDataFromCursor(Cursor cursor) {

        String[] fieldNames;
        int index;
        ContentValues songData;

        if (cursor != null && cursor.moveToFirst()) {
            fieldNames = cursor.getColumnNames();
            songData = new ContentValues();
            for (index=0;index < fieldNames.length;index++) {
                if (fieldNames[index].equals("_id")) {
                    songData.put("_id",cursor.getInt(index));
                } else if (fieldNames[index].equals("audio_media_id")) {
                    songData.put("audio_media_id",cursor.getInt(index));
                } else if (fieldNames[index].equals("song_name")) {
                    songData.put("song_name",cursor.getString(index));
                } else if (fieldNames[index].equals("plays")) {
                    songData.put("plays",cursor.getInt(index));
                }
            }
            return(songData);
        } else {
            return(null);
        }
    }
    //=============================================================================
    private static class DatabaseHelper extends SQLiteOpenHelper {
        //-------------------------------------------------------------------------
        public DatabaseHelper(Context context) {

            super(context,DATABASE_NAME,null,DATABASE_VERSION);
        }
        //-------------------------------------------------------------------------
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(CREATE_RATED_MUSIC_TABLE);
        }
        //-------------------------------------------------------------------------
        @Override
        public void onOpen(SQLiteDatabase db) {

            super.onOpen(db);
        }
        //-------------------------------------------------------------------------
        public void onUpgrade(SQLiteDatabase db,int oldVersion,
                              int newVersion) {

            db.execSQL("DROP TABLE IF EXISTS " + SAMPLE_TABLE_NAME);
            onCreate(db);
        }
        //-------------------------------------------------------------------------
    }
}
