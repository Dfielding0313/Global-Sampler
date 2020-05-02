package edu.miami.cs.donaldfielding.screamer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.Activity;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.GLDebugHelper;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.media.audiofx.*;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoticeDialogFragment.NoticeDialogListener, UIDialogsCustomFragment.CustomDialogListener, LocationListener {

    private MediaPlayer samplePlayer;
    private int songloc;
    private MediaRecorder recorder;
    private File audioFile = null;
    static final String TAG = "MediaRecording";
    private SampleDB samples;
    private Cursor audioMediaCursor;
    private SimpleCursorAdapter cursorAdapter;
    private boolean firstTime = true;
    long current_sample = -1;
    private final int CHOOSE_SAMPLE = 19;
    private final int MY_PERMISSIONS_REQUEST = 13;
    private String fileName;
    private boolean dataAdded = false;
    private LocationManager locationManager;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        samplePlayer = new MediaPlayer();
        samplePlayer.reset();
        samplePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        samplePlayer.setLooping(true);
        if (getPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})) {
            goOnCreating(true);
        }
    }

    private boolean getPermission(String[] whatPermissions) {

        int index;
        boolean haveAllPermissions;

        haveAllPermissions = true;
        for (index = 0; index < whatPermissions.length; index++) {
            if (ContextCompat.checkSelfPermission(this, whatPermissions[index]) != PackageManager.PERMISSION_GRANTED) {
                haveAllPermissions = false;
            }
        }
        if (haveAllPermissions) {
            return (true);
        } else {
            ActivityCompat.requestPermissions(this, whatPermissions, MY_PERMISSIONS_REQUEST);
            return (false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        int index;
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST:
                if (grantResults.length > 0) {
                    for (index = 0; index < grantResults.length; index++) {
                        if (grantResults[index] !=
                                PackageManager.PERMISSION_GRANTED) {
                            goOnCreating(false);
                            return;
                        }
                    }
                    goOnCreating(true);
                } else {
                    goOnCreating(false);
                }
                return;
            default:
                return;
        }
    }

    private void goOnCreating(boolean havePermission) {

        String[] queryFields = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };
        String[] displayFields = {
                "sample_name"
        };
        int[] displayViews = {
                R.id.sample_name
        };

        if (havePermission) {
            setContentView(R.layout.activity_main);
            locationManager = (LocationManager) (getSystemService(LOCATION_SERVICE));
            try{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
            }
            catch(SecurityException e)
            {
            }
            samples = new SampleDB(this);
            audioMediaCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queryFields,
                    null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            if (audioMediaCursor != null && audioMediaCursor.moveToFirst()) {
                if (!dataAdded) {
                    DialogFragment addLocal = new NoticeDialogFragment();
                    addLocal.show(getSupportFragmentManager(), "my_fragment");
                }
                cursorAdapter = new SimpleCursorAdapter(this, R.layout.list_item, samples.fetchAllSongs("sample_name"), displayFields, displayViews, 0);
            } else {
                Toast.makeText(this, "Cannot query MediaStore for audio",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Need permission", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        updateMusicDBFromContent();
        dataAdded = true;
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }

    private void updateMusicDBFromContent() {

        ContentValues songData;
        int audioMediaId;

        do {
            audioMediaId = audioMediaCursor.getInt(
                    audioMediaCursor.getColumnIndex(MediaStore.Audio.Media._ID));
            if (samples.getSongByAudioMediaId(audioMediaId) == null) {
                songData = new ContentValues();
                songData.put("audio_media_id", audioMediaId);
                songData.put("sample_name", audioMediaCursor.getString(audioMediaCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                samples.addSong(songData);
            }
        } while (audioMediaCursor.moveToNext());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
        dataAdded = savedInstanceState.getBoolean("dataAdded");
        current_sample = savedInstanceState.getLong("current_sample", -1);
        if (current_sample < 0) {
        } else {
            samplePlayer.reset();
            ContentValues songData = samples.getSongById(current_sample);
            int audioMediaId = songData.getAsInteger("audio_media_id");
            String songName = songData.getAsString("song_name");

            boolean songFound = false;
            if (audioMediaCursor.moveToFirst()) {
                int idIndex = audioMediaCursor.getColumnIndex(
                        MediaStore.Audio.Media._ID);
                do {
                    songFound = audioMediaId == audioMediaCursor.getInt(idIndex);
                } while (!songFound && audioMediaCursor.moveToNext());
            }

            if (songFound) {
                String audioFilename = audioMediaCursor.getString(audioMediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                try {
                    samplePlayer.setDataSource(audioFilename);
                } catch (IOException e) {
                    Toast.makeText(this, "Error playing \"" + songName + "\"", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            songloc = savedInstanceState.getInt("song_position", 0);
            samplePlayer.seekTo(songloc);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        songloc = samplePlayer.getCurrentPosition();
        outState.putInt("song_position", songloc);
        outState.putLong("current_sample", current_sample);
        outState.putBoolean("dataAdded", dataAdded);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(samplePlayer.isPlaying())
        {
            samplePlayer.pause();
            songloc = samplePlayer.getCurrentPosition();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        samplePlayer.release();
        samples.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public void startRecording(View view) throws IOException {
        //Creating file
        if (samplePlayer.isPlaying())
            samplePlayer.pause();
        findViewById(R.id.record).setVisibility(View.INVISIBLE);
        findViewById(R.id.stop).setVisibility(View.VISIBLE);
        File dir = Environment.getExternalStorageDirectory();
        try {
            audioFile = File.createTempFile("sound", ".3gp", dir);
        } catch (IOException e) {
            Log.e(TAG, "external storage access error");
            return;
        }
        //Creating MediaRecorder and specifying audio source, output format, encoder & output format
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audioFile.getAbsolutePath());
        recorder.prepare();
        recorder.start();
    }

    public void stopRecording(View view) {
        findViewById(R.id.record).setVisibility(View.VISIBLE);
        findViewById(R.id.stop).setVisibility(View.INVISIBLE);
        //stopping recorder
        recorder.stop();
        recorder.release();
        //after stopping the recorder, create the sound file and add it to media library.
        UIDialogsCustomFragment mine = new UIDialogsCustomFragment();
        Bundle args = new Bundle();
        args.putString("hint_text", "Enter File Name");
        mine.setArguments(args);
        mine.show(getSupportFragmentManager(), "my_fragment");
    }

    @Override
    public void onDialogSubmit(DialogFragment dialog, String result) {
        fileName = result;
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Audio.Media.TITLE, fileName);
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        values.put(MediaStore.Audio.Media.DATA, audioFile.getAbsolutePath());
        ContentResolver contentResolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(base, values);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
        Toast.makeText(this, "Added File " + fileName, Toast.LENGTH_LONG).show();
        ContentValues songData;
        songData = new ContentValues();
        songData.put("sample_name", fileName);
        String[] queryFields = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };
        audioMediaCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queryFields,
                null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        boolean songFound = false;
        if (audioMediaCursor!= null && audioMediaCursor.moveToFirst()) {
            Log.i("MAde it", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            int idIndex = audioMediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            do {
                songFound = audioFile.getAbsolutePath().equals(audioMediaCursor.getString(idIndex));
            } while (!songFound && audioMediaCursor.moveToNext());
        }
        else{
            Toast.makeText(this,"SOMETHING WENT WRONG",Toast.LENGTH_LONG).show();
        }

        if (songFound) {
            songData.put("audio_media_id", audioMediaCursor.getInt(audioMediaCursor.getColumnIndex(MediaStore.Audio.Media._ID)));
        }
        songData.put("uri", audioFile.getAbsolutePath());
        String loc = androidGeodecode(currentLocation);
        songData.put("sample_loc", loc);
        samples.addSong(songData);
        //creating content resolver and storing it in the external content uri

        //sending broadcast message to scan the media file so that it can be available
        try {
            samplePlayer.reset();
            samplePlayer.setDataSource(audioFile.getAbsolutePath());
            samplePlayer.prepare();
            firstTime = false;
        } catch (IOException e) {

        }
    }

    private String androidGeodecode(Location thisLocation) {

        Geocoder androidGeocoder;
        List<Address> addresses;
        Address firstAddress;
        String addressLine;
        String locationName;
        int index;

        if (Geocoder.isPresent()) {
            androidGeocoder = new Geocoder(this);
            try {
                addresses = androidGeocoder.getFromLocation(
                        thisLocation.getLatitude(), thisLocation.getLongitude(), 1);
                if (addresses.isEmpty()) {
                    return ("ERROR: Unkown location");
                } else {
                    firstAddress = addresses.get(0);
                    locationName = "";
                    index = 0;
                    while ((addressLine = firstAddress.getAddressLine(index)) != null) {
                        locationName += addressLine + ", ";
                        index++;
                    }
                    return (locationName);
                }
            } catch (Exception e) {
                return ("ERROR: " + e.getMessage());
            }
        } else {
            return ("ERROR: No Geocoder available");
        }
    }

    public void myClickHandler(View view) {
        switch (view.getId()) {
            case R.id.the_play:
                if (firstTime) {
                    Intent chooseSample = new Intent(this, SampleSelect.class);
                    startActivityForResult(chooseSample, CHOOSE_SAMPLE);
                    firstTime = false;
                }
                samplePlayer.start();
                break;
            case R.id.the_pause:
                if(samplePlayer.isPlaying())
                {
                    samplePlayer.pause();
                    songloc = samplePlayer.getCurrentPosition();
                }
                break;
            case R.id.choices:
                Intent chooseSample = new Intent(this, SampleSelect.class);
                startActivityForResult(chooseSample, CHOOSE_SAMPLE);
                break;
            case R.id.effects:
                final int sessionId = samplePlayer.getAudioSessionId();
                openEqualizer(this, sessionId);
                break;
            default:
                break;
        }
    }

    public static void openEqualizer(@NonNull final Activity activity, int sessionId) {
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
        } else {
            try {
                final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                activity.startActivityForResult(effects, 0);
            } catch (@NonNull final ActivityNotFoundException notFound) {
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_SAMPLE && resultCode == RESULT_OK) {
            samplePlayer.reset();
            String sampleName = "";
            try{
                sampleName = data.getStringExtra("path");
            }catch(NullPointerException e)
            {
                finish();
            }
            ContentValues songData = samples.getSongByName(sampleName);
            int audioMediaId = songData.getAsInteger("audio_media_id");
            String songName = songData.getAsString("song_name");

            boolean songFound = false;
            if (audioMediaCursor.moveToFirst()) {
            int idIndex = audioMediaCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            do {
                songFound = audioMediaId == audioMediaCursor.getInt(idIndex);
            } while (!songFound && audioMediaCursor.moveToNext());
            }

            if (songFound) {
                String audioFilename = audioMediaCursor.getString(audioMediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                try {
                    samplePlayer.setDataSource(audioFilename);
                    samplePlayer.prepare();
                    firstTime = false;
                } catch (IOException e) {
                    Toast.makeText(this, "Error playing \"" + songName + "\"", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }
}