package com.tagtoo.android;

import android.app.DialogFragment;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
// Boîte de dialogue qui s'affiche quand on veut envoyer un message sur le tag
public class BeamDialog extends DialogFragment
{

    String message; // Variable globale

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
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog);

        message = getArguments().getString("message");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // on lie au fichier xml "dialog_beam_message"
        View rootView = inflater.inflate(R.layout.dialog_beam_message, container, false);
        TextView msgTextView = rootView.findViewById(R.id.message);
        // On affiche le message que l'utilisateur a écrit
        msgTextView.setText(message);

        return rootView;
    }

}
