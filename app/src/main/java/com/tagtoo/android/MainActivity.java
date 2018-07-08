package com.tagtoo.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static Context context;

    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();

    private BottomNavigationView navigation;

    private static String LOG_TAG = "MAIN_ACTIVITY";
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";

    private String tagSerialNbr = null;
    private String tagName = null;
    private String tagDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity.context = getApplicationContext();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // On récupère les messages enregistrés grâce à SharedPreferences
        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);
        Gson gson                = new Gson();
        String messagesSaved     = mPrefs.getString(saved_var_id, null);
        Type type                = new TypeToken<ArrayList<SavedMessage>>() {}.getType();

        ArrayList<SavedMessage> gsonMessages = gson.fromJson(messagesSaved, type);
        if(gsonMessages != null)
            listMessages = gsonMessages;

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_read:
                        setFragment(new HomeTabFragment());
                        return true;
                    case R.id.navigation_write:
                        setFragment(new WriteTabFragment());
                        return true;
                }
                return false;
            }
        });

        getWindow().setSharedElementEnterTransition(new ChangeBounds().setDuration(2000));
        getWindow().setSharedElementReturnTransition(returnTransition());

        HomeTabFragment homeTabFragment = new HomeTabFragment();
        homeTabFragment.addToAdapter(listMessages);

        setFragment(homeTabFragment);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHelpActivity();
            }
        });

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (navigation.getSelectedItemId() == R.id.navigation_read)
                    if (action.equals("READ_TAG")) {
                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName = intent.getStringExtra("TAG_NAME");
                        tagDate = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "TAG DATA = Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        SavedMessage tagData = downloadData(tagSerialNbr, tagDate);
                        listMessages.add(new SavedMessage(tagSerialNbr, tagData.name, tagDate, tagData.thumbnailId, currentDateTimeString, tagData.messageText, tagData.audioFile, tagData.pictureFile, tagData.videoFile));

                        downloadFile(tagSerialNbr, tagDate);

                        HomeTabFragment homeTabFragment = new HomeTabFragment();
                        homeTabFragment.addToAdapter(listMessages);
                        saveMessages(listMessages);
                        setFragment(homeTabFragment);
                        Toast.makeText(context, R.string.read_success, Toast.LENGTH_SHORT).show();
                    }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));

    }

    private Transition returnTransition(){
        ChangeBounds bounds = new ChangeBounds();
        bounds.setInterpolator(new DecelerateInterpolator(1.5f));
        bounds.setDuration(400);
        return bounds;
    }

    public void startHelpActivity(){
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public <T> void saveMessages(ArrayList<T> list){
        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);  // On récupère les préférences de l'application correspondant à l'identifiant stocké dans saved_prefs_id
        SharedPreferences.Editor mEditor = mPrefs.edit();                               // On accède à l'édition de ces préférences
        Gson gson = new Gson();                                                         // On crée une nouvelle instance de Gson, permettant de convertir notre liste d'objets complexe en Json et inversement
        String json = gson.toJson(list);                                                // On convertit en json la liste donnée en argument
        mEditor.putString(saved_var_id, json);                                          // On la stocke dans les préférences sous l'identifiant contenu dans saved_var_id
        mEditor.apply();                                                                // On valide les modifications
        Log.i(LOG_TAG, "Saving messages");
    }

    public SavedMessage downloadData(String serialNbr, String date){
        File directoryCache = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File jsonCache      = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".json");
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
                        if(tagData != null) {
                            Log.i(LOG_TAG, tagData.serialNbr);
                            newTagData = tagData;
                            return newTagData;
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.i(LOG_TAG, "Error retrieving tag data");
                    return null;
                }
            }
            else {
                Log.e(LOG_TAG, "Could not connect to server");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean downloadFile(String serialNbr, String date){

        FTPClient ftp = null;

        File directoryCache = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File audioToDownload  = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".3gp");

        try {
            ftp = new FTPClient();

            ftp.connect(SendMessageActivity.verser);

            Log.i(LOG_TAG, "Trying to connect to the server");

            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                Log.i(LOG_TAG, "Connection to the server successful");
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                FileOutputStream fileOutput = new FileOutputStream(audioToDownload);
                boolean result = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".3gp", fileOutput);
                fileOutput.close();

                if(result) {
                    Log.i(LOG_TAG, "Success downloading from server");
                    ftp.logout();
                    ftp.disconnect();

                    return true;
                } else {
                    ftp.logout();
                    ftp.disconnect();

                    return false;
                }
            }
            else
                Log.e(LOG_TAG, "Could not connect to server");
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        }

        return false;
    }

    public void setFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.content, fragment).commitAllowingStateLoss();
    }
}
