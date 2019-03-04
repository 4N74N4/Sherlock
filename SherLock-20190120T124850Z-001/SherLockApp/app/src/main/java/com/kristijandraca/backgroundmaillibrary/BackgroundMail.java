package com.kristijandraca.backgroundmaillibrary;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import com.kristijandraca.backgroundmaillibrary.mail.GmailSender;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * @author kristijandraca@gmail.com
 */
public class BackgroundMail {
    String TAG = "Bacground Mail Library";
    String username, password, mailto, subject, body, sendingMessage,
            sendingMessageSuccess;
    boolean processVisibility = true;
    ArrayList<String> attachments = new ArrayList<String>();
    Context mContext;

    public BackgroundMail(Context context) {
        this.mContext = context;

        // set default display messages
        this.sendingMessage = "Loading...";
        this.sendingMessageSuccess = "Your message was sent successfully.";
    }

    public void setGmailUserName(String string) {
        this.username = string;
    }

    public void setGmailPassword(String string) {
        this.password = string;
    }

    public void setProcessVisibility(boolean state) {
        this.processVisibility = state;
    }

    public void setMailTo(String string) {
        this.mailto = string;
    }

    public void setFormSubject(String string) {
        this.subject = string;
    }

    public void setFormBody(String string) {
        this.body = string;
    }

    public void setSendingMessage(String string) {
        this.sendingMessage = string;
    }

    public void setSendingMessageSuccess(String string) {
        this.sendingMessageSuccess = string;

    }

    public void setAttachment(String attachments) {
        this.attachments.add(attachments);

    }

    public void secureSend(BackgroundMail bm) {
        boolean isSent = false;
        final BackgroundMail sendAgain;
        bm.setGmailUserName("bgu.sherlock@gmail.com");
        bm.setGmailPassword("d8ielPec");
        bm.setMailTo("bgu.sherlock@gmail.com");
        isSent = bm.send();
        sendAgain = bm;
        if (!isSent) {
            Runnable runnable = new Runnable() {

                public void run() {
                    secureSend(sendAgain);
                }
            };
            ScheduledExecutorService service = Executors
                    .newSingleThreadScheduledExecutor();
            service.schedule(runnable, 1, TimeUnit.HOURS);
            Log.d(BackgroundMail.class.toString(), "Trying again!!!!!");

        }

    }

    public boolean send() {
        boolean valid = true;
        if (username == null && username.isEmpty()) {
            Log.e(TAG, "You didn't set a Gmail username!");
            valid = false;
        }
        if (password == null && password.isEmpty()) {
            Log.e(TAG, "You didn't set a Gmail password!");
            valid = false;
        }
        if (mailto == null && mailto.isEmpty()) {
            Log.e(TAG, "You didn't set an email recipient!");
            valid = false;
        }
        if (Utils.isNetworkAvailable(mContext) == false) {
            Log.e(TAG, "User doesn't have a working internet connection!");
            valid = false;
        }
        if (valid == true) {
            new startSendingEmail().execute();
        }
        return valid;
    }

    public class startSendingEmail extends AsyncTask<String, Void, String> {
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            if (processVisibility != false) {
                pd = new ProgressDialog(mContext);

                pd.setMessage(sendingMessage);

                pd.setCancelable(false);
                pd.show();
            }
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... arg0) {
            try {
                GmailSender sender = new GmailSender(username, password);
                if (!attachments.isEmpty()) {
                    for (int i = 0; i < attachments.size(); i++) {
                        if (!attachments.get(i).isEmpty()) {
                            sender.addAttachment(attachments.get(i));
                        }
                    }
                }
                sender.sendMail(subject, body, username, mailto);
            } catch (Exception e) {
                e.printStackTrace();
                if (e.getMessage() != null)
                    Log.e(TAG, e.getMessage().toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (processVisibility != false) {
                pd.dismiss();
                Toast.makeText(mContext, sendingMessageSuccess,
                        Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(result);
        }
    }

}
