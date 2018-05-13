package com.tagtoo.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

// Activité d'écriture d'image sur le tag, WIP
public class WriteImageActivity extends AppCompatActivity {

    // Quand l'activité est créée (@Override = reprendre la fonction orginale de AppCompatActivity pour la réécrire selon ce fichier)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // On rappelle quand même la fonction originale avec "super" pour éxécuter ses fonctions par défaut
        super.onCreate(savedInstanceState);
        // On attribue le fichier xml à cette activité
        setContentView(R.layout.activity_write_image);
    }
}
