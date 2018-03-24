package com.tagtoo.android;

import android.app.DialogFragment;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BeamDialog extends DialogFragment // implements NfcAdapter.CreateNdefMessageCallback
{

    String mMessage;

    static BeamDialog newInstance(String message) {

        // On crée la nouvelle instance du DialogFragment
        BeamDialog fragment = new BeamDialog();

        // On ajoute le message au Bundle, pour le récupérer d'une activité à l'autre
        Bundle args = new Bundle();
        args.putString("message", message);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Ne pas recréer le fragment (la boîte de dialogue) quand l'activité change
        setRetainInstance(true);
        // On choisit le style : normal et clair
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.dialog_beam_message, container, false);

        return v;
    }

}
