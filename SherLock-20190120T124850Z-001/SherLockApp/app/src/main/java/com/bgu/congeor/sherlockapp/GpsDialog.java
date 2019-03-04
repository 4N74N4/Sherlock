package com.bgu.congeor.sherlockapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by simondzn on 14/06/2016.
 */
    public class GpsDialog extends Activity {
    AlertDialog alert;
        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            setContentView(R.layout.moriarty_dialog);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You must leave it on!\nIn the next screen please turn on \"Location\"")
                    .setTitle("Your GPS seems to be disabled")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                    finishAffinity();
                                }
                    });
             alert = builder.create();
            alert.show();
        }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        alert.dismiss();
        super.onDestroy();
    }
}
