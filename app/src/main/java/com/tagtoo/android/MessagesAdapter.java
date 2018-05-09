package com.tagtoo.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class MessagesAdapter  extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private Context mContext;

    private ArrayList<MainActivity.SavedMessage> messages;

    public MessagesAdapter(Context context, ArrayList<MainActivity.SavedMessage> list){
        this.mContext = context;
        messages = list;
    }

    public void setMessages(ArrayList<MainActivity.SavedMessage> list){
        this.messages = list;
    }

    @Override
    public MessagesAdapter.MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_savedmessage, viewGroup, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessagesAdapter.MessageViewHolder holder, int position) {
        holder.display(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView messagetw;
        private final TextView info;
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
            info         = itemView.findViewById(R.id.info);
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

        public void display(MainActivity.SavedMessage savedMessage){

            if(savedMessage.serialNbr != null){
                String formatSerialNbr = savedMessage.serialNbr;
                formatSerialNbr = formatSerialNbr.substring(8).toUpperCase();
                Resources res = mContext.getResources();
                String textTitle = String.format(res.getString(R.string.title_msg_params), formatSerialNbr, savedMessage.dateSaved);
                title.setText(textTitle);
            }
            messagetw.setText(savedMessage.content);
            info.setText(savedMessage.dateSaved);
            if(savedMessage.fileName == null) {
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
                                Log.e("MESSAGES", e.getMessage(), e);
                            }

                        }
                    };
                    updateAudioProgress.start();
                }

            } catch (IOException e) {
                Log.e("MESSAGES", "Preparing audio player failed : maybe file doesn't exist.");
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
