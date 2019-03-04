/**
 *
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 *
 * This file is part of Funf.
 *
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.bgu.agent.sensors;


import android.os.Bundle;
import android.util.Log;
import com.bgu.agent.commons.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.util.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Reads various information from the /proc file system.
 * <p/>
 * Based on the SystemSens Proc Sensor, written by Hossein Falaki.
 *
 * @author Alex Kletsky
 */
@Schedule.DefaultSchedule(interval = 300)
public class ProcessStatisticsProbe extends Base
{

    //
    @Override
    protected void onStart()
    {
        Gson gson = getGson();
        JsonObject data = new JsonObject();

        data.add("proc", gson.toJsonTree(this.getData()));
        sendData(data);
        stop();
    }

    @Override
    protected void onStop()
    {
        Logger.d(getClass(), "Alex");
    }


    /**
     * @return bundle with data from the /proc folder
     */
    public Bundle getData()
    {
        Bundle bundle = new Bundle();
        recFileTree(bundle, "/proc", 3);

        return bundle;
    }

    /**
     * Create bundle tree recursivly the depth level is defined by @level param
     *
     * @param bundle the bunle to add the tree too
     * @param path   the path from where create the tree
     * @param level  the depth level of the recurcive tree
     */
    private void recFileTree(Bundle bundle, String path, int level)
    {
        if (level == 0 || path.contains("/fd"))
        {
            return;
        }

        Log.e(LogUtil.TAG, path);
        String line;
        File dir = new File(path);
        for (File f : dir.listFiles())
        {
            if (f.canRead())
            {
                if (f.isDirectory())
                {
                    Bundle subBundle = new Bundle();
                    bundle.putBundle(f.getName(), subBundle);
                    recFileTree(subBundle, f.getPath(), level - 1);
                }
                else
                {
                    try
                    {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)), 2048);

                        StringBuilder sb = new StringBuilder();
                        while ((line = reader.readLine()) != null)
                        {
                            sb.append(line).append("\n");
                        }

                        reader.close();

                        bundle.putString(f.getName(), sb.toString());
                    }
                    catch (Exception e)
                    {
                        Log.e(LogUtil.TAG, "Exception parsing the file: " + f.getPath(), e);
                    }
                }
            }
        }
    }
}
