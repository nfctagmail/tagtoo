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

// On déclare l'activité principale. Activité = "page" de l'application. Basée sur la class AppCompatActivity
public class MainActivity extends AppCompatActivity {

    // Variable contenant le contexte de l'activité à partir duquel on peut utiliser toutes les fonctions relatives à celle-ci
    private static Context context;

    // Listes qui contiendront les messages que l'on a reçu des tags
    // ces listes contiennent seulement des objets SavedMessage
    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();
    private static ArrayList<SavedMessage> gsonMessages = new ArrayList<>();

    // Variable de la barre de navigation du bas de l'activité
    private BottomNavigationView navigation;

    // Les variables qui contiennent les codes pour identifier ...
    private static String LOG_TAG = "MAIN_ACTIVITY";                // Les messages de cette activité dans la console
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";    // Retrouver les préférences dans lesquels on stocke les messages reçus
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";   // Retrouver la variable les contenant dans ces préférences

    // La variable qui contiendra le numéro de série du tag
    private String tagSerialNbr = null;
    // La variable qui contiendra le message porté par le tag
    private String tagMessage = null;

    // Quand l'activité est créée (@Override = reprendre la fonction orginale de AppCompatActivity pour la réécrire selon ce fichier)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // On rappelle quand même la fonction originale avec "super" pour éxécuter ses fonctions par défaut
        super.onCreate(savedInstanceState);
        //On attribue le layout xml "activity_main" à cette activité
        setContentView(R.layout.activity_main);

        // On récupère le "contexte" de l'activité à partir duquel on peut utiliser toutes les fonctions relatives à celle-ci
        MainActivity.context = getApplicationContext();

        // On autorise cette activité à accéder à internet, on permet toutes les actions sur le réseau
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // On récupère les messages enregistrés grâce à SharedPreferences
        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);      // On récupère les préférences de l'application correspondant à l'identifiant stocké dans saved_prefs_id
        Gson gson                = new Gson();                                              // On crée une nouvelle instance de Gson, permettant de convertir notre liste d'objets complexe en Json et inversement
        String messagesSaved     = mPrefs.getString(saved_var_id, null);                // On récupère les données correspondant à l'identifiant stocké dans saved_var_id
        Type type                = new TypeToken<ArrayList<SavedMessage>>() {}.getType();   // On crée le type correspondant à celui de la liste
        gsonMessages             = gson.fromJson(messagesSaved, type);                      // On récupère les données en json que l'on met au type de la liste et que l'on stocke dans une version temporaire de la liste de messages
        if(gsonMessages != null)                                                            // Si cette version temporaire de la liste n'est pas nulle
            listMessages = gsonMessages;                                                    // On donne sa valeur à la véritable liste.


        // On attribue chaque clic sur une option de la barre de navigation à un fragment à ouvrir
        // (Fragment = bout d'activité que l'on peut lui-même insérer dans une activité)
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

        // Quand on clique sur le bouton d'aide, on démarre l'activité d'aide
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHelpActivity();
            }
        });


        // On crée un "receveur d'émissions" qui récupérera pour cette activité-ci toutes les infos du tag récupérées par l'activité NFCLinkingActivity
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                // ces infos sont portés dans l'intention (intent) envoyé par l'activité NFCLinkingActivity
                String action = intent.getAction();
                // si on est dans l'onglet de lecture du tag
                if (navigation.getSelectedItemId() == R.id.navigation_read)
                    // si l'intention transmise est bien celle de lecture du tag
                    if (action.equals("READ_TAG")) {
                        // on récupère le contenu ce tag
                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL"); // numéro de série, correspondant à l'identifiant TAG_SERIAL dans l'intention
                        Log.i(LOG_TAG, "TAG DATA : " + tagSerialNbr);        // on l'affiche dans la console
                        tagMessage = intent.getStringExtra("TAG_MESSAGE");  // le message, correspondant à l'identifiant TAG_MESSAGE dans l'intention
                        Log.i(LOG_TAG, "TAG DATA : " + tagMessage);          // on l'affiche dans la console

                        // On récupère la date sous la forme d'une chaîne de caractères mise en forme correctement par Android
                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        // Et sous forme de secondes, pour donner un nom unique au fichier téléchargé, pour que pour un même tag on ait différent fichiers audio à chaque téléchargement
                        Long time = System.currentTimeMillis()/1000;
                        String timeString = time.toString();

                        // On crée le message qui s'affichera sur l'accueil
                        // Si on parvient à télécharger un fichier audio du serveur, on ajoute à l'objet SavedMessage le nom de ce fichier
                        if (downloadFile(tagSerialNbr, timeString))
                            listMessages.add(new SavedMessage(tagMessage, tagSerialNbr, currentDateTimeString, tagSerialNbr + "_download" + time + ".3gp"));
                        else
                            listMessages.add(new SavedMessage(tagMessage, tagSerialNbr, currentDateTimeString));

                        // On crée une nouvelle instance de HomeTabFragment
                        HomeTabFragment homeTabFragment = new HomeTabFragment();
                        // On l'ajoute à l'adaptateur, ce qui gère chaque entrée de la liste de l'onglet d'accueil,
                        // grâce à la fonction addToAdapter utilisant la valeur de la variable globale listMessages que l'on vient de modifier
                        homeTabFragment.addToAdapter(listMessages);
                        // On change de section
                        navigation.setSelectedItemId(R.id.navigation_home);
                        // On sauvegarde les messages
                        saveMessages(listMessages);
                    }
            }
        };

        // On enregistre le receveur de l'émetteur déclaré précédemment,
        // qui va prendre les infos contenues dans l'intention "READ_TAG" lorsqu'elle est émise par une autre activité
        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));

    }

    public void startHelpActivity(){
        // On crée une intention qui est de passer de cette activité à HelpActivity
        Intent intent = new Intent(this, HelpActivity.class);
        // Puis on l'éxécute
        startActivity(intent);
    }

    // Fonction pour sauvegarder en dehors de la mémoire vive les messages que l'on ajoute à l'accueil
    public <T> void saveMessages(ArrayList<T> list){
        SharedPreferences mPrefs = context.getSharedPreferences(saved_prefs_id, 0);  // On récupère les préférences de l'application correspondant à l'identifiant stocké dans saved_prefs_id
        SharedPreferences.Editor mEditor = mPrefs.edit();                               // On accède à l'édition de ces préférences
        Gson gson = new Gson();                                                         // On crée une nouvelle instance de Gson, permettant de convertir notre liste d'objets complexe en Json et inversement
        String json = gson.toJson(list);                                                // On convertit en json la liste donnée en argument
        mEditor.putString(saved_var_id, json);                                          // On la stocke dans les préférences sous l'identifiant contenu dans saved_var_id
        mEditor.apply();                                                                // On valide les modifications
        Log.i(LOG_TAG, "Saving messages");      // On envoie à la console un message disant qu'on a éxécuté la sauvegarde des messages
    }

    // Fonction pour télécharger un fichier audio depuis le serveur selon le numéro de série du tag
    // Elle est booléenne pour savoir si vrai ou faux on a reçu un fichier
    private boolean downloadFile(String serialNbr, String time){

        // On crée la variable du client FTP (on utilise le FTP pour télécharger le fichier)
        FTPClient ftp = null;

        // On crée la variable contenant l'emplacement ou le fichier sera sauvegardé (cache de l'app)
        File directoryCache = new File(getExternalCacheDir().getAbsolutePath());
        File audioToDownload  = new File(directoryCache, serialNbr + "_download" + time + ".3gp");

        // "try" sert à éviter le crash de l'app si une des erreurs gérées par "catch" (plus bas) venait à se produire
        try{
            // On crée une nouvelle instance du client FTP
            ftp = new FTPClient();
            // On détermine le serveur (SendMessageActivity n'est pas inclue dans les annexes car contient seulement les codes du serveur)
            ftp.connect(SendMessageActivity.verser);
            // On envoie un message à la console pour expliquer ce que l'on fait
            Log.i(LOG_TAG, "Trying to connect to the server");
            // On tente la connexion avec utilisateur et mot de passe
            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                // Message à la console
                Log.i(LOG_TAG, "Connection to the server successful");
                // On choisit le mode d'action (de base, recommandé)
                ftp.enterLocalPassiveMode();
                // et on modifie les fichiers avec des informations bianires (et non ASCII par exemple)
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                // On crée un flux de fichiers sortants, qui arrivent à l'emplacement désigné plus tôt par audioToDownload
                FileOutputStream fileOutput = new FileOutputStream(audioToDownload);
                // si l'on arrive à récupérer le fichier a. sur le serveur, stocké là où fileOutput b. nous le dit / (a, b)
                boolean result = ftp.retrieveFile("/" + serialNbr + ".3gp", fileOutput);
                // on ferme le flux de fichier
                fileOutput.close();
                // si on a réussi à télécharger le fichier...
                if(result) {
                    // message dans la console + déconnection de l'utilisateur et du serveur
                    Log.i(LOG_TAG, "Success downloading from server");
                    ftp.logout();
                    ftp.disconnect();
                    // et la fonction s'arrête ici et elle renvoie "vrai"
                    return true;
                } else {
                    // sinon déconnection de l'utilisateur et du serveur
                    ftp.logout();
                    ftp.disconnect();
                    // la fonction s'arrête ici, il n'y a pas de fichiers
                    return false;
                }
            }
            // Si on n'a pas pu se connecter au serveur on le dit
            else
                Log.e(LOG_TAG, "Could not connect to server");
        }
        // on récupère toutes les erreurs et on les affiche dans la console, et on termine la fonction
        catch (SocketException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
            return false;
        }
        // Si rien n'a pu se passer comme il faut, on termine avec "faux" la fonction
        return false;
    }

    // Fonction pour déterminer le fragment à afficher
    public void setFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();                   // On récupère le gérant de fragment
        fm.beginTransaction().replace(R.id.content, fragment).commitAllowingStateLoss();     // On fait une transaction de fragment en remplaçant celui qui est dans la partie "content" = "contenu" de la disposition par celui donné en arguement
    }
}
