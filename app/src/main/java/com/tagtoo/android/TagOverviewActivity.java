package com.tagtoo.android;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class TagOverviewActivity extends AppCompatActivity {

    private static Context context;

    ImageView iconText, iconAudio, iconPicture, iconVideo;
    HorizontalScrollView hScrollView;
    LinearLayout textLayout, audioLayout, pictureLayout, videoLayout;
    LinearLayout.LayoutParams layoutParams;
    GestureDetector gestureDetector;
    ArrayList<LinearLayout> layoutsList;
    int dispWidth, currentPos;
    int xStart, yStart;
    boolean hasText, hasAudio, hasPicture, hasVideo;
    String serialNbr;
    String LOG_TAG = "TAG_OVERVIEW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_overview);

        iconText      = findViewById(R.id.messageTextIcon);
        iconAudio     = findViewById(R.id.messageAudioIcon);
        iconPicture   = findViewById(R.id.messagePictureIcon);
        iconVideo     = findViewById(R.id.messageVideoIcon);
        hScrollView   = findViewById(R.id.hsv);
        textLayout    = findViewById(R.id.textLayout);
        audioLayout   = findViewById(R.id.audioLayout);
        pictureLayout = findViewById(R.id.pictureLayout);
        videoLayout   = findViewById(R.id.videoLayout);

        TagOverviewActivity.context = getApplicationContext();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dispWidth = size.x;
        layoutsList = new ArrayList<>();
        layoutParams = new LinearLayout.LayoutParams(dispWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

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

            hScrollView.smoothScrollTo(layoutsList.get(currentPos).getLeft(), 0);
            return true;
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


    @Override
    protected void onStart() {
        super.onStart();


        TextView title = findViewById(R.id.card_title);
        TextView info  = findViewById(R.id.card_info);
        title.setText(getIntent().getStringExtra("name"));
        info.setText(getIntent().getStringExtra("info"));

        serialNbr = getIntent().getStringExtra("serial");

        Log.i(LOG_TAG, serialNbr);

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
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        xStart = textLayout.getLeft();
        yStart = textLayout.getTop();
        hScrollView.scrollTo(0, 0);
    }

}
