package com.bgu.agent.commons.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class AlertUtil {

    private static final int MESSAGE_ALERT = 1;
    private static final int CONFIRM_ALERT = 2;
    private static final int DECISION_ALERT = 3;

    public static void messageAlert(Context ctx, String title, String message) {
        showAlertDialog(MESSAGE_ALERT, ctx, title, message, null, null, "OK");
    }

    public static void messageAlert(Context ctx, String title, String message, int time) {
        showAlertDialog(MESSAGE_ALERT, ctx, title, message, null, time, "OK");
    }

    public static void confirmationAlert(Context ctx, String title, String message, DialogInterface.OnClickListener callBack) {
        showAlertDialog(CONFIRM_ALERT, ctx, title, message, callBack, null, "OK");
    }

    public static void decisionAlert(Context ctx, String title, String message, DialogInterface.OnClickListener posCallback, String... buttonNames) {
        showAlertDialog(DECISION_ALERT, ctx, title, message, posCallback, null, buttonNames);
    }

    public static void showAlertDialog(int alertType, Context ctx, String title, String message, DialogInterface.OnClickListener posCallback, Integer time, String... buttonNames) {
        if ( message == null ) message = "default message";

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder = builder.setTitle(title)
                .setMessage(message)

        // false = pressing back button won't dismiss this alert
                .setCancelable(false);

        // icon on the left of title

        switch (alertType) {
            case MESSAGE_ALERT:
                builder = builder.setNegativeButton(buttonNames [buttonNames.length - 1], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder = builder.setIcon(android.R.drawable.ic_dialog_info);
                break;

            case CONFIRM_ALERT:
                builder.setPositiveButton(buttonNames[0], posCallback);
                builder = builder.setIcon(android.R.drawable.ic_dialog_alert);
                break;

            case DECISION_ALERT:
                builder = builder.setIcon(android.R.drawable.ic_dialog_alert);
                break;
        }

        final AlertDialog dialog = builder.create();
        dialog.show();
    /*    if ( time != null ){
            Handler handler = null;
            handler = new Handler();
            handler.postDelayed(new Runnable(){
                public void run(){
                    dialog.cancel();
                    dialog.dismiss();
                }
            }, time);
        }*/
    }
}