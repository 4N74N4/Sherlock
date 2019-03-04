package com.bgu.congeor.sherlockapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;

/**
 * Created by clint on 1/2/14.
 */
public class NoInternet extends Activity
{
    Button okButton;
    Button sendErrorButton;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nointernet);
        okButton = Utils.getView(this, R.id.noInternet_okButton);
        String errorCode = "";
        String error = "";
        if (getIntent().hasExtra("errorCode"))
        {
            errorCode = getIntent().getStringExtra("errorCode");
        }
        if (getIntent().hasExtra("error"))
        {
            error = getIntent().getStringExtra("error");
        }
        final String errorToShow = error;
        sendErrorButton = Utils.getView(this, R.id.noInternet_sendError);
        sendErrorButton.setVisibility(errorCode.equalsIgnoreCase("" + Constants.NO_INTERNET_EXCEPTION) ? View.INVISIBLE : View.VISIBLE);
        if (!errorCode.equalsIgnoreCase("" + Constants.NO_INTERNET_EXCEPTION))
        {
            TextView cannotConnect = Utils.getView(this, R.id.cannotConnect1);
            cannotConnect.setVisibility(View.GONE);
        }
        sendErrorButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Utils.sendEmail(NoInternet.this.getApplicationContext(), errorToShow, Utils.getLogcatLog(NoInternet.this.getApplicationContext()), Utils.getEmail(getApplicationContext(), getApplication()));
            }
        });
        okButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }
}