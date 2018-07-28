package com.tagtoo.android;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

public class WriteTextActivity extends AppCompatActivity {

    /**
     * Initialisation des variables globales
     */

    EditText editText;
    TextView counter;
    FloatingActionButton sendButton;

    Context context;
    NfcAdapter mNfcAdapter;
    BroadcastReceiver broadcastReceiver;

    private String tagSerialNbr = null;
    private String tagName = null;
    private String tagDate = null;

    private static final String LOG_TAG = "WRITE_TEXT_ACTIVITY";


    /**
     *  Lors de la création de l'activité
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_text);

        context = this;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        editText = findViewById(R.id.editText);
        counter = findViewById(R.id.textCompteur);
        sendButton = findViewById(R.id.sendButton);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textCounter = String.valueOf(editText.getText().length()) + "/248";
                counter.setText(textCounter);
                if(editText.getText().length() > 248 || editText.getText().length() == 0)
                    sendButton.hide();
                else
                    sendButton.show();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        sendButton.setVisibility(View.GONE);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beamMessage();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();

                if(getFragmentManager().findFragmentByTag("beam") != null)
                    if(action.equals("READ_TAG")) {

                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName      = intent.getStringExtra("TAG_NAME");
                        tagDate      = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        if(uploadText(editText.getText().toString(), tagSerialNbr, tagDate)) {
                            Toast.makeText(context, R.string.success_write_text, Toast.LENGTH_LONG).show();
                            unregisterReceiver(this);
                            finish();
                        } else
                            Toast.makeText(context, R.string.error_write_text_server, Toast.LENGTH_LONG).show();
                    }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();

                if(getFragmentManager().findFragmentByTag("beam") != null)
                    if(action.equals("READ_TAG")) {

                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName      = intent.getStringExtra("TAG_NAME");
                        tagDate      = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        if(uploadText(editText.getText().toString(), tagSerialNbr, tagDate)) {
                            Toast.makeText(context, R.string.success_write_text, Toast.LENGTH_LONG).show();
                            unregisterReceiver(this);
                            finish();
                        } else
                            Toast.makeText(context, R.string.error_write_text_server, Toast.LENGTH_LONG).show();
                    }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * Affichage de la boîte de dialogue
     */

    public void beamMessage() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        editText = findViewById(R.id.editText);
        DialogFragment beamDialog = BeamDialog.newInstance("\"" + editText.getText().toString() + "\"");
        beamDialog.show(ft, "beam");
    }

    private boolean uploadText(String text, String serialNbr, String date) {

        File directoryCache = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File audioCache     = new File(directoryCache, "recording_cache.3gp");
        File jsonCache      = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".json");

        Log.i(LOG_TAG, "Audio Cache : "     + audioCache.toString());

        FTPClient ftp = null;
        try {
            ftp = new FTPClient();
            ftp.connect(SendMessageActivity.verser);

            Log.i(LOG_TAG, "Trying to connect to the server");

            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                Log.i(LOG_TAG, "Connection to the server successful");

                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                FileOutputStream fileOutput = new FileOutputStream(jsonCache);
                boolean resultOut = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".json", fileOutput);
                fileOutput.close();
                SavedMessage tagData = null;
                SavedMessage newTagData = null;
                if(resultOut) {
                    Gson gson = new Gson();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(jsonCache));
                        Type type = new TypeToken<SavedMessage>() {}.getType();
                        tagData = gson.fromJson(br, type);
                        if(tagData != null){
                            newTagData = new SavedMessage(tagData.serialNbr, tagData.name, tagData.dateCreated, tagData.thumbnailId, tagData.dateSaved, text, tagData.audioFile, tagData.pictureFile, tagData.videoFile);
                            String json = gson.toJson(newTagData);
                            FileWriter file = new FileWriter(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/Tag_" + serialNbr + "_" + date + ".json");
                            file.write(json);
                            file.flush();
                            file.close();

                            FileInputStream fileInput2 = new FileInputStream(jsonCache);
                            boolean resultIn2 = ftp.storeFile("/Tag_" + serialNbr + "_" + date + ".json", fileInput2);
                            fileInput2.close();
                            ftp.logout();
                            ftp.disconnect();
                            if(resultIn2) {
                                Log.i(LOG_TAG, "Success uploading tag data to server");
                                return true;
                            }
                            else {
                                Log.i(LOG_TAG, "Error uploading tag data");
                                return false;
                            }
                        }

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.i(LOG_TAG, "Error retrieving tag data");
                    return false;
                }
            }
            else {
                Log.e(LOG_TAG, "Could not connect to server");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
