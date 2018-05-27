package com.tagtoo.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class TagOverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_overview);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TextView title = findViewById(R.id.card_title);
        title.setText(getIntent().getStringExtra("name"));
    }

}
