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

public class HelpActivity extends AppCompatActivity {

    /**
     * PagerAdapter fournit un fragment pour chaque page (dérivé de FragmentPagerAdapter sauf qu'il garde tous les fragments en mémoire)
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * ViewPager gère le contenu de chaque section (page), représente l'ensemble des pages, contrairement au PagerAdapter.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Initialisation de l'daptateur qui va gérer chaque fragment, l'assigner à une page
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Initialisation de ViewPager avec le PagerAdapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    /**
     * Un fragment contenant
     */
    public static class HelpFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public HelpFragment() {
        }

        /**
         * Quand on crée une nouvelle instance de ce fragment, on obtient aussi le numéro de la page
         */
        public static HelpFragment newInstance(int sectionNumber) {
            HelpFragment fragment = new HelpFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_help, container, false);
            int currentPage = getArguments().getInt(ARG_SECTION_NUMBER);
            TextView title = rootView.findViewById(R.id.section_title);
            TextView desc = rootView.findViewById(R.id.section_desc);
            Button endButton = rootView.findViewById(R.id.button_end);
            endButton.setVisibility(View.GONE);
            switch (currentPage){
                case 1:
                    title.setText(R.string.section_help_tag);
                    desc.setText(R.string.section_help_desc_tag);
                    break;
                case 2:
                    title.setText(R.string.section_help_write);
                    desc.setText(R.string.section_help_desc_write);
                    break;
                case 3:
                    title.setText(R.string.section_help_read);
                    desc.setText(R.string.section_help_desc_read);
                    break;
                case 4:
                    title.setText(R.string.section_help_home);
                    desc.setText(R.string.section_help_desc_home);
                    endButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    title.setText("There was a problem.");
                    desc.setText("There was a problem.");
                    break;
            }

            endButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().finish();
                }
            });


            return rootView;
        }
    }

    /**
     * SectionsPagerAdapter renvoie un fragment selon la page
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Quand ViewPager veut savoir quel Fragment (item) on renvoie une nouvelle instance de PLaceholderFragment avecle numéro de la page (0+1, 1+1 ou 2+1; page 1, 2, ou 3)
            return HelpFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Nombre total de pages à afficher.
            return 4;
        }
    }
}
