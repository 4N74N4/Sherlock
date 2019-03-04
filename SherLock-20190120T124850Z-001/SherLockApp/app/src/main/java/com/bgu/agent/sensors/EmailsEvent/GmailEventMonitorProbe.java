package com.bgu.agent.sensors.EmailsEvent;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;

/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/25/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */

//Pipe line path:  com.bgu.agent.sensors.EmailsEvent.GmailEventMonitorProbe

@DisplayName("GmailEventMonitorProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)

public class GmailEventMonitorProbe extends SecureBase implements ContinuousProbe
{


    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private static final String[] FEATURES_MAIL = {"service_mail"};

    ContentObserver emailContentObserver;

    Account[] accounts = null;
    JsonArray lastSession = null;

    long smsTime = 0;

    public Account[] getAccounts()
    {
        return accounts;
    }

    public void setAccounts(Account[] accounts)
    {
        this.accounts = accounts;
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();

        AccountManager accountManager;

        if ((accountManager = AccountManager.get(getContext())) != null)
        {
            accountManager.getAccountsByTypeAndFeatures(ACCOUNT_TYPE_GOOGLE, FEATURES_MAIL,
                    new AccountManagerCallback<Account[]>()
                    {
                        @Override
                        public void run(AccountManagerFuture<Account[]> future)
                        {
                            Account[] accounts = null;
                            try
                            {
                                accounts = future.getResult();
                            }
                            catch (Throwable t)
                            {
                                Logger.e(GmailEventMonitorProbe.class, "", t);
                            }
                            onAccountResults(accounts);
                        }
                    }, null /* handler */);
        }

    }

    private void onAccountResults(Account[] accounts)
    {
        setAccounts(accounts);


        if (getAccounts() != null && getAccounts().length > 0)
        {
            emailContentObserver = new ContentObserver(new android.os.Handler())
            {
                @Override
                public void onChange(boolean selfChange)
                {
                    super.onChange(selfChange);
                    String change = compareSession(getEmailSession(), lastSession);

                    if (change.equals("IncomingMail"))
                    {
                        Intent emailIntent = new Intent(Constants.INTENT_ACTION_INCOMING_MAIL);
                        getContext().sendBroadcast(emailIntent);
                    }
                    if (change.equals("OutgoingMail"))
                    {
                        Intent emailIntent = new Intent(Constants.INTENT_ACTION_OUTGOING_MAIL);
                        getContext().sendBroadcast(emailIntent);
                    }
                }
            };

            lastSession = getEmailSession();

            for (int i = 0; i < accounts.length; i++)
            {
                JsonArray labelsData =
                        getDataFormUri(
                                GmailContract.Labels.getLabelsUri(accounts[i].name),
                                null,
                                GmailContract.Labels.CANONICAL_NAME,
                                GmailContract.Labels.URI
                        );

                for (JsonElement labelData : labelsData)
                {
                    JsonObject data = labelData.getAsJsonObject();

                    String canonicalName = null;

                    if (data != null)
                    {
                        canonicalName = data.get(GmailContract.Labels.CANONICAL_NAME).getAsString();

                        Uri inboxURI = null;
                        Uri sentURI = null;

                        if ((canonicalName != null) && (canonicalName.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX)))
                        {
                            inboxURI = data.get(GmailContract.Labels.URI) != null ? Uri.parse(data.get(GmailContract.Labels.URI).getAsString()) : null;
                        }

                        if ((canonicalName != null) && (canonicalName.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_SENT)))
                        {
                            sentURI = data.get(GmailContract.Labels.URI) != null ? Uri.parse(data.get(GmailContract.Labels.URI).getAsString()) : null;
                        }

                        if (inboxURI != null)
                        {
                            getContext().getContentResolver().registerContentObserver(inboxURI, true, emailContentObserver);
                        }
                        if (sentURI != null)
                        {
                            Cursor cur = getContext().getContentResolver().query(sentURI, null, null, null, null);
                            getContext().getContentResolver().registerContentObserver(sentURI, true, emailContentObserver);

                        }

                    }

                }
            }


        }
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();
        getContext().getContentResolver().unregisterContentObserver(emailContentObserver);
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }

    private JsonArray getDataFormUri(Uri explorerUri, String selector, String... columnsNames)
    {
        JsonArray dataList = new JsonArray();
        Cursor cursor = null;
        try
        {
            cursor = getContext().getContentResolver().query(explorerUri, columnsNames, selector, null, null);
            cursor.moveToFirst();
            do
            {
                JsonObject row = new JsonObject();
                for (String column : columnsNames)
                {
                    row.addProperty(column, cursor.getString(cursor.getColumnIndex(column)));
                }
                dataList.add(row);
            } while (cursor.moveToNext());
        }
        catch (Exception e)
        {
            Logger.e(getClass(), e.getMessage());
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return dataList;
    }

    public JsonArray getEmailSession()
    {
        if (accounts != null && accounts.length > 0)
        {
            JsonArray allEmailAccountsData = new JsonArray();

            for (int i = 0; i < accounts.length; i++)
            {
                JsonObject accountData = new JsonObject();

                accountData.addProperty("AccountName", accounts[i].name);
                JsonArray labelsData =
                        getDataFormUri(
                                GmailContract.Labels.getLabelsUri(accounts[i].name),
                                null,
                                GmailContract.Labels.NAME,
                                GmailContract.Labels.CANONICAL_NAME,
                                GmailContract.Labels.NUM_CONVERSATIONS,
                                GmailContract.Labels.NUM_UNREAD_CONVERSATIONS
                        );

                accountData.add("LabelsData", labelsData);

                allEmailAccountsData.add(accountData);
            }
            return allEmailAccountsData;
        }
        return null;
    }

    public String compareSession(JsonArray newSession, JsonArray oldSession)
    {
        for (JsonElement oldSessionElement : oldSession)
        {
            for (JsonElement newSessionElement : newSession)
            {
                String oldAccountName = oldSessionElement.getAsJsonObject().get("AccountName").getAsString();
                String newAccountName = oldSessionElement.getAsJsonObject().get("AccountName").getAsString();

                if (oldAccountName.equals(newAccountName))
                {
                    for (JsonElement oldLabel : ((oldSessionElement.getAsJsonObject()).get("LabelsData")).getAsJsonArray())
                    {

                        for (JsonElement newLabel : ((newSessionElement.getAsJsonObject()).get("LabelsData")).getAsJsonArray())
                        {
                            String oldLabelName = oldLabel.getAsJsonObject().get(GmailContract.Labels.CANONICAL_NAME).getAsString();
                            String newLabelName = newLabel.getAsJsonObject().get(GmailContract.Labels.CANONICAL_NAME).getAsString();

                            if (oldLabelName.equals(newLabelName))
                            {
                                if (!oldLabel.getAsJsonObject().get(GmailContract.Labels.NUM_CONVERSATIONS).getAsString().equals(newLabel.getAsJsonObject().get(GmailContract.Labels.NUM_CONVERSATIONS).getAsString()))
                                {
                                    if (oldLabelName.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX))
                                    {
                                        lastSession = newSession;
                                        return "IncomingMail";
                                    }
                                    if (oldLabelName.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_SENT))
                                    {
                                        lastSession = newSession;
                                        return "OutgoingMail";
                                    }
                                }

                            }

                        }
                    }
                }

            }
        }
        return "NoChange";
    }
}

