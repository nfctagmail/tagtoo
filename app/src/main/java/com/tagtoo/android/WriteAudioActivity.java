package com.tagtoo.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.io.IOException;

public class WriteAudioActivity extends AppCompatActivity {

    // contexte de l'application
    private static Context context;


    // Identifiant de l'application dans les logs
    private static final String LOG_TAG = "WRITE_AUDIO_ACTIVITY";
    // Le nombre 200 correspond au code de requête d'un enregistrement audio auprès du système
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    // Chemin du fichier où sera enregistré le fichier audio
    private static String mFileName = null;

    // Classes permettant d'enregistrer et de jouer l'audio
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    // Variable booléenne sachant si un enregistrement doit être commencé (=true) ou arrêté (=false)
    private boolean mStartRecording = true;
    // Variable booléenne permettant d'arrêter l'enregistrement à la fin du compte à rebours, avant que l'utilisateur lève son doigt.
    private boolean isCountDownOver = false;

    // Comptes à rebours
    private CountDownTimer mCountDownTimer;
    private CountDownTimer mResetTimer;

    // Eléments de l'interface
    private ProgressBar mProgressBar;
    private Button sendButton;

    // Variable booléenne pour savoir si l'utilisateur a accepté que l'app enregistre le son, et ainsi continuer
    // NB : RECORD_AUDIO est considérée comme une permission "dangereuse"
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Trouver les résultats des demandes de permission ...
        switch(requestCode){
            // Ici seulement pour RECORD_AUDIO (mais d'autres peuvent être ajoutées)
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        // Si la permission n'est pas acceptée : on quitte l'activité
        if(!permissionToRecordAccepted)
            finish();
    }

    @SuppressLint("NewApi")
    private void startPlaying() {
        // Démarrage de la lecture du fichier audio spécifié
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        // Désactiver le bouton Play et changer sa couleur
        findViewById(R.id.playButton).setEnabled(false);
        findViewById(R.id.playButton).setBackgroundTintList(this.getResources().getColorStateList(R.color.colorDisabled));

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(mPlayer != null)
                {
                    mPlayer.release();
                    mPlayer = null;
                }
                findViewById(R.id.playButton).setEnabled(true);
                findViewById(R.id.playButton).setBackgroundTintList(WriteAudioActivity.getAppContext().getResources().getColorStateList(R.color.colorAccent));

            }
        });
    }

    // Choix entre démarrage ou arrêt de l'enregistrement
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        // Paramétrage de l'enregsitrement audio : source = micro de l'appareil, format en sortie : .3gp, encodeur audio : AMR-NB
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mFileName);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        // TODO bugfix : check if mFileName still exists and wasn't deleted before trying to record to it
        mRecorder.start();

        isCountDownOver = false;
    }

    private void stopRecording() {

        // Fin de l'enregsitrement audio : enregistrement stoppé et paramètres réinitialisés
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        // Ajout d'un temps où l'on ne peut pas appuyer sur le bouton d'enregistrement pour éviter de créer un enregistrement null
        mResetTimer = new CountDownTimer(500,10) {
            @Override public void onTick(long msUntilFinished){} // Fonction nécessaire pour la classe CountDownTimer

            @Override
            public void onFinish() {
                findViewById(R.id.recordButton).setClickable(true);
                findViewById(R.id.recordButton).setBackgroundTintList(WriteAudioActivity.getAppContext().getResources().getColorStateList(R.color.colorPrimary100));
            }
        };
        findViewById(R.id.recordButton).setClickable(false);
        findViewById(R.id.recordButton).setBackgroundTintList(WriteAudioActivity.getAppContext().getResources().getColorStateList(R.color.colorDisabled));
        mResetTimer.start();

        startPlaying();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_audio);

        WriteAudioActivity.context = getApplicationContext();

        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/recording_cache.3gp";

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        final ImageButton mRecordButton = findViewById(R.id.recordButton);
        final ImageButton mPlayButton = findViewById(R.id.playButton);
        mProgressBar = findViewById(R.id.progressBar);
        //sendButton = findViewById(R.id.sendButton);
        final Animation recordInflateButton = AnimationUtils.loadAnimation(this, R.anim.anim_inflate_button);
        recordInflateButton.setRepeatCount(Animation.INFINITE);

        mProgressBar.setProgress(0);

        mCountDownTimer = new CountDownTimer(15000,100) {

            @Override
            public void onTick(long msUntilFinished) {
                //int i = (15000 - Math.toIntExact(msUntilFinished))/150;
                int i = (15000 - safeLongToInt(msUntilFinished))/150;
                mProgressBar.setProgress(i);
            }

            @Override
            public void onFinish() {
                mProgressBar.setProgress(100);
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
                mRecordButton.clearAnimation();
                isCountDownOver = true;
                sendButton.setVisibility(View.VISIBLE);
                Log.i(LOG_TAG, "Countdown done");
            }
        };

        mRecordButton.setOnTouchListener(new View.OnTouchListener() {
             @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && mStartRecording) {
                view.performClick();
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
                mRecordButton.startAnimation(recordInflateButton);
                //sendButton.setVisibility(View.GONE);
                mCountDownTimer.start();
            }
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                if(!isCountDownOver) {
                    onRecord(mStartRecording);
                    mStartRecording = !mStartRecording;
                    mRecordButton.clearAnimation();
                    mCountDownTimer.cancel();
                    //sendButton.setVisibility(View.VISIBLE);
                }
            }
            return true;
        }
    }
        );

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPlaying();
            }
        });
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + "  trop grand pour être un \"int\"");
        }
        return (int) l;
    }


    public static Context getAppContext()
    {
        return WriteAudioActivity.context;
    }


}
