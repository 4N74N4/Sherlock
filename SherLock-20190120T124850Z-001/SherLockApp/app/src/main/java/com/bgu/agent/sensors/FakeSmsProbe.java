package com.bgu.agent.sensors;

import android.content.Intent;
import com.bgu.agent.commons.logging.Logger;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


//Pipe line path:  com.bgu.agent.sensors.FakeSmsProbe


@Probe.DisplayName("FakeSmsProbe")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class FakeSmsProbe extends SecureBase implements Probe.ContinuousProbe
{
    Timer timer;
    TimerTask refresher;

    private void sendSMS(long smsTime)
    {
        Intent smsIntent = new Intent("com.bgu.congeor.SmsSent");
        smsIntent.putExtra("smsTime", smsTime);
        smsIntent.putExtra("smsType", "Fake");
        getContext().sendBroadcast(smsIntent);
    }

    private void updateTimer()
    {
        Logger.v(FakeSmsProbe.class, "Start Timer");
        timer = new Timer();
        Random rand = new Random();
        final int delay = 4 * 60 * 60 * 1000 + Math.abs(rand.nextInt() / 500);

        refresher = new TimerTask()
        {
            public void run()
            {
                Logger.v(FakeSmsProbe.class, "Runnable Task");
                long smsTime = System.currentTimeMillis();
                sendSMS(smsTime);
                updateTimer();
            }
        };

        // first event immediately,  following after 'delay' seconds each
        timer.schedule(refresher, delay);
    }

    @Override
    protected void secureOnEnable()
    {
        super.secureOnEnable();

        Logger.v(FakeSmsProbe.class, "Enabled");

        updateTimer();
    }

    @Override
    protected void secureOnDisable()
    {
        super.secureOnDisable();

        Logger.v(FakeSmsProbe.class, "Disable");

        if (timer != null)
        {
            timer.cancel();
            timer = null;

            Logger.v(FakeSmsProbe.class, "Cancel Timer");
        }
        if (refresher != null)
        {
            refresher.cancel();
            refresher = null;

            Logger.v(FakeSmsProbe.class, "Cancel Task");
        }
    }

    @Override
    protected boolean isWakeLockedWhileRunning()
    {
        return false;
    }

}

