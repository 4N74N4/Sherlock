package com.bgu.agent.sensors.datalayer;

/**
 * Created by shedan on 17/09/2014.
 */
public class BandwidthData {
    public long RxBytes, RxPackets, TxBytes, TxPackets;

    public BandwidthData() {
        RxBytes = -1;
        RxPackets =-1;
        TxBytes =-1;
        TxPackets = -1;
    }
    public BandwidthData(Long[] data)
    {
        TxBytes = data[0];
        TxPackets = data[1];
        RxBytes = data[2];
        RxPackets = data[3];

    }
    public Long[] toArray()
    {
        Long[] res = new Long[]{TxBytes ,TxPackets , RxBytes ,RxPackets};
        return res;
    }
}
