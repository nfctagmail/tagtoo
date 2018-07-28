package com.tagtoo.android;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ConfigureTagActivity extends AppCompatActivity {

    /**
     * Initialisation des variables globales
     */

    Context context;

    private static final String LOG_TAG = "CONFIGURE_TAG_ACTIVITY";

    private static List<Pair<Integer, Integer>> listThumbs = Arrays.asList(
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_kitchen),
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_living_room),
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_hallway),
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_bedroom),
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_bathroom),
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_garage),
            Pair.create(R.drawable.thumbnail_fridge, R.string.thumbnail_title_other));

    EditText nameField;
    RecyclerView recyclerView = null;
    TextView selectedThumbText;
    Switch switchPassword;
    FrameLayout passwordLayout;
    EditText passwordField;
    ImageButton showPasswordBtn;
    Button nextButton;
    LinearLayout.LayoutParams layoutParams;

    NfcAdapter mNfcAdapter;
    PendingIntent mPendingIntent;
    IntentFilter writeTagFilters[];

    int dispWidth, itemWidth;
    boolean passwordShown = false;

    /**
     *  Lors de la création de l'activité
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_tag);

        context = this;

        // Autorisation d'accès à internet
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Taille des CardView du RecyclerView
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dispWidth = size.x;
        itemWidth = (int) (dispWidth - 0.25*dispWidth);
        layoutParams = new LinearLayout.LayoutParams(itemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

        // Mise en place du RecyclerView
        recyclerView = findViewById(R.id.thumbnailList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setItemViewCacheSize(listThumbs.size());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(new ThumbAdapter(listThumbs));

        // Récupération des éléments du layout
        nameField         = findViewById(R.id.name);
        selectedThumbText = findViewById(R.id.selectedThumbText);
        switchPassword    = findViewById(R.id.switchPassword);
        passwordLayout    = findViewById(R.id.passwordLayout);
        passwordField     = findViewById(R.id.password);
        showPasswordBtn   = findViewById(R.id.showPassword);
        nextButton        = findViewById(R.id.nextButton);

        // Texte sous recyclerView
        String text = getString(R.string.selected_thumbnail_text, getString(R.string.none));
        selectedThumbText.setText(text);

        // Mot de passe désactivé
        passwordField.setEnabled(false);
        showPasswordBtn.setEnabled(false);

        // Activation du mot de passe avec le Switch
        switchPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    passwordField.setEnabled(true);
                    showPasswordBtn.setEnabled(true);
                    showPasswordBtn.setImageTintList(getResources().getColorStateList(R.color.colorPrimary200));
                }
                else {
                    passwordField.setEnabled(false);
                    showPasswordBtn.setEnabled(false);
                    showPasswordBtn.setImageTintList(getResources().getColorStateList(R.color.colorDisabled));
                }
            }

        });

        // Activation de l'affichage du mot de passe
        showPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(passwordShown) {
                    passwordShown = false;
                    passwordField.setTransformationMethod(new PasswordTransformationMethod());
                    if(passwordField.getText().length() > 0)
                        passwordField.setSelection(passwordField.getText().length());
                    showPasswordBtn.setImageTintList(getResources().getColorStateList(R.color.colorPrimary200));
                }
                else {
                    passwordShown = true;
                    passwordField.setTransformationMethod(null);
                    if(passwordField.getText().length() > 0)
                        passwordField.setSelection(passwordField.getText().length());
                    showPasswordBtn.setImageTintList(getResources().getColorStateList(R.color.colorPrimaryA700));
                }
            }
        });

        // Action du bouton Suivant
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beamMessage();
            }
        });

        // Mise en place de la communication NFC
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };

    }

    /**
     *  Lors de la réception d'un intent
     */

    Tag myTag;
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        if(getFragmentManager().findFragmentByTag("beam") != null) {
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                try {
                    if (myTag != null) {
                        Toast.makeText(context, R.string.toast_conf_begin, Toast.LENGTH_LONG).show();
                        write(nameField.getText().toString(), myTag);
                    }
                    else {
                        Toast.makeText(context, R.string.error_configure_tag, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, R.string.error_configure_io, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, R.string.error_configure_format, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  Quand l'activité passe en arrière-plan on désactive l'envoi du message
     */
    @Override
    public void onPause(){
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    /**
     *  Quand l'activité repasse en premier-plan on active l'envoi du message
     */
    @Override
    public void onResume(){
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, writeTagFilters, null);
    }

    /**
     * Affichage de la boîte de dialogue
     */

    public void beamMessage() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        android.app.Fragment prev = getFragmentManager().findFragmentByTag("beam");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment beamDialog = BeamDialog.newInstance("\"" + nameField.getText().toString() + "\"");
        beamDialog.show(ft, "beam");
    }

    /**
     * Lecture du tag
     */

    private void write(String text, Tag tag) throws IOException, FormatException {
        Log.i(LOG_TAG, "WRITING TO TAG" + text);

        String dateCreated = System.currentTimeMillis() + "";

        NdefRecord name = NdefRecord.createMime("application/com.tagtoo.android", text.getBytes());
        NdefRecord date = NdefRecord.createMime("application/com.tagtoo.android", dateCreated.getBytes());
        NdefMessage message = new NdefMessage(new NdefRecord[]{name, date});

        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        String tagSerialNbr = bytesToHexString(tag.getId());
        ndef.writeNdefMessage(message);
        ndef.close();

        if(makeAndSendToServer(tagSerialNbr, dateCreated)){
            Toast.makeText(this, R.string.toast_success_conf, Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            DialogFragment prev = (DialogFragment) getFragmentManager().findFragmentByTag("beam");
            if (prev != null)
                prev.dismiss();
        }

    }

    /**
     * Conversion des octets du numéro de série
     */

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] tampon = new char[2];

        for (byte aSrc : src) {
            // Traitement du premier caractère puis du deuxième caractère, pour le même octet, les 4 premiers bits correspondront à un caractère en héxadécimal, puis les 4 autres un autre
            // Character.forDigit() nous donne l'équivalent du chiffre binaire en base 16, en héxadécimal
            // 1. dans l'octet src[i], >>> 4 prend seulement les 4 bits les plus élevés (= à gauche), par exemple : à partir de 1010 1111 on obtient 0000 1010
            // on les compare (& = opérat° binaire AND : 1 ou 0 AND 0 -> 0; 1 AND 1 -> 1) à 0F (16) = 15 (10) = 0000 1111 (2) (on ne garde donc que les bits de droite)
            tampon[0] = Character.forDigit((aSrc >>> 4) & 0x0F, 16);
            tampon[1] = Character.forDigit(aSrc & 0x0F, 16);
            stringBuilder.append(tampon);
        }

        return stringBuilder.toString();
    }

    /**
     * Création du fichier du nouveau tag et envoi sur le serveur
     */

    private boolean makeAndSendToServer(String serialNbr, String dateCreated){
        ThumbAdapter thumbAdapter = (ThumbAdapter) recyclerView.getAdapter();
        Log.i("CONF_TAG", thumbAdapter.getSelectedThumbnail() + " " + serialNbr);
        SavedMessage newTag = new SavedMessage(serialNbr, nameField.getText().toString(), dateCreated, thumbAdapter.getSelectedThumbnail());
        Gson gson = new Gson();
        String json = gson.toJson(newTag);
        try {
            FileWriter file = new FileWriter(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/newtag.json");
            file.write(json);
            file.flush();
            file.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        File newTagJson = new File(getExternalCacheDir().getAbsolutePath(), "newtag.json");

        FTPClient ftp;
        try {
            ftp = new FTPClient();
            ftp.connect(SendMessageActivity.verser);

            Log.i(LOG_TAG, "Trying to connect to the server");

            if(ftp.login(SendMessageActivity.seamen, SendMessageActivity.swords))
            {
                Log.i(LOG_TAG, "Connection to the server successful");

                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                FileInputStream fileInput = new FileInputStream(newTagJson);
                boolean result = ftp.storeFile("/Tag_" + serialNbr + "_" + dateCreated + ".json", fileInput);
                fileInput.close();
                ftp.logout();
                ftp.disconnect();
                if(result) {
                    Log.i(LOG_TAG, "Success uploading to server");
                    return true;
                }
                else {
                    Log.i(LOG_TAG, "Error while uploading to server");
                    Toast.makeText(this, R.string.toast_error_conf_upload, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            else {
                Log.e(LOG_TAG, "Could not connect to server");
                Toast.makeText(this, R.string.toast_error_conf_connect, Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, R.string.toast_error_conf_connect, Toast.LENGTH_LONG).show();
        return false;

    }


    /**
     * Class de l'Adapter de la liste des vignettes
     */

    public class ThumbAdapter  extends RecyclerView.Adapter<ThumbAdapter.ThumbViewHolder> {

        private List<Pair<Integer, Integer>> thumbs;
        private final List<ThumbViewHolder> holderList;
        private int selectedThumbnail;

        ThumbAdapter(List<Pair<Integer, Integer>> list){
            thumbs = list;
            holderList = new ArrayList<>();
        }

        @Override
        public ThumbViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            return new ThumbViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_thumbnail, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(final ThumbViewHolder holder, final int position) {

            holder.display(thumbs.get(position));
            holderList.add(holder);

            holder.cardView.setOnCheckedChangeListener(new CheckableCardView.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CheckableCardView cV, boolean isChecked) {
                    for(ThumbViewHolder h : holderList){
                        h.cardView.setChecked(false);
                    }
                    if(isChecked) {
                        holder.cardView.setChecked(true);
                        selectedThumbnail = position + 1;
                        selectedThumbText.setTag(selectedThumbnail);
                        int thumbName = R.string.none;
                        switch(selectedThumbnail){
                            case 1 :
                                thumbName = R.string.thumbnail_title_kitchen;
                                break;
                            case 2 :
                                thumbName = R.string.thumbnail_title_living_room;
                                break;
                            case 3 :
                                thumbName = R.string.thumbnail_title_hallway;
                                break;
                            case 4 :
                                thumbName = R.string.thumbnail_title_bedroom;
                                break;
                            case 5 :
                                thumbName = R.string.thumbnail_title_bathroom;
                                break;
                            case 6 :
                                thumbName = R.string.thumbnail_title_garage;
                                break;
                            case 7 :
                                thumbName = R.string.thumbnail_title_other;
                                break;
                        }
                        String text = getString(R.string.selected_thumbnail_text, getString(thumbName));
                        selectedThumbText.setText(text);
                    }
                    else {
                        selectedThumbText.setTag(null);
                        String text = getString(R.string.selected_thumbnail_text, getString(R.string.none));
                        selectedThumbText.setText(text);
                    }

                    Log.i("CONF_TAG", holder.cardView.isChecked() + "");

                }
            });
        }

        @Override
        public int getItemCount() {
            return thumbs.size();
        }

        public int getSelectedThumbnail(){
            return selectedThumbnail;
        }

        public class ThumbViewHolder extends RecyclerView.ViewHolder {

            LinearLayout linearLayout;
            CheckableCardView cardView;
            FrameLayout frameLayout;
            ImageView thumbImage;
            TextView thumbTitle;

            public ThumbViewHolder(View itemView) {
                super(itemView);
                linearLayout = itemView.findViewById(R.id.linearLayoutCard);
                cardView     = itemView.findViewById(R.id.cardView);
                frameLayout  = itemView.findViewById(R.id.frameLayout);
                thumbImage   = itemView.findViewById(R.id.thumbImage);
                thumbTitle   = itemView.findViewById(R.id.thumbTitle);
            }

            public void display(Pair<Integer, Integer> pair){
                linearLayout.setLayoutParams(layoutParams);
                thumbImage.setBackgroundResource(pair.first);
                thumbTitle.setText(pair.second);
                //if(cardView.isChecked())
                  //  frameLayout.setForeground(new ColorDrawable(getResources().getColor(R.color.cardChecked)));
            }

        }

    }


}
