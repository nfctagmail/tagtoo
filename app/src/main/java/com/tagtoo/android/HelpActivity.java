package com.tagtoo.android;

import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

// Activité expliquant le fonctionnement de l'app, accessible depuis MainActivity
public class HelpActivity extends AppCompatActivity {

    // PagerAdapter fournit un fragment pour chaque page (dérivé de FragmentPagerAdapter sauf qu'il garde tous les fragments en mémoire)
    private SectionsPagerAdapter mSectionsPagerAdapter;
    // ViewPager gère le contenu de chaque section (page), représente l'ensemble des pages, contrairement au PagerAdapter,
    // permet de glisser vers la gauche/droite pour faire défiler les fragmentds
    private ViewPager mViewPager;

    // Fonction appelée quand l'activité est créée
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Attribue le layout (disposition, affichage) xml "activity_help" à cette activité
        setContentView(R.layout.activity_help);
        // Initialisation de l'adaptateur qui va gérer chaque fragment, l'assigner à une page
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Initialisation de ViewPager avec le PagerAdapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    // Un fragment avec le contenu de chaque page
    public static class HelpFragment extends Fragment {

        // Une chaîne de caractères pour reconnaitre l'argument passé lorsqu'on crée la nouvelle instance
        private static final String ARG_NUMERO_SECTION = "section_numero";

        // Quand on crée une nouvelle instance de ce fragment, on obtient aussi le numéro de la page
        public static HelpFragment newInstance(int sectionNum) {
            // nouvelle instance du fragment
            HelpFragment fragment = new HelpFragment();
            // nouvel ensemble d'arguments que l'on pourra passer ensuite aux fonctions du fragment une fois créé
            Bundle args = new Bundle();
            // on y ajoute le numéro du fragment que l'on veut afficher, qui est argument de cette fonction
            args.putInt(ARG_NUMERO_SECTION, sectionNum);
            // le fragment prend donc le numéro de la section en argument
            fragment.setArguments(args);
            // on renvoie l'instance du fragment, auquel on a joint l'argument, en tant que résultat de la fonction
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // On récupère les éléments du fichier xml de disposition "fragment_help"
            View rootView = inflater.inflate(R.layout.fragment_help, container, false);
            // On récupère l'argument du fragment qui a été désigné lors de la création de cette instance du fragment,
            // qui nous dit le numéro de page
            int currentPage = getArguments().getInt(ARG_NUMERO_SECTION);

            // On récupère les éléments dont on veut modifier les attributs selon la page
            TextView titre = rootView.findViewById(R.id.section_title);
            TextView desc = rootView.findViewById(R.id.section_desc);
            Button endButton = rootView.findViewById(R.id.button_end);
            // On n'affiche pas le bouton de fin de base, que pour la dernière page
            endButton.setVisibility(View.GONE);

            // On attribue le titre, l'image et la description à chacune des 4 pages
            switch (currentPage){
                // Page sur le tag NFC
                case 1:
                    titre.setText(R.string.section_help_tag);
                    desc.setText(R.string.section_help_desc_tag);
                    break;
                // Page sur l'écriture d'un tag
                case 2:
                    titre.setText(R.string.section_help_write);
                    desc.setText(R.string.section_help_desc_write);
                    break;
                // Page sur la lecture d'un tag
                case 3:
                    titre.setText(R.string.section_help_read);
                    desc.setText(R.string.section_help_desc_read);
                    break;
                case 4:
                    titre.setText(R.string.section_help_home);
                    desc.setText(R.string.section_help_desc_home);
                    endButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    // Dans le cas ou le numéro de la page n'est pas un de ceux au-dessus, on affiche un texte pour dire qu'il y a un problème
                    titre.setText("There was a problem.");
                    desc.setText("There was a problem.");
                    endButton.setVisibility(View.VISIBLE);
                    break;
            }
            // On termine l'activité quand on clique sur le bouton de fin
            endButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().finish();
                }
            });

            // Résultat de la fonction : tous les éléments desquels on vient de modifier les attributs
            return rootView;
        }
    }


    // SectionsPagerAdapter renvoie un fragment selon la page
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Quand ViewPager veut savoir quel Fragment (item) on renvoie une nouvelle instance de PlaceholderFragment
            // avec le numéro de la page (0+1, 1+1 ou 2+1; page 1, 2, ou 3)
            return HelpFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Nombre total de pages à afficher.
            return 4;
        }
    }
}
