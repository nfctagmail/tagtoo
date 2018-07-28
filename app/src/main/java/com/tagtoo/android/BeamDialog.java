package com.tagtoo.android;

import android.app.DialogFragment;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BeamDialog extends DialogFragment
{

    String message;

    static BeamDialog newInstance(String message) {

        BeamDialog fragment = new BeamDialog();
        Bundle args = new Bundle();
        args.putString("message", message);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog);

        message = getArguments().getString("message");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.dialog_beam_message, container, false);
        TextView msgTextView = rootView.findViewById(R.id.message);
        msgTextView.setText(message);

        return rootView;
    }

}
