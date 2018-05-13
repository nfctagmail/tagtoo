package com.tagtoo.android;

import android.nfc.NfcAdapter;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

// Fragment correspondant à l'onglet accueil (=bout d'activité inséré dans une autre activité)
public class HomeTabFragment extends Fragment {

    // Liste qui contient les messages que l'on veut afficher
    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();
    // Chaîne de caractères pour identifier les messages de la console venant de ce fragment
    private static String LOG_TAG = "TAB_HOME";

    // Fonction appelée à la création du fragment : renvoie tous les éléments à afficher
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Définition de la variable correspondant à l'objet View renvoyé par l'"inflater" : il lie les fichiers xml à tous ses objets View correspondants (rootView contient tous les objets View de la disposition).
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // On récupère l'adaptateur NFC du téléphone, qui va permettre de communiquer avec le tag
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        // On récupère les éléments de Layout nécessaires, que l'on va utiliser (la liste, le texte, le bouton pour actualiser et le bouton d'aide)
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        TextView alerttv          = rootView.findViewById(R.id.alertTextView);
        Button refresh            = rootView.findViewById(R.id.refreshButton);
        final FloatingActionButton fab  = getActivity().findViewById(R.id.fab);

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

            // On quitte l'app avec un message d'erreur affiché à l'utilisateur
            Toast.makeText(getActivity(), "Your device is not compatible with NFC.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
        // Si le nfc est désactivé
        else if(!mNfcAdapter.isEnabled())
        {
            Log.i(LOG_TAG, "NFC is disabled");

            recyclerView.setVisibility(View.GONE);  // On n'affiche pas la liste
            alerttv.setVisibility(View.VISIBLE);    // On affiche le texte d'alerte
            alerttv.setText(R.string.nfc_disabled); // Le texte affiche que le NFC est désactivé
            refresh.setVisibility(View.VISIBLE);    // Un bouton "Rafraichir" apparait pour actualiser l'app si le NFC est activé
            fab.setVisibility(View.VISIBLE);        // On affiche le bouton d'aide
        }
        else if(listMessages.isEmpty())
        {
            alerttv.setVisibility(View.VISIBLE);    // On affiche le texte d'alerte
            alerttv.setText(R.string.list_empty);   // On affiche le texte par défaut
            fab.setVisibility(View.VISIBLE);        // On affiche le bouton d'aide
        }

        // Action du clic sur le bouton "Rafraichir" =  redémarrer l'activité
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();                     // On arrête l'activité
                startActivity(getActivity().getIntent());   // On démarre l'activité
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // On fait glisser la liste vers le bas (dy positif) : on cache le bouton d'aide avec une fonction fournie par Android qui anime cette disparition
                if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
                    fab.hide();
                }
                // On fait glisser la liste vers le haut (dy négatif) : on affiche le bouton d'aide avec une fonction fournie par Android qui anime cette apparition
                else if (dy < 0 && fab.getVisibility() != View.VISIBLE) {
                    fab.show();
                }
            }
        });
        // On affiche dans la console que la longue configuration de ce fragment est terminée
        Log.i(LOG_TAG, "All set");

        // Résultat de la fonction : tous les éléments desquels on vient de modifier les attributs
        return rootView;
    }

    // Fonction pour ajouter un message à la liste
    public void addToAdapter(ArrayList<SavedMessage> list){
        listMessages    = list;                                                 // La liste propre à ce fragment devient celle renseignée en argument de la fonction
        MessagesAdapter messagesAdapter;
        messagesAdapter = new MessagesAdapter(getActivity(), listMessages);     // On crée une nouvelle instance de l'adaptateur des messages de la liste auquel on donne ces messages
        messagesAdapter.setMessages(listMessages);                              // On donne la liste des messages
        messagesAdapter.notifyDataSetChanged();                                 // On actualise la liste (en lui disant que ses données ont changé)
    }

}
