package com.bgu.agent.sensors;

import android.content.Intent;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.congeor.Constants;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by Danny on 03/08/2014.
 */

@Probe.DisplayName("GeneralSampleProbeT1")
@Schedule.DefaultSchedule(interval = Constants.MINUTE*2 , duration = 0)
public class GeneralSampleProbeT1 extends SecureBase {

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        Intent intent = new Intent("com.bgu.T1");

        this.getContext().sendBroadcast(intent);
        Logger.d(this.getClass(),"Sample The longest Sensors  ");
        stop();
    }
}