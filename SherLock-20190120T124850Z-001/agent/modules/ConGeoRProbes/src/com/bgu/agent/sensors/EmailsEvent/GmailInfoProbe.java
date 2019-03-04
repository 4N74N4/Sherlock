package com.bgu.agent.sensors.EmailsEvent;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.database.Cursor;
import android.net.Uri;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.sensors.SecureBase;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;


// com.bgu.agent.sensors.EmailsEvent.GmailInfoProbe

@Probe.DisplayName("GmailInfoProbe")
@Schedule.DefaultSchedule(interval = 12 * Constants.HOUR, duration = 0 * Constants.SECOND)
public class GmailInfoProbe extends SecureBase
{
    private static final String GMAIL_CONTENT = "content://gmail-ls/labels/";
    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    private static final String[] FEATURES_MAIL = {"service_mail"};

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();

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
                                Logger.e(GmailInfoProbe.class, "", t);
                            }
                            onAccountResults(accounts);
                        }
                    }, null /* handler */);
        }
    }

    private void onAccountResults(Account[] accounts)
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
                                GmailContract.Labels.NUM_CONVERSATIONS,
                                GmailContract.Labels.NUM_UNREAD_CONVERSATIONS
                        );

                accountData.add("LabelsData",labelsData);

                allEmailAccountsData.add(accountData);
            }

                JsonObject dataToSend = new JsonObject();

                dataToSend.add("EmailsData",allEmailAccountsData);
                sendData(dataToSend);
        }
        stop();


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
}
