package net.melove.app.chat.call;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import net.melove.app.chat.R;

public class MLVoiceCallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);


    }

    private View.OnClickListener viewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


        }
    };
}