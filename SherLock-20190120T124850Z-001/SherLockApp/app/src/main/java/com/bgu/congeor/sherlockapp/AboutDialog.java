package com.bgu.congeor.sherlockapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: stan
 * Date: 26/12/13
 * Time: 10:00
 * To change this template use File | Settings | File Templates.
 */
public class AboutDialog extends DialogFragment
{

    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View aboutView = View.inflate(getActivity(), R.layout.about, null);
        return new AlertDialog.Builder(getActivity())
                .setTitle("About").setView(aboutView)
                .setPositiveButton("Ok", new OkButtonListener())
                .create();
    }

    private class OkButtonListener implements DialogInterface.OnClickListener
    {

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
        }
    }
}
