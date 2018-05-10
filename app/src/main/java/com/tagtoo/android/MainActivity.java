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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static Context context;

    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();
    private static ArrayList<SavedMessage> gsonMessages = new ArrayList<>();

    private BottomNavigationView navigation;

    // Les variables qui contiennent les codes pour identifier ...
    private static String LOG_TAG = "MAIN_ACTIVITY";                // Les messages de cette activité dans les journaux
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";    // Retrouver les préférences dans lesquels on stocke les messages reçus
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";   // Retrouver la variable les contenant dans ces préférences

    // La variable qui contiendra le numéro de série du tag
    private String tagSerialNbr = null;
    private String tagMessage = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity.context = getApplicationContext();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // On récupère les messages enregistrés grâce à SharedPreferences
        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);      // On récupère les préférences de l'application correspondant à l'identifiant stocké dans saved_prefs_id
        Gson gson                = new Gson();                                              // On crée une nouvelle instance de Gson, permettant de convertir notre liste d'objets complexe en Json et inversement
        String messagesSaved     = mPrefs.getString(saved_var_id, null);                // On récupère les données correspondant à l'identifiant stocké dans saved_var_id
        Type type                = new TypeToken<ArrayList<SavedMessage>>() {}.getType();   // On crée le type correspondant à celui de la liste
        gsonMessages             = gson.fromJson(messagesSaved, type);                      // On récupère les données en json que l'on met au type de la liste et que l'on stocke dans une version temporaire de la liste de messages
            if(gsonMessages != null)                                                        // Si cette version temporaire de la liste n'est pas nulle
                listMessages = gsonMessages;                                                // On donne sa valeur à la véritable liste.

        // On attribue chaque fragment à chaque option de la barre de navigation
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        setFragment(new HomeTabFragment());
                        return true;
                    case R.id.navigation_read:
                        setFragment(new ReadTabFragment());
                        return true;
                    case R.id.navigation_write:
                        setFragment(new WriteTabFragment());
                        return true;
                }
                return false;
            }
        });

        // On crée une nouvelle instance de l'onglet principal et on lui donne la liste de messages
        HomeTabFragment homeTabFragment = new HomeTabFragment();
        homeTabFragment.addToAdapter(listMessages);

        // Par défaut, le fragment qui s'affichera quand Main Activity est créé est l'onglet Home
        setFragment(homeTabFragment);

        // On met en place la barre d'outils
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Quand on clique sur le bouton d'aide, on démarre l'activité d'aide
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
                        Log.i(LOG_TAG, "TAG DATA : " + tagSerialNbr);
                        tagMessage = intent.getStringExtra("TAG_MESSAGE");
                        Log.i(LOG_TAG, "TAG DATA : " + tagMessage);

                        // On récupère la date sous la forme d'une chaîne de caractères
                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        // Et sous forme de secondes, pour donner un nom unique au fichier téléchargé, pour que pour un même tag on ait différent fichiers audio à chaque téléchargement
                        Long time = System.currentTimeMillis()/1000;
                        String timeString = time.toString();
                        // On crée le message qui s'affichera sur l'accueil
                        if(downloadFile(tagSerialNbr, timeString))
                            listMessages.add(new SavedMessage(tagMessage, tagSerialNbr, currentDateTimeString, tagSerialNbr + "_download" + time + ".3gp"));
                        else
                            listMessages.add(new SavedMessage(tagMessage, tagSerialNbr, currentDateTimeString));

                        // On crée une nouvelle instance de HomeTabFragment
                        HomeTabFragment homeTabFragment = new HomeTabFragment();

                        // On l'ajoute à l'adapteur de la liste de l'onglet d'accueil grâce à la fonction addToAdapter utilisant la valeur de la variable listMessages étant globale
                        homeTabFragment.addToAdapter(listMessages);

                        // On change de section
                        navigation.setSelectedItemId(R.id.navigation_home);

                        // On sauvegarde les messages
                        saveMessages(listMessages);

                    }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));

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

    private boolean downloadFile(String serialNbr, String time){

        FTPClient ftp = null;

        File directoryCache = new File(getExternalCacheDir().getAbsolutePath());
        File audioToDownload  = new File(directoryCache, serialNbr + "_download" + time + ".3gp");

        try{
            ftp = new FTPClient();
            ftp.connect(SendMessageActivity.verser);

            Log.i(LOG_TAG, "Trying to connect to the server");

            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                Log.i(LOG_TAG, "Connection to the server successful");

                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                FileOutputStream fileOutput = new FileOutputStream(audioToDownload);
                boolean result = ftp.retrieveFile("/" + serialNbr + ".3gp", fileOutput);
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
        } catch (SocketException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        }
        return false;
    }


    public void setFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();                   // On récupère le gérant de fragment (Niveau C1 d'anglais... eh oui)
        fm.beginTransaction().replace(R.id.content, fragment).commitAllowingStateLoss();     // On fait une transaction de fragment en remplaçant celui qui est dans la partie "content" = "contenu" de la disposition poar celui donné en arguement
    }

}
