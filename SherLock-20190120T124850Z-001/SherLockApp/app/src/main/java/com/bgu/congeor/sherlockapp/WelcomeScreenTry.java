package com.bgu.congeor.sherlockapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.manager.AgentManager;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.baseactivities.BaseActivity;
import com.bgu.congeor.sherlockapp.manager.CongeorManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by clint on 1/5/14.
 */
public class WelcomeScreenTry extends BaseActivity {

    private final int SPLASH_DISPLAY_LENGTH = 1800;
    private boolean fastLogin = false;
    private final int startSampling = 1;
    private final int afterLogin = 2;
    Class nextScreen;
    Thread thread = null;
    String error = "";
    String errorCode = "";
    int elapsedTime = 0;
    boolean hashedMailExist;
    IRunnableWithParameters uiRunnable;
//    WebView gif;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcomescreen);
        if (getIntent().hasExtra(Constants.FAST_LOGIN)) {
            fastLogin = true;
        }
        Utils.deleteImages(getApplicationContext());
        Animation textFadeIn = new AlphaAnimation(0, 1);
        textFadeIn.setInterpolator(new AccelerateInterpolator()); //add this
        textFadeIn.setDuration(SPLASH_DISPLAY_LENGTH);

        Animation logoFadeIn = new AlphaAnimation(0, 1);
        logoFadeIn.setInterpolator(new AccelerateInterpolator()); //and this
        logoFadeIn.setStartOffset(SPLASH_DISPLAY_LENGTH / 2);
        logoFadeIn.setDuration(SPLASH_DISPLAY_LENGTH / 2);

        AnimationSet textAnimation = new AnimationSet(false); //change to false
        textAnimation.addAnimation(textFadeIn);
        AnimationSet logoAnimation = new AnimationSet(false); //change to false
        logoAnimation.addAnimation(logoFadeIn);
        ImageView t = Utils.getView(this, R.id.animatedLogoImg);
        TextView text = Utils.getView(this, R.id.text1);
        if (!fastLogin) {
            t.setAnimation(logoAnimation);
            text.setAnimation(textAnimation);
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startProcess();
            }
        }, 900);


    }

    private void startProcess() {
        Configuration userData = null;
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/" + Constants.APPLICATION_NAME + "/" + Constants.CONF_USER_DATA_HASHED);

            boolean exists = dir.exists();
            userData = Configuration.loadLocalConfiguration(getApplicationContext(), Constants.CONF_USER_DATA_HASHED, !exists);
            String hashedMail = userData.getKeyAsString(Constants.HASHED_MAIL);
            if (hashedMail.equals("")) {
                if (CongeorManager.getInstance().isInitialized()) {
                    Intent intent = new Intent(this.getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startProcess();
                        }
                    }, 900);
                }
            } else {
                Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
                intent.putExtra("LoginStatus", "logged");
                startActivity(intent);
                startSampling();
//                Intent i = new Intent(getApplicationContext(), SensorService.class);
//                startService(i);
                finish();
            }
        } catch (IOException e) {
            Log.e(WelcomeScreenTry.class.toString(), "Failed to open Hashed data file");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            try {
                thread.join();
                thread = null;
            } catch (InterruptedException e) {
                Logger.e(WelcomeScreenTry.class, "Thread not stopped");
            }
        }
    }

    private void startSampling() {
        IRunnableWithParameters runnableWithParameters = new IRunnableWithParameters() {
            @Override
            public void run(Object... params) {
                AgentManager.startSampling(getApplicationContext());
            }
        };
        Log.e(WelcomeScreenTry.class.toString(), "Login in CongeorManager called from welcome screen!!!");
        CongeorManager.getInstance().login(runnableWithParameters);
    }
}