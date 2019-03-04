package com.bgu.agent.sensors;


import android.net.Uri;
import android.provider.CallLog;
import com.bgu.congeor.Constants;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.DatedContentProviderProbe;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: BittonRon
 * Date: 12/26/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */

//Pipe line path:  com.bgu.agent.sensors.CallLogNoHashProbe

@Schedule.DefaultSchedule(interval = 10 * Constants.HOUR)
@Probe.RequiredPermissions(android.Manifest.permission.READ_CONTACTS)

public class CallLogNoHashProbe extends DatedContentProviderProbe
{

    @Override
    protected Uri getContentProviderUri()
    {
        return CallLog.Calls.CONTENT_URI;
    }

    @Override
    protected String getDateColumnName()
    {
        return CallLog.Calls.DATE;
    }

    @Override
    protected Map<String, CursorCell<?>> getProjectionMap()
    {
        Map<String, CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
        projectionKeyToType.put(CallLog.Calls._ID, intCell());
        projectionKeyToType.put(CallLog.Calls.NUMBER, new CursorCell.PhoneNumberCell());
        projectionKeyToType.put(CallLog.Calls.DATE, longCell());
        projectionKeyToType.put(CallLog.Calls.TYPE, intCell());
        projectionKeyToType.put(CallLog.Calls.DURATION, longCell());
        projectionKeyToType.put(CallLog.Calls.CACHED_NAME, stringCell());
        projectionKeyToType.put(CallLog.Calls.CACHED_NUMBER_LABEL, stringCell());
        projectionKeyToType.put(CallLog.Calls.CACHED_NUMBER_TYPE, stringCell());
        return projectionKeyToType;
    }
}

