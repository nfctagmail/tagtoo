package com.tagtoo.android;

import android.nfc.NfcAdapter;
import android.os.Build;
import android.support.annotation.RequiresApi;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class HomeTabFragment extends Fragment {

    private static ArrayList<SavedMessage> listMessages = new ArrayList<>();
    private static String LOG_TAG = "TAB_HOME";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        final FloatingActionButton fab  = getActivity().findViewById(R.id.fab);

        // Setting up RecyclerView
        Log.i(LOG_TAG, "Loading the list of messages");

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(new MessagesAdapter(getActivity(), listMessages));

        // Handling problems with NFC
        if(mNfcAdapter == null)
        {
            Log.i(LOG_TAG, "No NFC adapter");

            Toast.makeText(getActivity(), "Your device is not compatible with NFC.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
        else if(!mNfcAdapter.isEnabled())
        {
            Log.i(LOG_TAG, "NFC is disabled");
        }


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // On fait glisser la liste vers le bas (dy positif) : on cache le bouton d'aide
                if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
                    fab.hide();
                }
                // On fait glisser la liste vers le haut (dy négatif) : on affiche le bouton d'aid
                else if (dy < 0 && fab.getVisibility() != View.VISIBLE) {
                    fab.show();
                }
            }
        });

        Log.i(LOG_TAG, "All set");

        return rootView;
    }

    public void addToAdapter(ArrayList<SavedMessage> list){
        listMessages    = list;                                                 // La liste propre à ce fragment devient celle renseignée en argument de la fonction
        MessagesAdapter messagesAdapter;
        messagesAdapter = new MessagesAdapter(getActivity(), listMessages);     // On crée une nouvelle instance de l'adaptateur des messages de la liste auquel on donne ces messages
        messagesAdapter.setMessages(listMessages);                              // On donne la liste des messages
        messagesAdapter.notifyDataSetChanged();                                 // On actualise la liste (en lui disant que ses données ont changé)
    }

}
