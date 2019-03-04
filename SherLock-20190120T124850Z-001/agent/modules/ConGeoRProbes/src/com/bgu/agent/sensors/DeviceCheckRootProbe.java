package com.bgu.agent.sensors;

import android.util.Log;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by BittonRon on 2/20/14.
 */

// com.bgu.agent.sensors.DeviceCheckRootProbe

@DisplayName("DeviceCheckRootProbe")
@Schedule.DefaultSchedule(interval = Constants.WEEK)
public class DeviceCheckRootProbe extends SecureBase
{


    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        JsonObject data = new JsonObject();
        data.addProperty(Constants.IS_ROOTED, new RootVerify().isDeviceRooted());
        sendData(data);
        stop();
    }


    enum SHELL_CMD
    {
        check_su_binary(new String[]{"/system/xbin/which", "su"});

        String[] command;

        SHELL_CMD(String[] command)
        {
            this.command = command;
        }
    }

    class RootVerify
    {

        private String LOG_TAG = RootVerify.class.getName();

        public boolean isDeviceRooted()
        {
            return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
        }

        public boolean checkRootMethod1()
        {
            String buildTags = android.os.Build.TAGS;
            return buildTags != null && buildTags.contains("test-keys");
        }

        public boolean checkRootMethod2()
        {
            try
            {
                File file = new File("/system/app/Superuser.apk");
                return file.exists();
            }
            catch (Exception e)
            {
                return false;
            }
        }

        public boolean checkRootMethod3()
        {
            return new ExecShell().executeCommand(SHELL_CMD.check_su_binary) != null;
        }
    }

    /**
     * @author Kevin Kowalewski
     */
    class ExecShell
    {

        private String LOG_TAG = ExecShell.class.getName();


        public ArrayList<String> executeCommand(SHELL_CMD shellCmd)
        {
            String line = null;
            ArrayList<String> fullResponse = new ArrayList<String>();
            Process localProcess = null;
            try
            {
                localProcess = Runtime.getRuntime().exec(shellCmd.command);
            }
            catch (Exception e)
            {
                return null;
            }
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(localProcess.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
            try
            {
                while ((line = in.readLine()) != null)
                {
                    Log.d(LOG_TAG, "--> Line received: " + line);
                    fullResponse.add(line);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            Log.d(LOG_TAG, "--> Full response was: " + fullResponse);
            return fullResponse;
        }
    }


}