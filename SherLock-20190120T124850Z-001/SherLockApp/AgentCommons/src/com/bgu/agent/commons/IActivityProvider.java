package com.bgu.agent.commons;

import android.app.Activity;

/**
 * Created by clint on 1/6/14.
 */
public interface IActivityProvider extends IAppContextProvider {
    public Activity getActivity ();
    public boolean isActive ();
}
