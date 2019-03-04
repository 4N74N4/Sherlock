package com.bgu.agent.sensors;

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.TelephonyManager;
import com.google.gson.JsonObject;

/**
 * Created by shedan on 26/02/2015.
 */
public class TelephonyProbe extends SecureBase {



    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onStart()
    {
        super.onStart();
        TelephonyManager telephony = (TelephonyManager)this.getContext().getSystemService("phone");
        JsonObject data = new JsonObject();
//        data.addProperty("callState", Integer.valueOf(telephony.getCallState()));
        data.addProperty("deviceId", telephony.getDeviceId());
        data.addProperty("deviceSoftwareVersion", telephony.getDeviceSoftwareVersion());
        //data.addProperty("line1Number", this.sensitiveData(telephony.getLine1Number(), new DataNormalizer.PhoneNumberNormalizer()));
        data.addProperty("networkCountryIso", telephony.getNetworkCountryIso());
        data.addProperty("networkOperator", telephony.getNetworkOperator());
        data.addProperty("networkOperatorName", telephony.getNetworkOperatorName());
        data.addProperty("networkType", Integer.valueOf(telephony.getNetworkType()));
        data.addProperty("phoneType", Integer.valueOf(telephony.getPhoneType()));
        data.addProperty("simCountryIso", telephony.getSimCountryIso());
        data.addProperty("simOperator", telephony.getSimOperator());
        data.addProperty("simOperatorName", telephony.getSimOperatorName());
        data.addProperty("simSerialNumber", telephony.getSimSerialNumber());
        data.addProperty("simState", Integer.valueOf(telephony.getSimState()));
        data.addProperty("subscriberId", telephony.getSubscriberId());
//        data.addProperty("voicemailAlphaTag", telephony.getVoiceMailAlphaTag());
//        data.addProperty("voicemailNumber", telephony.getVoiceMailNumber());
        data.addProperty("hassIccCard", Boolean.valueOf(telephony.hasIccCard()));
        this.sendData(data);
        this.stop();
    }
}
