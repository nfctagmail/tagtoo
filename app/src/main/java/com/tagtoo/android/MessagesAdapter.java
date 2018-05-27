package com.tagtoo.android;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

// Class gérant tous les messages de la liste de l'onglet d'accueil
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
    public int getItemViewType(int position) {
        return position == messages.size() ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        return messages.size() + 1;
    }

    @Override
    public MessagesAdapter.MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        View viewItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_savedmessage, viewGroup, false);
        View viewRead = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_read, viewGroup, false);

        switch (viewType) {
            case 0:
                return new MessageViewHolder(viewItem);
            case 1:
                return new MessageViewHolder(viewRead);
        }

        return new MessageViewHolder(viewItem);

    }

    @Override
    public void onBindViewHolder(MessagesAdapter.MessageViewHolder holder, final int position) {

        if(position != messages.size()){
            holder.display(messages.get(position));
        }
        else if(position == messages.size()){
            holder.displayReadFragment();
        }


        // S'il y a un fichier audio
        /*if(item.fileName != null) {
            // On crée un nouvel objet de la liste, sans fichier audio associé
            final SavedMessage newMessage = new SavedMessage(item.content, item.serialNbr, item.dateSaved);
            // quand on reste appuyé sur le lecteur audio de l'objet de liste
            holder.audioPlayer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // On crée un nouveau constructeur de boite de dialogue
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    // On définit ses attributs
                    builder.setTitle(R.string.delete_audio_title);
                    builder.setMessage(R.string.delete_audio_desc);
                    // ... on ajoute un bouton avec une action positive
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // qui va supprimer le fichier correspondant
                            if(deleteFile(item.fileName, item.serialNbr)){
                                // on remplace ce message par un message sans fichier audio
                                messages.set(position, newMessage);
                                if(mContext instanceof MainActivity) {                                  // Si on est dans le contexte de l'activité principale
                                    ((MainActivity) mContext).saveMessages(messages);                   // On appelle ses fonctions pour sauvegarder les messages ...
                                    ((MainActivity) mContext).setFragment(new HomeTabFragment());       // ... et actualiser le fragment
                                }
                            }

                        }
                    });
                    // ... on ajoute un bouton avec une action négative
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // on ferme la boite de dialogue
                            dialogInterface.cancel();
                        }
                    });
                    // on crée l'objet de la boite de dialogue à partir du constructeur
                    AlertDialog dialogDelete = builder.create();
                    // on l'affiche
                    dialogDelete.show();
                    // On arrête de gérer le clic long
                    return true;
                }
            });
        }*/
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


    public class MessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


        private NfcAdapter mNfcAdapter;

        private final CardView cardView;
        private final ImageView thumbnail;
        private final TextView title;
        private final TextView info;
        private final ImageView iconText;
        private final ImageView iconAudio;
        private final ImageView iconPicture;
        private final ImageView iconVideo;

        private final Button refresh;
        private final TextView readTextView;
        private final ImageView readImageView;
        private final ImageView errorImageView;

        private final Context context;

        private MediaPlayer mPlayer = null;
        private String mFileName = null;
        private int duration = 0;
        private boolean isPlaying = false;
        private int MESSAGE_UPDATE_PROGRESS = 42;
        private Handler audioProgressHandler = null;
        private Thread updateAudioProgress = null;

        @SuppressLint("HandlerLeak")
        public MessageViewHolder(final View itemView) {
            super(itemView);

            mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

            // CardView
            cardView = itemView.findViewById(R.id.card_view);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.card_title);
            info  = itemView.findViewById(R.id.card_info);
            iconText = itemView.findViewById(R.id.messageTextIcon);
            iconAudio = itemView.findViewById(R.id.messageAudioIcon);
            iconPicture = itemView.findViewById(R.id.messagePictureIcon);
            iconVideo = itemView.findViewById(R.id.messageVideoIcon);

            // ReadFragment
            refresh        = itemView.findViewById(R.id.refreshButton);
            readTextView   = itemView.findViewById(R.id.readTextView);
            readImageView  = itemView.findViewById(R.id.section_image);
            errorImageView = itemView.findViewById(R.id.error_image);

            context = itemView.getContext();
        }

        @Override
        public void onClick(View v) {

            final Intent intent = new Intent(context, TagOverviewActivity.class);
            intent.putExtra("name", title.getText());

            String transitionName = context.getString(R.string.transition_key);

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((MainActivity) context, cardView, transitionName);

            context.startActivity(intent, options.toBundle());

        }

            /*// on fait appel à la fonction pour supprimer quand on appuye sur le bouton pour supprimer
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    remove(getAdapterPosition());
                }
            });

            // on fait appel à la fonction pour jouer l'audio quand on appuye sur le bouton play
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPlaying();
                }
            });

            // si on ne gère pas déjà la lecture d'un fichier audio
            if(audioProgressHandler == null) {
                audioProgressHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if(msg.what == MESSAGE_UPDATE_PROGRESS) {
                            if (mPlayer != null) {
                                // on met à jour le progrès de la barre de progresion du lecteur
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
*/
        // Fonction pour afficher la liste
        public void display(SavedMessage savedMessage){

            // on affiche le numéro de série du tag s'il existe
            if(savedMessage.serialNbr != null){
                String formatSerialNbr = savedMessage.serialNbr;
                formatSerialNbr = formatSerialNbr.substring(8).toUpperCase();
                Resources res = context.getResources();
                String textTitle = String.format(res.getString(R.string.title_msg_params), formatSerialNbr, savedMessage.dateSaved);
                info.setText(textTitle);
            }

            cardView.setOnClickListener(this);

            title.setText(savedMessage.name);

            if(savedMessage.messageText != null)
                iconText.setImageTintList(context.getResources().getColorStateList(R.color.colorPrimary));
            if(savedMessage.audioFile != null)
                iconAudio.setImageTintList(context.getResources().getColorStateList(R.color.colorPrimary));
            if(savedMessage.pictureFile != null)
                iconPicture.setImageTintList(context.getResources().getColorStateList(R.color.colorPrimary));
            if(savedMessage.videoFile != null)
                iconVideo.setImageTintList(context.getResources().getColorStateList(R.color.colorPrimary));

            // s'il n'y a pas de fichier audio on n'affiche pas le lecteur audio
            /*if(savedMessage.fileName == null) {
                titleAudio.setVisibility(View.GONE);
                playButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                counter.setVisibility(View.GONE);
            }
            else {
                // s'il y en a un on récupère le fichier audio dans le cache de l'application pour trouver sa longueur et l'afficher dans le compteur
                mFileName = mContext.getExternalCacheDir().getAbsolutePath() + "/" +  savedMessage.fileName;
                Uri uri = Uri.parse(mFileName);
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(mContext, uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                duration = Integer.parseInt(durationStr) / 1000;
                counter.setText("0s");
            }*/
        }

        public void displayReadFragment() {

            if(!mNfcAdapter.isEnabled())
            {
                Log.i(LOG_TAG, "NFC is disabled");

                readTextView.setText(R.string.nfc_disabled);
                readImageView.setImageTintList(mContext.getResources().getColorStateList(R.color.colorDisabled));
                errorImageView.setVisibility(View.VISIBLE);
                errorImageView.bringToFront();
                refresh.setVisibility(View.VISIBLE);
                refresh.setEnabled(true);
            }
            else if(messages.isEmpty())
            {
                readTextView.setText(R.string.list_empty);
            }

            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((MainActivity) mContext).finish();
                    mContext.startActivity(((MainActivity) mContext).getIntent());
                }
            });

        }



        /*
        // Fonction pour retirer un message de la liste
        public void remove(int position){
            messages.remove(position);                                              // On enlève de la liste le message correspondantà la position renseignée
            notifyItemRemoved(position);                                            // On actualise la liste
            notifyItemRangeChanged(position, messages.size());                      //
            if(mContext instanceof MainActivity) {                                  // Si on est dans le contexte de l'activité principale
                ((MainActivity) mContext).saveMessages(messages);                   // On appelle ses fonctions pour sauvegarder les messages ...
                ((MainActivity) mContext).setFragment(new HomeTabFragment());       // ... et actualiser le fragment
            }
        }

        // Fonction pour commencer la lecture d'un fichier audio
        @SuppressLint("NewApi")
        private void startPlaying() {
            // Démarrage de la lecture du fichier audio spécifié
            mPlayer = new MediaPlayer();
            try {
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
            playButton.setBackgroundTintList(mContext.getResources().getColorStateList(R.color.colorDisabled));

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
                    playButton.setBackgroundTintList(mContext.getResources().getColorStateList(R.color.colorPrimary100));


                }
            });
        }
        */
    }

}
