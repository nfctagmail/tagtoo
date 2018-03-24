package com.tagtoo.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static Context context;

    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();
    private static ArrayList<SavedMessage> gsonMessages = new ArrayList<>();

    private BottomNavigationView navigation;

    private static String LOG_TAG = "MAIN_ACTIVITY";
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity.context = getApplicationContext();

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
    }

    public void startHelpActivity(){
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    /*
    Fonctions pour créer un menu en haut à droite, dans la barre d'outils
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    // Si l'activité reçoit une Intention
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Log.i(LOG_TAG, "New intent");
        Log.i(LOG_TAG, intent.getAction() + "");

        // Si l'onglet sélectionné est celui de lecture
        if (navigation.getSelectedItemId() == R.id.navigation_read)
            // Si l'action de l'intention correspond à celle d'un tag NFC
            if ((NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))) {

                Log.i(LOG_TAG, "Tag découvert");

                // On récupère le(s) message(s). Parcelable sert à envoyer des données à travers des Intents, d'activité à activité par exemple
                Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                // S'il y a un/des message(s)
                if (rawMessages != null) {

                    Log.i(LOG_TAG, "Tag contient un message");

                    // On récupère le premier message contenu dans le Parcelable
                    NdefMessage messages = (NdefMessage) rawMessages[0];

                    // On le change en chaîne de caractères
                    String stringMessage = new String(messages.getRecords()[0].getPayload());

                    // On récupère la date sous la forme d'une chaîne de caractères
                    String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                    // On crée le message qui s'affichera sur l'accueil
                    listMessages.add(new SavedMessage(stringMessage, currentDateTimeString));

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

    public void setFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();                   // On récupère le gérant de fragment (Niveau C1 d'anglais... eh oui)
        fm.beginTransaction().replace(R.id.content, fragment).commit();     // On fait une transaction de fragment en remplaçant celui qui est dans la partie "content" = "contenu" de la disposition poar celui donné en arguement
    }

    // On crée l'objet SavedMessage qui detérmine toute l'information que va contenir un élément de la liste des messages
    public class SavedMessage {
        public final String content;
        public final String dateSaved;
        //public final String tagName;

        public SavedMessage(String content, String dateSaved){
            this.content = content;
            this.dateSaved = dateSaved;
        }
    }

}
