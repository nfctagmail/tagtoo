package com.tagtoo.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

// Fragment correspondant à l'onglet "Ecrire" (=bout d'activité inséré dans une autre activité)
public class WriteTabFragment extends Fragment {

    boolean isWriteLayoutOpen = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_write, container, false);

        final LinearLayout hideLayout = rootView.findViewById(R.id.hide_view);

        Button writeButton = rootView.findViewById(R.id.writetag_button);
        Button configButton = rootView.findViewById(R.id.newtag_button);

        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isWriteLayoutOpen){
                    Animation slide_up = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
                    slide_up.setDuration(200);
                    slide_up.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            hideLayout.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            startWriteActivity(ConfigureTagActivity.class);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });

                    hideLayout.startAnimation(slide_up);

                    isWriteLayoutOpen = false;
                }
                else
                    startWriteActivity(ConfigureTagActivity.class);

            }
        });

        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isWriteLayoutOpen){
                    Animation slide_down = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
                    slide_down.setDuration(500);
                    slide_down.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            hideLayout.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });

                    hideLayout.startAnimation(slide_down);

                    isWriteLayoutOpen = true;
                }
            }
        });

        ImageButton buttonText = rootView.findViewById(R.id.text_button);
        buttonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWriteActivity(WriteTextActivity.class);
            }
        });

        ImageButton buttonAudio = rootView.findViewById(R.id.audio_button);
        buttonAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWriteActivity(WriteAudioActivity.class);
            }
        });

        ImageButton buttonImage = rootView.findViewById(R.id.picture_button);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWriteActivity(WritePictureActivity.class);
            }
        });

        ImageButton buttonVideo = rootView.findViewById(R.id.video_button);
        buttonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWriteActivity(WriteVideoActivity.class);
            }
        });

        return rootView;
    }

    public void startWriteActivity(final Class<? extends Activity>  activity){
        Intent intent = new Intent(getActivity(), activity);
        startActivity(intent);
    }
}
