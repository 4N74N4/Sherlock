package com.bgu.congeor.sherlockapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by simondzn on 26/06/2016.
 */
public class DataDialog extends Activity {
    AlertDialog alert;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.moriarty_dialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You didn't sent data for more then 3 days.\nPlease connect to wifi and do a force upload.").setCancelable(
                false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).setNegativeButton("Remained me later",
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