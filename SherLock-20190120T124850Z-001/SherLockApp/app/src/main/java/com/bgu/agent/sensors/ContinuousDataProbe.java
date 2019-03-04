package com.bgu.agent.sensors;

import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.time.TimeUtil;

import java.math.BigDecimal;

/**
 * Created by clint on 3/17/14.
 */
public class ContinuousDataProbe extends SecureBase implements Probe.ContinuousProbe
{

    Runnable endOfTimeRunnable;

    @Configurable
    protected BigDecimal maxWaitTime = BigDecimal.valueOf(DEFAULT_DURATION - 2);

    protected void secureOnStart()
    {
        super.secureOnStart();
        endOfTimeRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    endOfTimeSendData();
                }
                catch (Throwable t)
                {
                    sendErrorLog(t);
                }
                endOfTimeRunnable = null;
                if (getState() == State.RUNNING)
                {
                    stop();
                }
                if (getState() == State.ENABLED)
                {
                    disable();
                }
            }
        };
        getHandler().postDelayed(endOfTimeRunnable, TimeUtil.secondsToMillis(maxWaitTime));
    }

    protected void endOfTimeSendData()
    {

    }

    protected void secureOnStop()
    {
        super.secureOnStop();
        try
        {
            if (endOfTimeRunnable != null)
            {
                getHandler().removeCallbacks(endOfTimeRunnable);
                endOfTimeRunnable = null;
            }
        }
        catch (Throwable t)
        {

        }
    }
}
