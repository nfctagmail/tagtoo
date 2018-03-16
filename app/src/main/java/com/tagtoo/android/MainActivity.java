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

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

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
    };


    private static String LOG_TAG = "MAIN_ACTIVITY";
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity.context = getApplicationContext();

        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);
        Gson gson                = new Gson();
        String messagesSaved     = mPrefs.getString(saved_var_id, null);
        Type type                = new TypeToken<ArrayList<SavedMessage>>() {}.getType();
        gsonMessages             = gson.fromJson(messagesSaved, type);
        if(gsonMessages != null)
            listMessages = gsonMessages;

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        HomeTabFragment homeTabFragment = new HomeTabFragment();
        homeTabFragment.addToAdapter(listMessages);

        setFragment(homeTabFragment);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


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

    /*@Override
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

    Fragment fragment;
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Log.i(LOG_TAG, "New intent");
        Log.i(LOG_TAG, intent.getAction() + " " + navigation.getSelectedItemId() + " " + R.id.navigation_read);

        // Si l'activité est déclenchée par un Intent
        if (navigation.getSelectedItemId() == R.id.navigation_read)
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

                    saveMessages(listMessages);

                }
            }
    }

    public <T> void saveMessages(ArrayList<T> list){
        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        mEditor.putString(saved_var_id, json);
        mEditor.apply();
        Log.i(LOG_TAG, "Saving messages");
    }

    public void setFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.content, fragment).commit();
    }


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
