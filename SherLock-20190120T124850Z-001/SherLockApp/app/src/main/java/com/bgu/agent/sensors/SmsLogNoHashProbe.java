package com.bgu.agent.sensors;

/**
 * Created by BittonRon on 2/13/14.
 */

import android.net.Uri;
import com.bgu.congeor.Constants;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.DatedContentProviderProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;

import java.util.HashMap;
import java.util.Map;

@Schedule.DefaultSchedule(interval = 10 * Constants.HOUR)
@Probe.DisplayName("SmsLogNoHashProbe")
@Probe.RequiredPermissions(android.Manifest.permission.READ_SMS)
public class SmsLogNoHashProbe extends DatedContentProviderProbe implements ProbeKeys.SmsKeys
{

    @Override
    protected Uri getContentProviderUri()
    {
        return ProbeKeys.AndroidInternal.Sms.CONTENT_URI;
    }

    @Override
    protected String getDateColumnName()
    {
        return ProbeKeys.AndroidInternal.Sms.DATE;
    }

    @Override
    protected Map<String, CursorCell<?>> getProjectionMap()
    {
        Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.TYPE, intCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.THREAD_ID, intCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.ADDRESS, stringCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.PERSON_ID, longCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.DATE, longCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.READ, booleanCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.STATUS, intCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.BODY, stringCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.PERSON, stringCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.PROTOCOL, intCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.REPLY_PATH_PRESENT, booleanCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.SERVICE_CENTER, stringCell());
        projectionMap.put(ProbeKeys.AndroidInternal.Sms.LOCKED, booleanCell());
        return projectionMap;
    }

}

