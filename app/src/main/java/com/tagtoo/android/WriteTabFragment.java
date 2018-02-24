package com.tagtoo.android;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.io.IOException;

public class WriteTabFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    /*public void beamMessage() {
        // Commencer la transaction (càd la création/suppression/remplacement) de fragments d'activités
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // S'il y a déjà un fragment au tag égal à "beam"
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        //On le supprime
        if (prev != null) {
            ft.remove(prev);
        }
        // On ajoute la transaction au "back stack" qui tient la liste des transactions pour qu'elles puissent ensuite être annulées, en appuyent par exemple sur le bouton retour
        ft.addToBackStack(null);

        editText = findViewById(R.id.editText);
        DialogFragment beamDialog = BeamDialog.newInstance(editText.getText().toString());
        beamDialog.show(ft, "beam");
    }*/


    /*private void write(String text, Tag tag) throws IOException, FormatException {
        // Création du message en format NDEF avec le texte saisi par l'utilisateur encodés en octets de type MIME (ex: image/jpeg) corrzspondant à celui de l'application
        NdefMessage message = new NdefMessage(new NdefRecord[]{NdefRecord.createMime("application/com.tagtoo.android", text.getBytes())});

        // Création d'une instance du tag
        Ndef ndef = Ndef.get(tag);
        // Activation de la connection téléphone/tag
        ndef.connect();
        // On y écrit le message
        ndef.writeNdefMessage(message);
        // Désactivation de la connection téléphone/tag
        ndef.close();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            final EditText message = findViewById(R.id.editText);

            try {
                if(myTag != null){
                    write(message.getText().toString(), myTag);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onPause(){
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, writeTagFilters, null);
    }*/

}
