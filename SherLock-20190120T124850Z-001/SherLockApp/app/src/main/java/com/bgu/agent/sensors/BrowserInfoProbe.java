package com.bgu.agent.sensors;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;


// com.bgu.agent.sensors.BrowserInfoProbe

@Probe.DisplayName("BrowserInfoProbe")
@Schedule.DefaultSchedule(interval = 30 * Constants.MINUTE, duration = 0 * Constants.SECOND)
public class BrowserInfoProbe extends SecureBase
{

    //BOOKMARKS DEFINITIONS
    private static final Uri CHROME_BOOKMARKS = Uri.parse("content://com.android.chrome.browser/bookmarks");

    //private static final Uri DEFAULT_BROWSER_BOOKMARKS = Browser.BOOKMARKS_URI;

    //SEARCHES DEFINITIONS
    private static final Uri CHROME_SEARCHES = Uri.parse("content://com.android.chrome.browser/searches");

    //private static final Uri DEFAULT_BROWSER_SEARCHES = Browser.SEARCHES_URI;

    private static final long MILLISECONDS_IN_SECOND = 1000;

    @Configurable
    long aggInterval = 30 * Constants.MINUTE;

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();

        JsonArray chromeBookmarks, chromeSearches, defaultBrowserBookmarks, defaultBrowserSearches;
        /*long currentTime = System.currentTimeMillis();
        String bookmarkSelector = currentTime + " - " + Browser.BookmarkColumns.DATE + "< " +
                (aggInterval * MILLISECONDS_IN_SECOND);
        String searchSelector = currentTime + " - " + Browser.SearchColumns.DATE + " < " + (aggInterval * MILLISECONDS_IN_SECOND);

        chromeBookmarks = getDataFormUri(CHROME_BOOKMARKS, bookmarkSelector, Browser.BookmarkColumns.DATE,
                Browser.BookmarkColumns.URL);
        chromeSearches = getDataFormUri(CHROME_SEARCHES, searchSelector, Browser.SearchColumns.DATE,
                Browser.SearchColumns.SEARCH);
        defaultBrowserBookmarks = getDataFormUri(DEFAULT_BROWSER_BOOKMARKS, bookmarkSelector,
                Browser.BookmarkColumns.DATE, Browser.BookmarkColumns.URL);
        defaultBrowserSearches = getDataFormUri(DEFAULT_BROWSER_SEARCHES, searchSelector, Browser.SearchColumns.DATE,
                Browser.SearchColumns.SEARCH);

        JsonObject data = new JsonObject();
        data.add("ChromeBookmarks", chromeBookmarks);
        data.add("DefaultBrowserBookmarks", defaultBrowserBookmarks);
        data.add("ChromeSearches", chromeSearches);
        data.add("DefaultBrowserSearches", defaultBrowserSearches);
        sendData(data);
        Log.e(BrowserInfoProbe.class.toString(), data.toString());
        stop();*/
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
