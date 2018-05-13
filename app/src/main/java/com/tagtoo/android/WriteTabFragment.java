package com.tagtoo.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

// Fragment correspondant à l'onglet "Ecrire" (=bout d'activité inséré dans une autre activité)
public class WriteTabFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Définition de la variable correspondant à l'objet View renvoyé par l'"inflater" : il lie les fichiers xml à tous ses objets View correspondants (rootView contient tous les objets View de ce fichier).
        View rootView = inflater.inflate(R.layout.fragment_write, container, false);

        // On récupère l'objet du bouton d'écriture de texte
        Button buttonText = rootView.findViewById(R.id.buttontext);
        // On attribue une action quand on clique sur ce bouton ...
        buttonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ... lancer l'activité d'écriture de texte grâce à une fonction créée plus bas
                startWriteActivity(WriteTextActivity.class);
            }
        });
        // On récupère l'objet du bouton d'écriture d'audio
        Button buttonAudio = rootView.findViewById(R.id.buttonaudio);
        // On attribue une action quand on clique sur ce bouton ...
        buttonAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ... lancer l'activité d'écriture de message audio grâce à une fonction créée plus bas
                startWriteActivity(WriteAudioActivity.class);
            }
        });

        // On récupère l'objet du bouton d'écriture d'images
        Button buttonImage = rootView.findViewById(R.id.buttonimage);
        // On attribue une action quand on clique sur ce bouton ...
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ... lancer l'activité correspondante grâce à une fonction créée plus bas
                startWriteActivity(WriteImageActivity.class);
            }
        });

        // On récupère l'objet du bouton d'écriture de vidéo
        Button buttonVideo = rootView.findViewById(R.id.buttonvideo);
        // On attribue une action quand on clique sur ce bouton ...
        buttonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ... lancer l'activité correspondante grâce à une fonction créée plus bas
                startWriteActivity(WriteVideoActivity.class);
            }
        });

        // On retourne l'objet View correspondant à tout le fragment
        return rootView;
    }

    // Fonction pour lancer une autre activité
    public void startWriteActivity(final Class<? extends Activity>  activity){
        // Création d'une nouvelle intention : celle d'aller depuis cette activité vers celle donnée en argument de la fonction
        Intent intent = new Intent(getActivity(), activity);
        // On l'exécute
        startActivity(intent);
    }
}
