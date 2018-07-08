package com.tagtoo.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class TagOverviewActivity extends AppCompatActivity {

    private static Context context;

    ImageView iconText, iconAudio, iconPicture, iconVideo;
    TextView message;
    ImageButton playButton;
    ProgressBar progressBar;
    TextView counter;
    HorizontalScrollView hScrollView;
    LinearLayout textLayout, audioLayout, pictureLayout, videoLayout;
    LinearLayout.LayoutParams layoutParams;
    GestureDetector gestureDetector;
    ArrayList<LinearLayout> layoutsList;
    int idInList, dispWidth, itemWidth, currentPos;
    boolean hasText, hasAudio, hasPicture, hasVideo;

    private MediaPlayer mPlayer = null;
    private String mFileName = null;
    private int duration = 0;
    private boolean isPlaying = false;
    private int MESSAGE_UPDATE_PROGRESS = 42;
    private Handler audioProgressHandler = null;
    private Thread updateAudioProgress = null;

    String serialNbr, date;
    String LOG_TAG = "TAG_OVERVIEW";

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_overview);

        iconText      = findViewById(R.id.messageTextIcon);
        iconAudio     = findViewById(R.id.messageAudioIcon);
        iconPicture   = findViewById(R.id.messagePictureIcon);
        iconVideo     = findViewById(R.id.messageVideoIcon);
        message       = findViewById(R.id.message);
        playButton    = findViewById(R.id.playButton);
        progressBar   = findViewById(R.id.audioProgressBar);
        counter       = findViewById(R.id.progressCounter);
        hScrollView   = findViewById(R.id.hsv);
        textLayout    = findViewById(R.id.textLayout);
        audioLayout   = findViewById(R.id.audioLayout);
        pictureLayout = findViewById(R.id.pictureLayout);
        videoLayout   = findViewById(R.id.videoLayout);

        TagOverviewActivity.context = getApplicationContext();

        getWindow().setSharedElementEnterTransition(new ChangeBounds().setDuration(250));

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dispWidth = size.x;
        itemWidth = (int) (dispWidth - 0.1*dispWidth);
        layoutsList = new ArrayList<>();
        layoutParams = new LinearLayout.LayoutParams(itemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

        gestureDetector = new GestureDetector(this, new SwipeDetector());

        textLayout.setLayoutParams(layoutParams);
        audioLayout.setLayoutParams(layoutParams);
        pictureLayout.setLayoutParams(layoutParams);
        videoLayout.setLayoutParams(layoutParams);

        layoutsList.add(textLayout);
        layoutsList.add(audioLayout);
        layoutsList.add(pictureLayout);
        layoutsList.add(videoLayout);

        hScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        });

        hScrollView.post(new Runnable() {
            @Override
            public void run() {
                hScrollView.scrollTo(0,0);
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(hasAudio)
                    startPlaying();
                else
                    Toast.makeText(context, "No audio", Toast.LENGTH_SHORT).show();
            }
        });

        if(audioProgressHandler == null) {
            audioProgressHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if(msg.what == MESSAGE_UPDATE_PROGRESS) {
                        if (mPlayer != null) {
                            int currentPlayPos = mPlayer.getCurrentPosition();
                            int playDuration = mPlayer.getDuration();
                            int currProgress = ((currentPlayPos * 1000) / playDuration);
                            progressBar.setProgress(currProgress);
                            counter.setText(currentPlayPos / 1000 + "s");

                        }
                    }
                }
            };
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        SavedMessage tagData = (SavedMessage) getIntent().getSerializableExtra("object");

        idInList = getIntent().getIntExtra("id", 0);

        TextView title = findViewById(R.id.card_title);
        TextView info  = findViewById(R.id.card_info);
        title.setText(getIntent().getStringExtra("name"));
        info.setText(getIntent().getStringExtra("info"));

        serialNbr = tagData.serialNbr;
        date = tagData.dateCreated;

        message.setText(tagData.messageText);

        hasText = getIntent().getExtras().getBoolean("hasText");
        hasAudio = getIntent().getExtras().getBoolean("hasAudio");
        hasPicture = getIntent().getExtras().getBoolean("hasPicture");
        hasVideo = getIntent().getExtras().getBoolean("hasVideo");

        if(hasText)
            iconText.setImageTintList(getResources().getColorStateList(R.color.colorPrimary));
        if(hasAudio)
            iconAudio.setImageTintList(getResources().getColorStateList(R.color.colorPrimary));
        if(hasPicture)
            iconPicture.setImageTintList(getResources().getColorStateList(R.color.colorPrimary));
        if(hasVideo)
            iconVideo.setImageTintList(getResources().getColorStateList(R.color.colorPrimary));

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putInt("idTag", idInList);
        outState.putInt("scrollX", hScrollView.getScrollX());
        outState.putInt("scrollY", hScrollView.getScrollY());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.getInt("idTag") == idInList)
        {
            //Toast.makeText(this, "restored pos, " + idInList + " " + savedInstanceState.getInt("idTag"), Toast.LENGTH_SHORT).show();
            int scrollX = savedInstanceState.getInt("scrollX");
            int scrollY = savedInstanceState.getInt("scrollY");
            hScrollView.scrollTo(scrollX, scrollY);
        }

    }



    public int getVisibleViews(int direction) {
        Rect hitRect = new Rect();
        int position = 0;
        int rightCounter = 0;
        for (int i = 0; i < layoutsList.size(); i++) {
            if (layoutsList.get(i).getLocalVisibleRect(hitRect)) {
                if (direction == -1) {
                    position = i;
                    break;
                } else if (direction == 1) {
                    rightCounter++;
                    position = i;
                    if (rightCounter == 2)
                        break;
                }
            }
        }
        return position;
    }


    private void startPlaying() {
        // Démarrage de la lecture du fichier audio spécifié
        mPlayer = new MediaPlayer();
        try {
            mFileName = getExternalCacheDir().getAbsolutePath();
            mFileName += "/Tag_" + serialNbr + "_" + date + ".3gp";
            // on détermine la source audio à jouer et on démarre la lecture
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
            // on est bien en train de jouer un média
            isPlaying = true;
            // si on ne met pas déjà à jour la barre de progrès etc.
            if(updateAudioProgress == null){
                // on crée un nouvel objet qui va enclencher cette mise à jour toutes les 100 ms
                updateAudioProgress = new Thread(){
                    @Override
                    public void run() {
                        try {
                            while(isPlaying) {
                                if(audioProgressHandler != null){
                                    Message msg = new Message();
                                    msg.what = MESSAGE_UPDATE_PROGRESS;
                                    audioProgressHandler.sendMessage(msg);
                                    Thread.sleep(100);
                                }
                            }
                        } catch (InterruptedException e){
                            Log.e(LOG_TAG, e.getMessage(), e);
                        }
                    }
                };
                // on le démarre
                updateAudioProgress.start();
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Preparing audio player failed : maybe file doesn't exist.");
        }



        // Désactiver le bouton Play
        playButton.setEnabled(false);
        playButton.setBackgroundTintList(context.getResources().getColorStateList(R.color.colorDisabled));

        // Quand la lecture est terminée
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // On remet à null le lecteur si ce n'est déjà le cas
                if(mPlayer != null)
                {
                    mPlayer.release();
                    mPlayer = null;
                }

                // la barre de progrès est au max et le compteur affiche la durée de l'audio
                progressBar.setProgress(1000);
                counter.setText(duration + "s");
                // On ne doit plus mettre à jour la barre de progrès, on ne joue plus l'audio
                updateAudioProgress = null;
                isPlaying = false;
                // On peut à nnouveau cliquer sur le bouton play, sa couleur normale revient
                playButton.setEnabled(true);
                playButton.setBackgroundTintList(context.getResources().getColorStateList(R.color.colorPrimary100));


            }
        });



    }

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // if the point where the swipe begin (e1) has an x coordinate lower than where it ends (e2) than it is a left swipe.
            if (e1.getX() < e2.getX()) {
                currentPos = getVisibleViews(-1); // -1 : left
            } else {
                currentPos = getVisibleViews(1); // 1 : right
            }

            hScrollView.smoothScrollTo((int) (layoutsList.get(currentPos).getLeft() - 0.05*dispWidth), 0);
            return true;
        }
    }
}
