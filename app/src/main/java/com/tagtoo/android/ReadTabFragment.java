package com.tagtoo.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// Fragment correspondant à l'onglet "Lire" (=bout d'activité inséré dans une autre activité)
public class ReadTabFragment extends Fragment {

    // Quand le fragment est créé
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // On retourne les éléments du fichier xml "fragment_read" selon les arguments originaux de la fonction
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

}
