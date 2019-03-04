package com.bgu.agent.sensors;

import android.content.Intent;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by Danny on 03/08/2014.
 */

@Probe.DisplayName("GeneralSampleProbeT3")
@Schedule.DefaultSchedule(interval = Constants.SECOND*1)
public class GeneralSampleProbeT3 extends SecureBase {

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        Intent intent = new Intent("com.bgu.T3");
        this.getContext().sendBroadcast(intent);
        Logger.d(this.getClass(),"Sample The most short timed Sensors");
        stop();
    }
}