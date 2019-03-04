package com.bgu.agent.sensors;

import android.content.Intent;
import com.bgu.agent.commons.logging.Logger;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;

/**
 * Created by simondzn on 22/10/2015.
 */

@Probe.DisplayName("GeneralAllMotionProbe")
@Schedule.DefaultSchedule(interval = 5, duration= 0 ,strict = true)
public class GeneralAllMotionProbe extends SecureBase {


            @Override
            protected void secureOnStart()
            {
                super.secureOnStart();
                Intent intent = new Intent("com.bgu.Motion");

                this.getContext().sendBroadcast(intent);
                Logger.d(this.getClass(), "Sample The longest Sensors  ");
                stop();
            }
        }
