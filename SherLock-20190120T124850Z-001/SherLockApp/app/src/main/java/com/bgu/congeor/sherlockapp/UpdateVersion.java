package com.bgu.congeor.sherlockapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.bgu.agent.commons.utils.Utils;

/**
 * Created by clint on 1/2/14.
 */
public class UpdateVersion extends Activity
{

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.updateversion);
        Button updateBtn = Utils.getView(this, R.id.updateVersion_updateBtn);
        final Context context = this;
        updateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.bgu.congeor.sherlockapp"));
                startActivity(intent);

            }
        });
    }
}