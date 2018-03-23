package com.tagtoo.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class WriteTabFragment extends Fragment {

    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_write, container, false);

        Button buttonText = rootView.findViewById(R.id.buttontext);

        buttonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWriteActivity(WriteTextActivity.class);
            }
        });

        return rootView;
    }

    public void startWriteActivity(final Class<? extends Activity>  activity){
        Intent intent = new Intent(getActivity(), activity);
        startActivity(intent);
    }
}
