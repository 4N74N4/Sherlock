package com.bgu.congeor.sherlockapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.manager.CongeorManager;

/**
 * Created by shedan on 11/12/2014.
 */
public class LoginActivity extends Activity {
    boolean loginSuccess;
    private ProgressBar spinner;
    String connectionSuccess,hashValidity;
    IRunnableWithParameters uiRunnable;

    @Override
    public void onCreate(final Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
        setContentView(R.layout.login_simple);
        uiRunnable = new IRunnableWithParameters() {
            @Override
            public void run(Object... params) {
                spinner.setVisibility(View.GONE);
                if(params!=null && params[0]!=null)
                {
                    if(params[0].equals(Constants.SERVER_ERROR))
                    {
                        Toast.makeText(getApplicationContext(), "Server Error, Please try again later!", Toast.LENGTH_LONG).show();
                    }
                    if(params[0].equals(Constants.INVALID_HASH)){
                        Toast.makeText(getApplicationContext(), "Email is invalid. please try again!", Toast.LENGTH_LONG).show();
                    }
                    if(params[0].equals(Constants.VALID_HASH)){
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext() ,MainActivity.class);
                        intent.putExtra("LoginStatus" , "logged");
                        startActivity(intent);
                        finish();
                    }
                }
            }
        };
    }
    @Override
    public void onStart(){

        super.onStart();

        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);
        final Button buttonSend = (Button) findViewById(R.id.sendLoginButton);
//        buttonSend.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                EditText emailText = (EditText) findViewById(R.id.email);
//                String email = emailText.getText().toString();
//                if(!email.equals("")){
//                CongeorManager.getInstance().LoginUser(uiRunnable,email);
//                    spinner.setVisibility(View.VISIBLE);
//                }
//            }
//        });
        buttonSend.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        Button view = (Button) v;
                        view.getBackground().setColorFilter(0x959D9E, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    EditText emailText = (EditText) findViewById(R.id.email);
                    String email = emailText.getText().toString().trim();
                    if(!email.equals("")){
                        CongeorManager.getInstance().LoginUser(uiRunnable,email);
                        spinner.setVisibility(View.VISIBLE);
                        buttonSend.setClickable(false);
                    }

                        // Your action here on button click
                    case MotionEvent.ACTION_CANCEL: {
                        Button view = (Button) v;
                        view.getBackground().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }
                return true;
            }
        });
        if(!Utils.isOnline(getApplicationContext(),false))
        {
            buttonSend.setText("Cant really Send with no Network Connection");

            buttonSend.setClickable(false);
            Toast.makeText(getApplicationContext(), "No Internet Connection, please try again later", Toast.LENGTH_LONG).show();

        }
        else{
            buttonSend.setText("Send");
            buttonSend.setClickable(true);


        }

    }
    @Override
    public void onResume(){
        super.onResume();
        Button buttonSend = (Button) findViewById(R.id.sendLoginButton);
        buttonSend.setText("Send");
        buttonSend.setClickable(true);
    }


    @Override
    protected void onPause()
    {

        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

}
