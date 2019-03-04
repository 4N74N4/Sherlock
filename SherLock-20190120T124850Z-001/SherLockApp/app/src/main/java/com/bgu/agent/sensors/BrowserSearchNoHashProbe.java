package com.bgu.agent.sensors;

import android.net.Uri;
import android.provider.Browser;
import com.bgu.congeor.Constants;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.DatedContentProviderProbe;

import java.util.HashMap;
import java.util.Map;

//Pipe line path:  com.bgu.agent.sensors.BrowserSearchNoHashProbe


@Schedule.DefaultSchedule(interval = 10 * Constants.HOUR)
//@RequiredPermissions(android.Manifest.permission.READ_HISTORY_BOOKMARKS)
public class BrowserSearchNoHashProbe extends DatedContentProviderProbe
{

    @Override
    protected Uri getContentProviderUri()
    {
        return Uri.parse("kk");
    }

    @Override
    protected String getDateColumnName()
    {
        return "xyz";
    }

    @Override
    protected Map<String, CursorCell<?>> getProjectionMap()
    {
        Map<String, CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
       /* projectionKeyToType.put(Browser.BookmarkColumns._ID, intCell());
        projectionKeyToType.put(Browser.BookmarkColumns.TITLE, stringCell());
        projectionKeyToType.put(Browser.BookmarkColumns.URL, stringCell());
        projectionKeyToType.put(Browser.BookmarkColumns.VISITS, intCell());
        projectionKeyToType.put(Browser.BookmarkColumns.DATE, longCell());
        projectionKeyToType.put(Browser.BookmarkColumns.CREATED, longCell());
        projectionKeyToType.put(Browser.BookmarkColumns.BOOKMARK, intCell());*/
        return projectionKeyToType;
    }
}