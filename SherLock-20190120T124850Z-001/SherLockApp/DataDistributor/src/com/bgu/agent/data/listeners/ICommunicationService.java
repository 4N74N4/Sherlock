package com.bgu.agent.data.listeners;

/**
 * Created by clint on 12/31/13.
 */
public interface ICommunicationService {
    public String getServerIP (String service);
    public int getServerPort (String service);
    public String getBaseURL (String service);
}
