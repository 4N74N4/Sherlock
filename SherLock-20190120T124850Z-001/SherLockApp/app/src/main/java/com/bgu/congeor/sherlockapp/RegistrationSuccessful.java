package com.bgu.congeor.sherlockapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.bgu.congeor.sherlockapp.baseactivities.BaseActivity;

/**
 * Created by clint on 1/5/14.
 */
public class RegistrationSuccessful extends BaseActivity
{
    private final int SPLASH_DISPLAY_LENGTH = 1000;

    public void onCreate(Bundle savedInstanceState)
    {   super.onCreate(savedInstanceState);
        setContentView(R.layout.registrationsuccessful);
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                /* Create an Intent that will start the Menu-Activity. */
                Intent mainIntent = new Intent(RegistrationSuccessful.this, MainActivity.class);
                RegistrationSuccessful.this.startActivity(mainIntent);
                RegistrationSuccessful.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}