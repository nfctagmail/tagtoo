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
    TextView textCompteur;
    FloatingActionButton sendButton;

    NfcAdapter mNfcAdapter;
    PendingIntent mPendingIntent;
    IntentFilter writeTagFilters[];
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_text);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);


        editText = findViewById(R.id.editText);

        textCompteur = findViewById(R.id.textCompteur);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textCompteur.setText(String.valueOf(editText.getText().length()) + "/100");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sendButton = findViewById(R.id.sendButton);

        context = this;

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beamMessage();
                try {
                    if(myTag ==null) {
                        Toast.makeText(context, "ERREUR", Toast.LENGTH_LONG).show();
                    } else {
                        write(editText.getText().toString(), myTag);
                        Toast.makeText(context, "SUCCESS", Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, "ERREUR", Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, "ERREUR", Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }

            }
        });

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };


    }

    public void beamMessage() {
        // Commencer la transaction (càd la crÃ©ation/suppression/remplacement) de fragments d'activitÃ©s
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // S'il y a déjà  un fragment au tag Ã©gal Ã  "beam"
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        //On le supprime
        if (prev != null) {
            ft.remove(prev);
        }
        // On ajoute la transaction au "back stack" qui tient la liste des transactions pour qu'elles puissent ensuite Ãªtre annulÃ©es, en appuyent par exemple sur le bouton retour
        ft.addToBackStack(null);

        editText = findViewById(R.id.editText);
        DialogFragment beamDialog = BeamDialog.newInstance(editText.getText().toString());
        beamDialog.show(ft, "beam");
    }


    @SuppressLint("NewApi")
    private void write(String text, Tag tag) throws IOException, FormatException {
        Log.i("WRITE", text);
        // CrÃ©ation du message en format NDEF avec le texte saisi par l'utilisateur encodÃ©s en octets de type MIME (ex: image/jpeg) corrzspondant Ã  celui de l'application
        NdefMessage message = new NdefMessage(new NdefRecord[]{NdefRecord.createMime("application/com.tagtoo.android", text.getBytes())});
        // CrÃ©ation d'une instance du tag
        Ndef ndef = Ndef.get(tag);
        // Activation de la connection tÃ©lÃ©phone/tag
        ndef.connect();
        // On y Ã©crit le message
        ndef.writeNdefMessage(message);
        // DÃ©sactivation de la connection tÃ©lÃ©phone/tag
        ndef.close();
    }

    Tag myTag;
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
    }

}
