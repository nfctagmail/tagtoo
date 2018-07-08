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
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;


public class WriteAudioActivity extends AppCompatActivity {

    private static Context context;

    private static final String LOG_TAG = "WRITE_AUDIO_ACTIVITY";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private static String mFileName = null;

    private String tagSerialNbr = null;
    private String tagName = null;
    private String tagDate = null;

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    private boolean mStartRecording = true;
    private boolean isCountDownOver = false;

    // Comptes à rebours
    private CountDownTimer mCountDownTimer;
    private CountDownTimer mResetTimer;

    // Eléments de l'interface
    ProgressBar mProgressBar;
    TextView mSCounter;
    ImageButton mRecordButton;
    ImageButton mPlayButton;
    FloatingActionButton mSendButton;

    // Variable booléenne pour savoir si l'utilisateur a accepté que l'app enregistre le son, et ainsi continuer
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    /**
     *  Lors de la création de l'activité
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_audio);

        WriteAudioActivity.context = getApplicationContext();

        // Autorisation d'accès à internet
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // On crée l'adresse à laquelle le fichier audio en cache sera enregistré, en récupérant l'adresse du cache attribué à l'application
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/recording_cache.3gp";

        // On demande la permission d'enregistrer un fichier audio
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // On récupère les objets d'affichage auxquels on veut attribuer une action (final = variable inchangeable)
        mRecordButton = findViewById(R.id.recordButton);
        mPlayButton = findViewById(R.id.playButton);
        mProgressBar = findViewById(R.id.progressBar);
        mSCounter = findViewById(R.id.scounter);
        mSendButton = findViewById(R.id.sendButton);

        mSendButton.setVisibility(View.GONE);

        // on charge l'animation qui fait gonfler le bouton d'enregistrement, qui se répète à l'infini
        final Animation recordInflateButton = AnimationUtils.loadAnimation(this, R.anim.anim_inflate_button);
        recordInflateButton.setRepeatCount(Animation.INFINITE);

        // la barre de progression est remise à zéro
        mProgressBar.setProgress(0);

        // on démarre le compte à rebours qui empêche de faire un enregistrement plus long que 30s et qui fait avancer la barre de progression
        mCountDownTimer = new CountDownTimer(30000,100) {

            @Override
            public void onTick(long msUntilFinished) {
                int i = (30000 - (int)msUntilFinished)/300;
                mProgressBar.setProgress(i);
                int s = (30000 - (int)msUntilFinished)/1000;
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
                // Tant que l'on a le doigt pressé dessus on démarre l'enregistrement
                if (event.getAction() == MotionEvent.ACTION_DOWN && mStartRecording) {
                    view.performClick();
                    onRecord(mStartRecording);
                    mStartRecording = !mStartRecording;
                    mRecordButton.startAnimation(recordInflateButton);
                    mSendButton.setVisibility(View.GONE);
                    mCountDownTimer.start();
                }
                // Si le doigt est relevé on arrête l'enregistrement, à part s'il a déjà été arrêté par le compte à rebours
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
        });

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

                if(getFragmentManager().findFragmentByTag("beam") != null)
                    if(action.equals("READ_TAG")) {

                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName      = intent.getStringExtra("TAG_NAME");
                        tagDate      = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        if(uploadAudio(tagSerialNbr, tagDate)) {
                            Toast.makeText(context, R.string.success_write_audio, Toast.LENGTH_LONG).show();
                            unregisterReceiver(this);
                            finish();
                        } else
                            Toast.makeText(context, R.string.error_write_audio_server, Toast.LENGTH_LONG).show();
                    }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if(!permissionToRecordAccepted)
            finish();
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        // Désactiver le bouton Play et changer sa couleur
        mPlayButton.setEnabled(false);
        mPlayButton.setBackgroundTintList(this.getResources().getColorStateList(R.color.colorDisabled));

        // Quand la lecture est terminée
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(mPlayer != null)
                {
                    mPlayer.release();
                    mPlayer = null;
                }
                // On réactive le bouton play
                mPlayButton.setEnabled(true);
                mPlayButton.setBackgroundTintList(WriteAudioActivity.getAppContext().getResources().getColorStateList(R.color.colorPrimary100));
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
        // Paramétrage de l'enregsitrement audio : source = micro de l'appareil, format en sortie : .3gp, encodeur audio : AMR-NB, emplacement de sortie
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

        mRecorder.start();

        isCountDownOver = false;
    }

    private void stopRecording() {

        // Fin de l'enregsitrement audio : enregistrement stoppé et paramètres réinitialisés
        try{
            mRecorder.stop();
        }
        catch(RuntimeException stopException){
            Log.e(LOG_TAG, "Recorder couldn't be stopped, maybe you didn't long press the button ?");
        }

        // On réinitialise l'enregistreur
        mRecorder.release();
        mRecorder = null;

        // Ajout d'un temps où l'on ne peut pas appuyer sur le bouton d'enregistrement pour éviter de créer un enregistrement null
        mResetTimer = new CountDownTimer(500,10) {
            @Override public void onTick(long msUntilFinished){}
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

    public void beamMessage() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment beamDialog = BeamDialog.newInstance("Message audio");
        beamDialog.show(ft, "beam");
    }

    private boolean uploadAudio(String serialNbr, String date) {

        File directoryCache = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File audioCache     = new File(directoryCache, "recording_cache.3gp");
        File jsonCache      = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".json");

        Log.i(LOG_TAG, "Audio Cache : "     + audioCache.toString());

        FTPClient ftp = null;
        try {
            ftp = new FTPClient();
            ftp.connect(SendMessageActivity.verser);

            Log.i(LOG_TAG, "Trying to connect to the server");

            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                Log.i(LOG_TAG, "Connection to the server successful");

                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                FileOutputStream fileOutput = new FileOutputStream(jsonCache);
                boolean resultOut = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".json", fileOutput);
                fileOutput.close();
                SavedMessage tagData = null;
                SavedMessage newTagData = null;
                if(resultOut) {
                    Gson gson = new Gson();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(jsonCache));
                        Type type = new TypeToken<SavedMessage>() {}.getType();
                        tagData = gson.fromJson(br, type);
                        if(tagData != null)
                            newTagData = tagData;

                        FileInputStream fileInput = new FileInputStream(audioCache);
                        boolean resultIn = ftp.storeFile("/Tag_" + serialNbr + "_" + date + ".3gp", fileInput);
                        fileInput.close();

                        if(resultIn) {
                            Log.i(LOG_TAG, "Success uploading audio file to server");
                            if(newTagData != null) {
                                newTagData = new SavedMessage(newTagData.serialNbr, newTagData.name, newTagData.dateCreated, newTagData.thumbnailId, newTagData.dateSaved, newTagData.messageText, true, newTagData.pictureFile, newTagData.videoFile);
                                String json = gson.toJson(newTagData);
                                FileWriter file = new FileWriter(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/Tag_" + serialNbr + "_" + date + ".json");
                                file.write(json);
                                file.flush();
                                file.close();

                                FileInputStream fileInput2 = new FileInputStream(jsonCache);
                                boolean resultIn2 = ftp.storeFile("/Tag_" + serialNbr + "_" + date + ".json", fileInput2);
                                fileInput2.close();
                                ftp.logout();
                                ftp.disconnect();
                                if(resultIn2) {
                                    Log.i(LOG_TAG, "Success uploading tag data to server");
                                    return true;
                                }
                                else {
                                    Log.i(LOG_TAG, "Error uploading tag data");
                                    return false;
                                }
                            }
                        }
                        else {
                            Log.i(LOG_TAG, "Error uploading audio");
                            return false;
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.i(LOG_TAG, "Error retrieving tag data");
                    return false;
                }
            }
            else {
                Log.e(LOG_TAG, "Could not connect to server");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static Context getAppContext()
    {
        return WriteAudioActivity.context;
    }


}
