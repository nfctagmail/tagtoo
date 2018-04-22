package com.tagtoo.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StrictMode;
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
import android.widget.TextView;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class WriteAudioActivity extends AppCompatActivity {

    // contexte de l'application
    private static Context context;


    // Identifiant de l'application dans les logs
    private static final String LOG_TAG = "WRITE_AUDIO_ACTIVITY";
    // Le nombre 200 correspond au code de requête d'un enregistrement audio auprès du système
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    // Chemin du fichier où sera enregistré le fichier audio
    private static String mFileName = null;

    // La variable qui contiendra le numéro de série du tag
    private String tagSerialNbr = null;
    private String tagMessage = null;

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
    private TextView mSCounter;
    private Button mSendButton;

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
                findViewById(R.id.playButton).setBackgroundTintList(WriteAudioActivity.getAppContext().getResources().getColorStateList(R.color.colorPrimary100));

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

    @SuppressLint("NewApi")
    private void stopRecording() {

        // Fin de l'enregsitrement audio : enregistrement stoppé et paramètres réinitialisés
        try{
            mRecorder.stop();
        }
        catch(RuntimeException stopException){
            Log.e(LOG_TAG, "Recorder couldn't be stopped, maybe you didn't long press the button ?");
        }

        mRecorder.release();
        mRecorder = null;

        // Ajout d'un temps où l'on ne peut pas appuyer sur le bouton d'enregistrement pour éviter de créer un enregistrement null
        mResetTimer = new CountDownTimer(500,10) {
            @Override public void onTick(long msUntilFinished){} // Fonction nécessaire pour la classe CountDownTimer

            @Override
            public void onFinish() {
                findViewById(R.id.recordButton).setClickable(true);
                findViewById(R.id.recordButton).setBackgroundTintList(WriteAudioActivity.getAppContext().getResources().getColorStateList(R.color.colorAccent));
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

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

        }

        // On crée l'adresse à laquelle le fichier audio en cache sera enregistré, en récupérant l'adresse du cache attribué à l'application
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/recording_cache.3gp";

        // On demande la permission d'enregistrer un fichier audio
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // On récupère les objets d'affichage auxquels on veut attribuer une action (final = variable inchangeable)
        final ImageButton mRecordButton = findViewById(R.id.recordButton);
        final ImageButton mPlayButton = findViewById(R.id.playButton);
        mProgressBar = findViewById(R.id.progressBar);
        mSCounter = findViewById(R.id.scounter);
        mSendButton = findViewById(R.id.sendButton);
        mSendButton.setVisibility(View.GONE);

        final Animation recordInflateButton = AnimationUtils.loadAnimation(this, R.anim.anim_inflate_button);
        recordInflateButton.setRepeatCount(Animation.INFINITE);

        mProgressBar.setProgress(0);

        mCountDownTimer = new CountDownTimer(30000,100) {

            @Override
            public void onTick(long msUntilFinished) {
                int i = (30000 - safeLongToInt(msUntilFinished))/300;
                mProgressBar.setProgress(i);
                int s = (30000 - safeLongToInt(msUntilFinished))/1000;
                if(s < 10)
                    mSCounter.setText("0" + s + " / 30 s");
                else
                    mSCounter.setText(s + " / 30 s");

            }

            @Override
            public void onFinish() {
                mProgressBar.setProgress(100);
                mSCounter.setText("30 / 30 s");
                onRecord(mStartRecording);
                mStartRecording = !mStartRecording;
                mRecordButton.clearAnimation();
                isCountDownOver = true;
                mSendButton.setVisibility(View.VISIBLE);
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
                mSendButton.setVisibility(View.GONE);
                mCountDownTimer.start();
            }
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                if(!isCountDownOver) {
                    onRecord(mStartRecording);
                    mStartRecording = !mStartRecording;
                    mRecordButton.clearAnimation();
                    mCountDownTimer.cancel();
                    mSendButton.setVisibility(View.VISIBLE);
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

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beamMessage();
            }
        });

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (action.equals("READ_TAG")) {
                    tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                    Log.i(LOG_TAG, tagSerialNbr);
                    tagMessage = intent.getStringExtra("TAG_MESSAGE");
                    Log.i(LOG_TAG, tagMessage);

                    uploadAudio(tagSerialNbr);

                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));


    }

    public void beamMessage() {
        // Commencer la transaction (càd la création/suppression/remplacement) de fragments d'activités
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // S'il y a déjà  un fragment au tag égal à "beam"
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        //On le supprime
        if (prev != null) {
            ft.remove(prev);
        }
        // On ajoute la transaction au "back stack" qui tient la liste des transactions pour qu'elles puissent ensuite être annulées, en appuyant par exemple sur le bouton retour
        ft.addToBackStack(null);

        // On crée une instance la boîte de dialogue que l'on veut afficher, à laquelle on envoie le texte écrit
        DialogFragment beamDialog = BeamDialog.newInstance("audio");
        // On affiche la boîte de dialogue, à laquelle on donne le tag "beam"
        beamDialog.show(ft, "beam");
    }

    private static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + "  trop grand pour être un \"int\"");
        }
        return (int) l;
    }

    private void uploadAudio(String serialNbr){

        File directoryCache = new File(getExternalCacheDir().getAbsolutePath());
        File audioCache     = new File(directoryCache, "recording_cache.3gp");
        File audioToUpload  = new File(directoryCache, serialNbr + ".3gp");

        audioCache.renameTo(audioToUpload);

        Log.i(LOG_TAG, "Audio Cache : "     + audioCache.toString());
        Log.i(LOG_TAG, "Audio renamed : "   + audioToUpload.toString());

        FTPClient ftp = null;
        try{
            ftp = new FTPClient();
            ftp.connect(SendMessageActivity.verser);
            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords.toString()))
            {
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                FileInputStream fileInput = new FileInputStream(audioToUpload);
                boolean result = ftp.storeFile("/" + serialNbr + ".3gp", fileInput);
                fileInput.close();
                if(result)
                    Log.i(LOG_TAG, "Success uploading to server");
                ftp.logout();
                ftp.disconnect();
            }
        } catch (SocketException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
        }

    }


    public static Context getAppContext()
    {
        return WriteAudioActivity.context;
    }


}
