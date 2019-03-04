package com.bgu.agent.sensors;


import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

// com.bgu.agent.sensors.ContactsListProbe

@Probe.DisplayName("ContactsListProbe")
@Probe.RequiredPermissions(android.Manifest.permission.READ_CONTACTS)
@Schedule.DefaultSchedule(interval = Constants.WEEK)

public class ContactsListProbe extends SecureBase
{


    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        JsonObject data = new JsonObject();
        JsonArray contactList = getContactList();
        data.add("Contacts", contactList);
        sendData(data);
        stop();
    }

    private JsonArray getContactList()
    {

        JsonArray contactsList = new JsonArray();
        JsonObject contact;
        Cursor cursor = null;

        try
        {
            cursor = getContext().getContentResolver().query(Phone.CONTENT_URI, null, null, null, null);
            int nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME);
            int phoneNumberIdx = cursor.getColumnIndex(Phone.NUMBER);
            cursor.moveToFirst();

            do
            {
                contact = new JsonObject();
                //contact.addProperty("Name", cursor.getString(nameIdx));
                contact.addProperty("PhoneNumber", Utils.hashWithSHA(cursor.getString(phoneNumberIdx).replace("-",
                        "").replace("+972", "0").replace(" ", "")));
                contactsList.add(contact);
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
        return contactsList;
    }
}
