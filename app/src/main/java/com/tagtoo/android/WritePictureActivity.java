package com.tagtoo.android;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Objects;

public class WritePictureActivity extends AppCompatActivity {

    Context context;

    BroadcastReceiver broadcastReceiver;

    ImageButton cameraButton;
    ImageButton galleryButton;
    FloatingActionButton sendButton;

    private String tagSerialNbr = null;
    private String tagName = null;
    private String tagDate = null;

    private static final String LOG_TAG = "WRITE_PICTURE_ACTIVITY";
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_READ_PHOTO = 2;
    String mCurrentPhotoPath;
    private Handler mHandler = new Handler();

    private static final int REQUEST_ACCESS_STORAGE_PERMISSION = 200;
    private boolean permissionToAccessStorageAccepted = false;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_picture);

        context = this;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ActivityCompat.requestPermissions((Activity) context, permissions, REQUEST_ACCESS_STORAGE_PERMISSION);

        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);
        sendButton = findViewById(R.id.sendButton);

        final Animation inflateButton = AnimationUtils.loadAnimation(this, R.anim.anim_maxinflate_button);
        inflateButton.setInterpolator(new AccelerateInterpolator());

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraButton.setAnimation(inflateButton);
                cameraButton.startAnimation(inflateButton);
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        dispatchTakePictureIntent();
                    }
                }, 500);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionToAccessStorageAccepted) {
                    galleryButton.setAnimation(inflateButton);
                    galleryButton.startAnimation(inflateButton);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            performFileSearch();
                        }
                    }, 500);
                }
                else
                    ActivityCompat.requestPermissions((Activity) context, permissions, REQUEST_ACCESS_STORAGE_PERMISSION);

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beamMessage();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();

                if(getFragmentManager().findFragmentByTag("beam") != null) {
                    assert action != null;
                    if(action.equals("READ_TAG")) {

                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName      = intent.getStringExtra("TAG_NAME");
                        tagDate      = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                if(uploadPicture(tagSerialNbr, tagDate)) {
                                    Toast.makeText(context, R.string.success_write_picture, Toast.LENGTH_LONG).show();
                                    unregisterReceiver(broadcastReceiver);
                                    finish();
                                } else
                                    Toast.makeText(context, R.string.error_write_picture_server, Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();

                if(getFragmentManager().findFragmentByTag("beam") != null) {
                    assert action != null;
                    if(action.equals("READ_TAG")) {

                        tagSerialNbr = intent.getStringExtra("TAG_SERIAL");
                        tagName      = intent.getStringExtra("TAG_NAME");
                        tagDate      = intent.getStringExtra("TAG_DATE");

                        Log.i(LOG_TAG, "Serial : " + tagSerialNbr + "; Name : " + tagName + "; Date : " + tagDate);

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                if(uploadPicture(tagSerialNbr, tagDate)) {
                                    Toast.makeText(context, R.string.success_write_picture, Toast.LENGTH_LONG).show();
                                    unregisterReceiver(broadcastReceiver);
                                    finish();
                                } else
                                    Toast.makeText(context, R.string.error_write_picture_server, Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("READ_TAG"));
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_READ_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                assert uri != null;
                Log.i(LOG_TAG, "Uri: " + uri.toString());

                // Get real file path from id
                String wholeID = DocumentsContract.getDocumentId(uri);
                String id = wholeID.split(":")[1];
                String[] column = { MediaStore.Images.Media.DATA };
                String sel = MediaStore.Images.Media._ID + "=?";
                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column, sel, new String[]{ id }, null);
                String filePath = "";
                assert cursor != null;
                int columnIndex = cursor.getColumnIndex(column[0]);
                if (cursor.moveToFirst())
                    filePath = cursor.getString(columnIndex);
                cursor.close();

                File photoFile = new File(filePath);
                File cacheFile = createImageFile();
                try {
                    copy(photoFile, cacheFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case REQUEST_ACCESS_STORAGE_PERMISSION:
                permissionToAccessStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // If there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create picture file
            File photoFile;
            photoFile = createImageFile();

            // If File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.tagtoo.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_READ_PHOTO);
    }

    private File createImageFile() {
        String imageFileName = "picture_cache.jpg";
        File storageDir = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File image = new File(storageDir, imageFileName);

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
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

        DialogFragment beamDialog = BeamDialog.newInstance("");
        beamDialog.show(ft, "beam");
    }

    private boolean uploadPicture(String serialNbr, String date) {
        File directoryCache = new File(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath());
        File pictureCache   = new File(directoryCache, "picture_cache.jpg");
        File jsonCache      = new File(directoryCache, "Tag_" + serialNbr + "_" + date + ".json");

        Log.i(LOG_TAG, "Picture Cache : " + pictureCache.toString());

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

                Toast.makeText(context, "Uploading, please wait...", Toast.LENGTH_SHORT).show();

                FileOutputStream fileOutputData = new FileOutputStream(jsonCache);
                boolean resultOut = ftp.retrieveFile("/Tag_" + serialNbr + "_" + date + ".json", fileOutputData);
                fileOutputData.close();
                SavedMessage tagData;
                SavedMessage newTagData;
                if(resultOut) {
                    Gson gson = new Gson();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(jsonCache));
                        Type type = new TypeToken<SavedMessage>() {}.getType();
                        tagData = gson.fromJson(br, type);
                        if(tagData != null) {
                            newTagData = tagData;

                            FileInputStream fileInput = new FileInputStream(pictureCache);
                            boolean resultIn = ftp.storeFile("/Tag_" + serialNbr + "_" + date + ".jpg", fileInput);
                            fileInput.close();

                            if (resultIn) {
                                Log.i(LOG_TAG, "Success uploading picture to server");
                                newTagData = new SavedMessage(newTagData.serialNbr, newTagData.name, newTagData.dateCreated, newTagData.thumbnailId, newTagData.dateSaved, newTagData.messageText, newTagData.audioFile, true, newTagData.videoFile);
                                String json = gson.toJson(newTagData);
                                FileWriter file = new FileWriter(Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/Tag_" + serialNbr + "_" + date + ".json");
                                file.write(json);
                                file.flush();
                                file.close();

                                FileInputStream fileInputData = new FileInputStream(jsonCache);
                                boolean resultInData = ftp.storeFile("/Tag_" + serialNbr + "_" + date + ".json", fileInputData);
                                fileInputData.close();
                                ftp.logout();
                                ftp.disconnect();

                                if (resultInData) {
                                    Log.i(LOG_TAG, "Success uploading tag data to server");
                                    return true;
                                }
                                else {
                                    Log.i(LOG_TAG, "Error uploading tag data");
                                    return false;
                                }
                            } else {
                                Log.i(LOG_TAG, "Error uploading picture");
                                return false;
                            }
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

}
