package com.bgu.congeor.sherlockapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import com.bgu.agent.sensors.MoriartyProbe;

/**
 * Created by simondzn on 30/11/2015.
 */
public class MoriartyDialog extends Activity {
    AlertDialog alert;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.moriarty_dialog);
        Log.e(MoriartyProbe.class.toString(), "activity started");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You have not used the Moriarty app for more than 3 days. \nPlease open and use the app for a few minutes today.").setCancelable(
                false).setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finishAffinity();
                    }
                }).setIcon(R.drawable.sherlock_icon).setTitle("Sherlock");
        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onDestroy() {
        alert.dismiss();
        super.onDestroy();
    }
}