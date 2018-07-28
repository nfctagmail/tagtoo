package com.tagtoo.android;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Context context;

    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();

    private BottomNavigationView navigation;

    private static String LOG_TAG = "MAIN_ACTIVITY";
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";

    private String tagSerialNbr = null;
    private String tagName = null;
    private String tagDate = null;

    SavedMessage tagData;
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

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

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (navigation.getSelectedItemId() == R.id.navigation_read) {
                    assert action != null;
                    if (action.equals("READ_TAG")) {
                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName = intent.getStringExtra("TAG_NAME");
                        tagDate = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "TAG DATA = Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        tagData = downloadData(tagSerialNbr, tagDate);

                        for(SavedMessage savedMessage : listMessages)
                        {
                            if(savedMessage.serialNbr.equals(tagSerialNbr)){
                                listMessages.remove(savedMessage);
                            }
                        }

                        listMessages.add(new SavedMessage(tagSerialNbr, tagData.name, tagDate, tagData.thumbnailId, currentDateTimeString, tagData.messageText, tagData.audioFile, tagData.pictureFile, tagData.videoFile));

                        new DownloadAsync((MainActivity) context).execute(tagSerialNbr, tagDate, tagData.audioFile, tagData.pictureFile, tagData.videoFile);

                        HomeTabFragment homeTabFragment = new HomeTabFragment();
                        homeTabFragment.addToAdapter(listMessages);
                        saveMessages(listMessages);
                        setFragment(homeTabFragment);

                    }
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
                if (navigation.getSelectedItemId() == R.id.navigation_read) {
                    assert action != null;
                    if (action.equals("READ_TAG")) {
                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName = intent.getStringExtra("TAG_NAME");
                        tagDate = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "TAG DATA = Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        tagData = downloadData(tagSerialNbr, tagDate);

                        for(SavedMessage savedMessage : listMessages)
                        {
                            if(savedMessage.serialNbr.equals(tagSerialNbr)){
                                listMessages.remove(savedMessage);
                            }
                        }

                        listMessages.add(new SavedMessage(tagSerialNbr, tagData.name, tagDate, tagData.thumbnailId, currentDateTimeString, tagData.messageText, tagData.audioFile, tagData.pictureFile, tagData.videoFile));

                        new DownloadAsync((MainActivity) context).execute(tagSerialNbr, tagDate, tagData.audioFile, tagData.pictureFile, tagData.videoFile);

                        HomeTabFragment homeTabFragment = new HomeTabFragment();
                        homeTabFragment.addToAdapter(listMessages);
                        saveMessages(listMessages);
                        setFragment(homeTabFragment);

                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
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
         //TODO save to data instead of cache
        File directoryCache = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File jsonCache      = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".json");
        FTPClient ftp;
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
                SavedMessage tagData;
                SavedMessage newTagData;
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

    public void setFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.content, fragment).commitAllowingStateLoss();
    }

    private static class DownloadAsync extends  AsyncTask<Object, Object, Boolean> {

        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        DownloadAsync(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = activityReference.get();
            FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
            android.app.Fragment prev = activity.getFragmentManager().findFragmentByTag("download");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            DialogFragment downloadDialog = DownloadDialog.newInstance();
            downloadDialog.setCancelable(false);
            downloadDialog.show(ft, "download");
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            MainActivity activity = activityReference.get();

            String serialNbr = (String) params[0];
            String date = (String) params[1];
            boolean[] toDownload = new boolean[]{(boolean) params[2], (boolean) params[3], (boolean) params[4]};
            int currentProgress = 0;
            int countToDownload = 0;

            for (boolean aToDownload : toDownload) {
                if (aToDownload)
                    countToDownload++;
            }

            FTPClient ftp;

            File directoryCache = new File(Objects.requireNonNull(activity.getExternalCacheDir()).getAbsolutePath());
            File audioToDownload  = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".3gp");
            File pictureToDownload  = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".jpg");
            File videoToDownload  = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".mp4");

            boolean audioOK = true, pictureOK = true, videoOK = true;

            try {
                ftp = new FTPClient();

                ftp.connect(SendMessageActivity.verser);

                Log.i(LOG_TAG, "Trying to connect to the server");

                if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
                {
                    Log.i(LOG_TAG, "Connection to the server successful");
                    ftp.enterLocalPassiveMode();
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);

                    if(toDownload[0]) {
                        publishProgress("Downloading audio", (int)((currentProgress / (float) countToDownload) * 100));
                        FileOutputStream fileOutputAudio = new FileOutputStream(audioToDownload);
                        boolean resultAudio = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".3gp", fileOutputAudio);
                        fileOutputAudio.close();

                        if (resultAudio) {
                            currentProgress++;
                            publishProgress("Done", (int)((currentProgress / (float) countToDownload) * 100));
                            Log.i(LOG_TAG, "Success downloading audio from server");
                        }
                        else {
                            audioOK = false;
                        }
                    }

                    if(toDownload[1]){
                        publishProgress("Downloading picture", (int)((currentProgress / (float) countToDownload) * 100));
                        FileOutputStream fileOutputPicture = new FileOutputStream(pictureToDownload);
                        boolean resultPicture = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".jpg", fileOutputPicture);
                        fileOutputPicture.close();

                        if (resultPicture) {
                            currentProgress++;
                            publishProgress("Done", (int)((currentProgress / (float) countToDownload) * 100));
                            Log.i(LOG_TAG, "Success downloading picture from server");
                        }
                        else
                            pictureOK = false;

                    }

                    if(toDownload[2]){
                        publishProgress("Downloading video", (int)((currentProgress / (float) countToDownload) * 100));
                        FileOutputStream fileOutputVideo = new FileOutputStream(videoToDownload);
                        boolean resultVideo = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".mp4", fileOutputVideo);
                        fileOutputVideo.close();

                        if (resultVideo) {
                            currentProgress++;
                            publishProgress("Done", (int)((currentProgress / (float) countToDownload) * 100));
                            Log.i(LOG_TAG, "Success downloading video from server");
                        }
                        else
                            videoOK = false;

                    }
                }
                else
                    Log.e(LOG_TAG, "Could not connect to server");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            Log.i(LOG_TAG, audioOK  + " " + pictureOK + " " + videoOK);

            return audioOK && pictureOK && videoOK;
        }

        protected void onProgressUpdate(Object... progress) {
            MainActivity activity = activityReference.get();

            DialogFragment prev = (DialogFragment) activity.getFragmentManager().findFragmentByTag("download");
            Dialog dialog = prev.getDialog();

            TextView progressText = dialog.findViewById(R.id.progressText);
            ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

            progressText.setText((String) progress[0]);
            progressBar.setProgress((int) progress[1]);
        }

        protected void onPostExecute(Boolean result) {

            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            DialogFragment prev = (DialogFragment) activity.getFragmentManager().findFragmentByTag("download");
            prev.dismiss();

            if(result){
                Toast.makeText(activity, R.string.read_success, Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(activity, R.string.error_download, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
