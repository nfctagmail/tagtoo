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

    // Chaine de caractères pour identifier les messages de cette activité dans la console
    private static final String LOG_TAG = "NFC_LINK";

    // On ne fait rien lorsque l'activité est créée : elle n'affiche rien de toute façon (@Override = reprendre la fonction orginale de Activity pour la réécrire selon ce fichier)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            }

    // Quand l'activité est déclenchée
    @Override
    protected void onStart() {
        // On rappelle quand même la fonction originale avec "super" pour éxécuter ses fonctions par défaut
        super.onStart();

        // On récupère l'intention qui l'a démarrée et si elle correspond à une des 3 intentions de tag NFC on continue
        if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED) || getIntent().getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED) || getIntent().getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {

            Intent newIntent = new Intent("READ_TAG");
            Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] tagId =  tag.getId();
            String tagSerialNbr = bytesToHexString(tagId);
            Log.i(LOG_TAG, "Tag serial number : " + tagSerialNbr);
            String nameFileTag = "TagSN_" + tagSerialNbr;
            newIntent.putExtra("TAG_SERIAL", nameFileTag);
            Parcelable[] rawMessages = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (rawMessages != null) {
                Log.i(LOG_TAG, "Tag contient un message");
                NdefMessage messages = (NdefMessage) rawMessages[0];
                String stringMessage = new String(messages.getRecords()[0].getPayload());
                newIntent.putExtra("TAG_NAME", stringMessage);
            }
            sendBroadcast(newIntent);
            finish();
        }
        else {
            finish();
        }
    }

    // Fonction pour convertir une source (src) d'octets en chaîne de caractères hexadecimale, qui correspond au numéro de série
    private String bytesToHexString(byte[] src) {
        // On va créer une chaine de caractères à laquelle on ajoutera au fur et à mesure des caractères. Elle commence par "0x"
        StringBuilder stringBuilder = new StringBuilder("0x");
        // On arrête tout si l'argument donné est null ou a une longueur null
        if (src == null || src.length <= 0) {
            return null;
        }
        // On ajoutera les caractères par groupe de deux en créant un tableau de 2 caractères
        char[] tampon = new char[2];

        // Pour chaque octet de l'argument
        for (int i = 0; i < src.length; i++) {
            // Traitement du premier caractère puis du deuxième caractère, pour le même octet, les 4 premiers bits correspondront à un caractère en héxadécimal, puis les 4 autres un autre
            // Character.forDigit() nous donne l'équivalent du chiffre binaire en base 16, en héxadécimal
            // 1. dans l'octet src[i], >>> 4 prend seulement les 4 bits les plus élevés (= à gauche), par exemple : à partir de 1010 1111 on obtient 0000 1010
            // on les compare (& = opérat° binaire AND : 1 ou 0 AND 0 -> 0; 1 AND 1 -> 1) à 0F (16) = 15 (10) = 0000 1111 (2) (on ne garde donc que les bits de droite)
            tampon[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            tampon[1] = Character.forDigit(src[i] & 0x0F, 16);
            // On ajoute ces deux caractères au reste de la chaine
            stringBuilder.append(tampon);
        }

        // On retourne la chaine de caractères totale en tant que résultat de la fonction
        return stringBuilder.toString();
    }

}
