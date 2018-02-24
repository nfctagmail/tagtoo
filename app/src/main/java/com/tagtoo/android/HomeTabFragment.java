package com.tagtoo.android;

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

        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //messagesAdapter = new MessagesAdapter(listMessages);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        TextView alerttv          = rootView.findViewById(R.id.alertTextView);
        Button refresh            = rootView.findViewById(R.id.refreshButton);


        if(!listMessages.isEmpty()) {
            Log.i(LOG_TAG, "Loading the list of messages");
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setAdapter(new MessagesAdapter(getActivity(), listMessages));
        }

        alerttv.setVisibility(View.GONE);
        refresh.setVisibility(View.GONE);

        // Si le téléphone n'a pas de NFC
        if(mNfcAdapter == null) {
            Log.i(LOG_TAG, "No NFC adapter");
            recyclerView.setVisibility(View.GONE);
            alerttv.setVisibility(View.VISIBLE);
            refresh.setVisibility(View.VISIBLE);
            alerttv.setText("Your device is not compatible with NFC.");
            // On quitte l'app avec un message d'erreur.
            Toast.makeText(getActivity(), "Your device is not compatible with NFC.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        // Si le nfc est désactivé
        if(!mNfcAdapter.isEnabled())
        {
            Log.i(LOG_TAG, "NFC is disabled");
            recyclerView.setVisibility(View.GONE);
            alerttv.setVisibility(View.VISIBLE);
            // Le texte affiche qu'il est désactivé
            alerttv.setText("NFC is disabled in the Settings. Activate it to be able to use this app.");
            // Un bouton "Rafraichir" apparait pour actualiser l'app si le NFC est activé
            refresh.setVisibility(View.VISIBLE);
        }
        else if(listMessages.isEmpty())
        {
            alerttv.setVisibility(View.VISIBLE);
            alerttv.setText("You have no saved NFC messages, tap the help button at the bottom of your screen to know how to add one.");
        }

        // Action du bouton "Rafraichir"
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
                startActivity(getActivity().getIntent());
            }
        });

        Log.i(LOG_TAG, "All set");

        return rootView;
    }

    public void addToAdapter(ArrayList<MainActivity.SavedMessage> list){
        listMessages    = list;
        messagesAdapter = new MessagesAdapter(getActivity(), listMessages);
        messagesAdapter.setMessages(listMessages);
        messagesAdapter.notifyDataSetChanged();
    }

}
