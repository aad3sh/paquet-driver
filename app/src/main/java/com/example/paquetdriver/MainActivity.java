package com.example.paquetdriver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView name;
    private TextView tagline;
    private ImageView logo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = (TextView) findViewById(R.id.paquet);
        tagline = (TextView)findViewById(R.id.tagline);
        logo = (ImageView) findViewById(R.id.imageView3);
        Animation myanim = AnimationUtils.loadAnimation(this, R.anim.splashscreen_transistion);
        name.startAnimation(myanim);
        tagline.startAnimation(myanim);
        logo.startAnimation(myanim);

        final Intent i = new Intent(this, LoginActivity.class);
        Thread thread = new Thread(){
            public void run(){
                try{
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally{
                    startActivity(i);
                    finish();
                }
            }
        };
        thread.start();
    }
}
