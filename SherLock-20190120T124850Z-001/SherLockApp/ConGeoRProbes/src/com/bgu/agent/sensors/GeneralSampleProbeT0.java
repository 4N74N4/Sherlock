package com.bgu.agent.sensors;

import android.content.Intent;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by Danny on 03/08/2014.
 */

@Probe.DisplayName("GeneralSampleProbeT0")
@Schedule.DefaultSchedule(interval = Constants.MINUTE*60*24 , duration = 0)
public class GeneralSampleProbeT0 extends SecureBase {

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        Intent intent = new Intent("com.bgu.T0");

        this.getContext().sendBroadcast(intent);
        Logger.d(this.getClass(),"Sample The longest Sensors  ");
        stop();
    }
}