package com.bgu.congeor.sherlockapp;;

/**
 * Created by Simon on 07/06/2015.
 */
public class AcceVals {
    private String _xVal;
    private String _yVal;
    private String _zVal;
    private String _timeStamp;


    public AcceVals(double x, double y, double z, long timestamp) {
        this._timeStamp = String.valueOf(timestamp);
        this._xVal = String.valueOf(x);
        this._yVal = String.valueOf(y);
        this._zVal = String.valueOf(z);
    }

    @Override
    public String toString() {
        return _timeStamp+", "+_xVal+", "+_yVal+", "+_zVal;
    }

    public String get_timeStamp() {
        return _timeStamp;
    }

    public String get_xVal() {
        return _xVal;
    }

    public String get_yVal() {
        return _yVal;
    }

    public String get_zVal() {
        return _zVal;
    }
}
