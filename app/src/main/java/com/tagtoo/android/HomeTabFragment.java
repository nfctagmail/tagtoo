package com.tagtoo.android;

import com.tagtoo.android.R;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentTransaction;
import android.nfc.NfcAdapter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class HomeTabFragment extends Fragment {

    private static NfcAdapter mNfcAdapter;

    private static ArrayList<MainActivity.SavedMessage> listMessages = new ArrayList<>();

    private static String LOG_TAG = "TAB_HOME";
    private static String saved_prefs_id = "TAGTOO_SAVED_PREFS";
    private static String saved_var_id = "TAGTOO_SAVED_MESSAGES";
    View rootView;
    MessagesAdapter messagesAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Définition de la variable correspondant à l'objet View renvoyé par l'"inflater" : il lie les fichiers xml à tous ses objets View correspondants (rootView contient tous les objets View du Layout).
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // On récupère l'adaptateur NFC du téléphone, qui va permettre de communiquer avec le tag
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        // On récupère les éléments de Layout nécessaires, que l'on va utiliser (la liste, le texte, le bouton)
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        TextView alerttv          = rootView.findViewById(R.id.alertTextView);
        Button refresh            = rootView.findViewById(R.id.refreshButton);


        // Si la variable contenant la liste
        if(!listMessages.isEmpty()) {
            Log.i(LOG_TAG, "Loading the list of messages");
            // On crée une instance du manager de la disposition du fragment
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            // Disposition verticale des éléments de la liste
            mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            // La liste a une taille fixée
            recyclerView.setHasFixedSize(true);
            // On attribue le LayoutManager à la liste, indispensable pour qu'elle soit affichée
            recyclerView.setLayoutManager(mLayoutManager);
            // On lui attribue un adaptateur qui va afficher les messages en créant chaque ligne individuellement
            recyclerView.setAdapter(new MessagesAdapter(getActivity(), listMessages));
        }

        // Le texte et le bouton ne sont pas affichés par défaut
        alerttv.setVisibility(View.GONE);
        refresh.setVisibility(View.GONE);


        if(mNfcAdapter == null) // Si le téléphone n'a pas de NFC
        {
            // On envoie un message à la console
            Log.i(LOG_TAG, "No NFC adapter");

            //recyclerView.setVisibility(View.GONE);
            //alerttv.setVisibility(View.VISIBLE);
            //refresh.setVisibility(View.VISIBLE);
            //alerttv.setText("Your device is not compatible with NFC.");

            // On quitte l'app avec un message d'erreur.
            Toast.makeText(getActivity(), "Your device is not compatible with NFC.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        // Si le nfc est désactivé
        else if( !mNfcAdapter.isEnabled())
        {
            Log.i(LOG_TAG, "NFC is disabled");

            recyclerView.setVisibility(View.GONE);  // On n'affiche pas la liste
            alerttv.setVisibility(View.VISIBLE);    // On affiche le texte d'alerte
            alerttv.setText(R.string.nfc_disabled); // Le texte affiche qu'il est désactivé
            refresh.setVisibility(View.VISIBLE);    // Un bouton "Rafraichir" apparait pour actualiser l'app si le NFC est activé
        }
        else if(listMessages.isEmpty())
        {
            alerttv.setVisibility(View.VISIBLE);    // On affiche le texte d'alerte
            alerttv.setText(R.string.list_empty);   // On affiche le texte par défaut
        }

        // Action du bouton "Rafraichir"
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();                     // On arrête l'activité
                startActivity(getActivity().getIntent());   // On démarre l'activité
            }
        });

        Log.i(LOG_TAG, "All set");

        return rootView;
    }

    public void addToAdapter(ArrayList<MainActivity.SavedMessage> list){
        listMessages    = list;                                                 // la liste propre à ce fragment devient celle renseignée en argument
        messagesAdapter = new MessagesAdapter(getActivity(), listMessages);     // On crée une nouvelle instance de l'adaptateur des messages de la liste auquel on donne ces messages
        messagesAdapter.setMessages(listMessages);                              // On donne la liste des messages
        messagesAdapter.notifyDataSetChanged();                                 // On actualise la liste (en disant que ses données ont changé)
    }

}
