package edu.miami.cs.donaldfielding.screamer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;

public class SampleSelect extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, UIDialogsCustomFragment.CustomDialogListener {

    private SampleDB samples;
    private Cursor audioMediaCursor;
    private SimpleCursorAdapter cursorAdapter;
    ListView theList;
    File targetFile;
    String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_select);
        theList = findViewById(R.id.the_list);
        goOnCreating();
    }
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        String[] displayFields = {
                "sample_name",
                "sample_loc"
        };
        int[] displayViews = {
                R.id.sample_name,
                R.id.sample_loc
        };
        switch(item.getItemId())
        {
            case(R.id.nameAtoZ):
                cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item,samples.fetchAllSongs("sample_name"),displayFields, displayViews,0);
                theList.setAdapter(cursorAdapter);
                return true;
            case(R.id.nameZtoA):
                cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item,samples.fetchAllSongs("sample_name DESC"),displayFields, displayViews,0);
                theList.setAdapter(cursorAdapter);
                return true;
            case(R.id.locationdown):
                cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item,samples.fetchAllSongs("sample_loc"),displayFields, displayViews,0);
                theList.setAdapter(cursorAdapter);
                return true;
            case(R.id.locationup):
                cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item,samples.fetchAllSongs("sample_loc DESC"),displayFields, displayViews,0);
                theList.setAdapter(cursorAdapter);
                return true;
            default:
                return true;
        }
    }

    private void goOnCreating() {
        String[] queryFields = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };
        String[] displayFields = {
                "sample_name",
                "sample_loc"
        };
        int[] displayViews = {
                R.id.sample_name,
                R.id.sample_loc
        };
        audioMediaCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queryFields,
                null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        samples = new SampleDB(this);
        cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item,samples.fetchAllSongs("sample_name"),displayFields, displayViews,0);
        theList.setAdapter(cursorAdapter);
        theList.setOnItemLongClickListener(this);
        theList.setOnItemClickListener(this);
        }
    public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
        TextView name = view.findViewById(R.id.sample_name);
        Intent returnIntent = new Intent();
        String[] queryFields = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };
        audioMediaCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queryFields,
                null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        boolean songFound = false;
        ContentValues current = samples.getSongByName(name.getText().toString());
        int mediaID = current.getAsInteger("audio_media_id");
        if (audioMediaCursor!= null && audioMediaCursor.moveToFirst()) {
            int idIndex = audioMediaCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            do {
                songFound = mediaID == audioMediaCursor.getInt(idIndex);
            } while (!songFound && audioMediaCursor.moveToNext());
        }
        else{
            Toast.makeText(this,"SOMETHING WENT WRONG",Toast.LENGTH_LONG).show();
        }

        if (songFound) {
            returnIntent.putExtra("path", name.getText().toString());
            setResult(RESULT_OK,returnIntent);
            finish();
        }
        else
        {
            samples.deleteSong(current.getAsInteger("_id"));
            goOnCreating();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        TextView name = view.findViewById(R.id.sample_name);
        path = name.getText().toString();
        UIDialogsCustomFragment mine = new UIDialogsCustomFragment();
        Bundle args = new Bundle();
        args.putString("hint_text", "Enter Recipient Email");
        mine.setArguments(args);
        mine.show(getSupportFragmentManager(), "my_fragment");
        return true;
    }

    @Override
    public void onDialogSubmit(DialogFragment dialog, String entryText) {
        Intent sendEmail = new Intent();
        sendEmail.setType("plain/text*");
        sendEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{entryText});
        sendEmail.putExtra(Intent.EXTRA_SUBJECT, "Your sample");
        sendEmail.putExtra(Intent.EXTRA_TEXT, path + " delivered!");
        ContentValues songData = samples.getSongByName(path);
        int audioMediaId = songData.getAsInteger("audio_media_id");
        boolean songFound = false;
        if (audioMediaCursor.moveToFirst()) {
            int idIndex = audioMediaCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            do {
                songFound = audioMediaId == audioMediaCursor.getInt(idIndex);
            } while (!songFound && audioMediaCursor.moveToNext());
        }

        if (songFound) {
            String audioFilename = audioMediaCursor.getString(audioMediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            targetFile = new File(audioFilename);
            Uri u = Uri.fromFile(targetFile);
            sendEmail.putExtra(Intent.EXTRA_STREAM, u);
            startActivityForResult(Intent.createChooser(sendEmail,"Send Mail"), 4);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu,view,menuInfo);
        getMenuInflater().inflate(R.menu.context,menu);
    }
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.delete:
                break;
            case R.id.rename:
                break;
        }
        return true;
    }
}
