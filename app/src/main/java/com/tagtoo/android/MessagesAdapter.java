package com.tagtoo.android;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class MessagesAdapter  extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static Context mContext;

    private ArrayList<SavedMessage> messages;

    private String LOG_TAG = "MESSAGES";

    public MessagesAdapter(Context context, ArrayList<SavedMessage> list){
        this.mContext = context;
        messages = list;
    }

    public void setMessages(ArrayList<SavedMessage> list){
        this.messages = list;
    }

    @Override
    public MessagesAdapter.MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_savedmessage, viewGroup, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessagesAdapter.MessageViewHolder holder, final int position) {
        final SavedMessage item = messages.get(position);

        holder.display(messages.get(position));
        final SavedMessage newMessage = new SavedMessage(item.content, item.serialNbr, item.dateSaved);

        if(item.fileName != null) {
            holder.audioPlayer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(R.string.delete_audio_title);
                    builder.setMessage(R.string.delete_audio_desc);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(deleteFile(item.fileName, item.serialNbr)){
                                messages.set(position, newMessage);
                                if(mContext instanceof MainActivity) {                                  // Si on est dans le contexte de l'activité principale
                                    ((MainActivity) mContext).saveMessages(messages);                   // On appelle ses fonctions pour sauvegarder les messages ...
                                    ((MainActivity) mContext).setFragment(new HomeTabFragment());       // ... et actualiser le fragment
                                }
                            }

                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog dialogDelete = builder.create();
                    dialogDelete.show();

                    return true;
                }
            });
        }
    }

    private boolean deleteFile(String fileToDelete, String serialNumber){

        FTPClient ftp = null;

        File directoryCache = new File(mContext.getExternalCacheDir().getAbsolutePath());
        File audioToDelete  = new File(directoryCache, fileToDelete);

        Log.i(LOG_TAG, audioToDelete.getAbsolutePath());
        Log.i(LOG_TAG, "/" + serialNumber + ".3gp");

        try{
            ftp = new FTPClient();
            ftp.connect(SendMessageActivity.verser);

            Log.i(LOG_TAG, "Trying to connect to the server.");

            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                Log.i(LOG_TAG, "Connection to the server successful.");

                ftp.enterLocalPassiveMode();

                if(ftp.deleteFile("/" + serialNumber + ".3gp")) {
                    Log.i(LOG_TAG, "Deleting file from server successfully.");
                    if(audioToDelete.delete()) {
                        Log.i(LOG_TAG, "Deleting file from phone successfully.");
                        ftp.logout();
                        ftp.disconnect();
                        return true;
                    } else
                        Log.e(LOG_TAG, "Could not delete file from phone. You can always clean the cache manually.");
                }
                else
                    Log.e(LOG_TAG, "Could not delete file from server.");

                ftp.logout();
                ftp.disconnect();
            }
            else
                Log.e(LOG_TAG, "Could not connect to server");
        } catch (SocketException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getStackTrace().toString());
        }

        return false;
    }



    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView messagetw;
        private final TextView titleAudio;
        private final LinearLayout audioPlayer;
        private final ImageButton deleteButton;
        private final ImageButton playButton;
        private final ProgressBar progressBar;
        private final TextView counter;

        private MediaPlayer mPlayer = null;
        private String mFileName = null;
        private int duration = 0;
        private boolean isPlaying = false;
        private int MESSAGE_UPDATE_PROGRESS = 42;
        private Handler audioProgressHandler = null;
        private Thread updateAudioProgress = null;

        @SuppressLint("HandlerLeak")
        public MessageViewHolder(final View itemView){
            super(itemView);
            title        = itemView.findViewById(R.id.msgTitle);
            messagetw    = itemView.findViewById(R.id.message);
            titleAudio   = itemView.findViewById(R.id.audioMsgTitle);
            audioPlayer  = itemView.findViewById(R.id.audioPlayer);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            playButton   = itemView.findViewById(R.id.playButton);
            progressBar  = itemView.findViewById(R.id.recordingProgressBar);
            counter      = itemView.findViewById(R.id.progressCounter);

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    remove(getAdapterPosition());
                }
            });

            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPlaying();
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
                                counter.setText(currentPlayPos/1000 +"s");
                            }
                        }
                    }
                };
            }
        }


        public void display(SavedMessage savedMessage){

            if(savedMessage.serialNbr != null){
                String formatSerialNbr = savedMessage.serialNbr;
                formatSerialNbr = formatSerialNbr.substring(8).toUpperCase();
                Resources res = mContext.getResources();
                String textTitle = String.format(res.getString(R.string.title_msg_params), formatSerialNbr, savedMessage.dateSaved);
                title.setText(textTitle);
            }
            messagetw.setText(savedMessage.content);
            if(savedMessage.fileName == null) {
                titleAudio.setVisibility(View.GONE);
                playButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                counter.setVisibility(View.GONE);
            }
            else {
                mFileName = mContext.getExternalCacheDir().getAbsolutePath() + "/" +  savedMessage.fileName;
                Uri uri = Uri.parse(mFileName);
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(mContext, uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                duration = Integer.parseInt(durationStr) / 1000;
                counter.setText(duration + "s");
            }
        }

        public void remove(int position){
            messages.remove(position);                                              // On enlève de la liste le message correspondantà la position renseignée
            notifyItemRemoved(position);                                            // On actualise la liste
            notifyItemRangeChanged(position, messages.size());                      //
            if(mContext instanceof MainActivity) {                                  // Si on est dans le contexte de l'activité principale
                ((MainActivity) mContext).saveMessages(messages);                   // On appelle ses fonctions pour sauvegarder les messages ...
                ((MainActivity) mContext).setFragment(new HomeTabFragment());       // ... et actualiser le fragment
            }
        }

        @SuppressLint("NewApi")
        private void startPlaying() {
            // Démarrage de la lecture du fichier audio spécifié
            mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(mFileName);
                mPlayer.prepare();
                mPlayer.start();

                isPlaying = true;

                if(updateAudioProgress == null){
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
                    updateAudioProgress.start();
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Preparing audio player failed : maybe file doesn't exist.");
            }

            // Désactiver le bouton Play
            playButton.setEnabled(false);
            playButton.setBackgroundTintList(mContext.getResources().getColorStateList(R.color.colorDisabled));

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if(mPlayer != null)
                    {
                        mPlayer.release();
                        mPlayer = null;
                    }

                    progressBar.setProgress(1000);
                    counter.setText(duration + "s");

                    updateAudioProgress = null;
                    isPlaying = false;

                    playButton.setEnabled(true);
                    playButton.setBackgroundTintList(mContext.getResources().getColorStateList(R.color.colorPrimary100));


                }
            });
        }

    }

}
