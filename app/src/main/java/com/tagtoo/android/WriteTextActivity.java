package com.tagtoo.android;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class WriteTextActivity extends AppCompatActivity {

    EditText editText;
    TextView counter;
    FloatingActionButton sendButton;

    NfcAdapter mNfcAdapter;
    PendingIntent mPendingIntent;
    IntentFilter writeTagFilters[];
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_text);

        context = this;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        editText = findViewById(R.id.editText);

        counter = findViewById(R.id.textCompteur);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textCounter = String.valueOf(editText.getText().length()) + "/100";
                counter.setText(textCounter);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beamMessage();
            }
        });

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };


    }

    public void beamMessage() {
        // Commencer la transaction (càd la création/suppression/remplacement) de fragments d'activités
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // S'il y a déjà  un fragment au tag égal à "beam"
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        //On le supprime
        if (prev != null) {
            ft.remove(prev);
        }
        // On ajoute la transaction au "back stack" qui tient la liste des transactions pour qu'elles puissent ensuite être annulées, en appuyant par exemple sur le bouton retour
        ft.addToBackStack(null);


        editText = findViewById(R.id.editText);
        // On crée une instance la boîte de dialogue que l'on veut afficher, à laquelle on envoie le texte écrit
        DialogFragment beamDialog = BeamDialog.newInstance(editText.getText().toString());
        // On affiche la boîte de dialogue, à laquelle on donne le tag "beam"
        beamDialog.show(ft, "beam");
    }


    private void write(String text, Tag tag) throws IOException, FormatException {
        Log.i("WRITING", text);

        // Création du message en format NDEF avec le texte saisi par l'utilisateur encodé en octets de type MIME (ex: image/jpeg) correspondant à  celui de l'application
        NdefMessage message = new NdefMessage(new NdefRecord[]{NdefRecord.createMime("application/com.tagtoo.android", text.getBytes())});

        Ndef ndef = Ndef.get(tag);      // Création d'une instance du tag
        ndef.connect();                 // Activation de la connection téléphone/tag
        ndef.writeNdefMessage(message); // On y écrit le message
        ndef.close();                   // Désactivation de la connection téléphone/tag
    }

    Tag myTag;
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        // Si l'utilisateur a cliqué sur le bouton pour afficher la boîte de dialogue
        if(getFragmentManager().findFragmentByTag("beam") != null) {
            // Si l'action de l'intention correspond à un tag NFC
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

                // On récupère les informations du tag, contenues dans l'intention
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                // On récupère le message écrit par l'utilisateur
                final EditText message = findViewById(R.id.editText);

                // On essaye de lancer la fonction qui écrit sur le tag (seulement si un tag a bien été découvert)
                try {
                    if (myTag != null) {
                        write(message.getText().toString(), myTag);
                        Toast.makeText(context, "Message écrit avec succès", Toast.LENGTH_LONG ).show();
                        finish();
                    }
                    else {
                        Toast.makeText(context, "Erreur, tag non détécté", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, "Erreur lors de la connexion", Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, "Erreur dans le message que vous avez écrit", Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  Quand l'activité passe en arrière-plan on désactive l'envoi du message
     */
    @Override
    public void onPause(){
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    /**
     *  Quand l'activité repasse en premier-plan on active l'envoi du message
     */
    @Override
    public void onResume(){
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, writeTagFilters, null);
    }

}
