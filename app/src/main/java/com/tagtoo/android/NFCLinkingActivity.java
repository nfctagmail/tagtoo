package com.tagtoo.android;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

public class NFCLinkingActivity extends Activity {

    public static int NFC_INTENT_CODE = 101;
    private static final String LOG_TAG = "NFC_LINK";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED) || getIntent().getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED) || getIntent().getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {

            Intent newIntent = new Intent("READ_TAG");


            Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] tagId =  tag.getId();
            String tagSerialNbr = bytesToHexString(tagId);
            Log.i(LOG_TAG, "Tag serial number : " + tagSerialNbr);
            String nameFileTag = "TagSN_" + tagSerialNbr;


            newIntent.putExtra("TAG_SERIAL", nameFileTag);

            // On récupère le(s) message(s). Parcelable sert à envoyer des données à travers des Intents, d'activité à activité par exemple
            Parcelable[] rawMessages = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            // S'il y a un/des message(s)
            if (rawMessages != null) {

                Log.i(LOG_TAG, "Tag contient un message");

                // On récupère le premier message contenu dans le Parcelable
                NdefMessage messages = (NdefMessage) rawMessages[0];

                // On le change en chaîne de caractères
                String stringMessage = new String(messages.getRecords()[0].getPayload());

                newIntent.putExtra("TAG_MESSAGE", stringMessage);

            }
            sendBroadcast(newIntent);

            finish();
        }
        else {
            finish();
        }
    }

    private String bytesToHexString(byte[] src) {
        // On va créer une chaine de caractères à laquelle on ajoutera au fur et à mesure des caractères. Elle commence par "0x"
        StringBuilder stringBuilder = new StringBuilder("0x");

        // On arrête tout si l'argument donné est null ou a une longueur null
        if (src == null || src.length <= 0) {
            return null;
        }

        // On ajoutera les caractères par groupe de deux
        char[] buffer = new char[2];

        // Pour chaque octet de l'argument
        for (int i = 0; i < src.length; i++) {

            // Traitement du premier caractère puis du deuxième caractère, pour le même octet, les 4 premiers bits correspondront à un caractère en héxadécimal, puis les 4 autres
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);      // Dans l'octet src[i] on prend seulement les 4 bits les plus élevés (= à gauche) et on les compare (& = AND) à 0x0F c'est à dire 15 ou encore 0000 1111 (on ne garde donc que les bits de droite)
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);              // Character.forDigit() nous donne alors l'équivalent du chiffre binaire de gauche en base 16, donc en héxadécimal

            // On ajoute ces deux caractères au reste de la chaine
            stringBuilder.append(buffer);
        }

        // On retourne la chaine de caractères totale
        return stringBuilder.toString();
    }


}
