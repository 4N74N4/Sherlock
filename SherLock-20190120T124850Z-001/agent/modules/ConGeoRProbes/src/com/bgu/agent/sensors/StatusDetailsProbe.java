package com.bgu.agent.sensors;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;
import com.google.gson.JsonObject;

/**
 * Created by simondzn on 08/10/2015.
 */
public class StatusDetailsProbe extends SecureBase {
    Integer curBrightnessValue;
    int brightnessMode;
    JsonObject data;
    int configOrientation;

    @Override
    protected void secureOnStart() {
        super.secureOnStart();
        JsonObject data = new JsonObject();
        String brightPath = "/sys";
        int brightness;

        try {
            curBrightnessValue = android.provider.Settings.System.getInt(
                    getContext().getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            brightnessMode = android.provider.Settings.System.getInt(
                    getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            data.addProperty("BrightnessMode", "Auto");
        else data.addProperty("BrightnessMode", "Manual");
        data.addProperty("Brightness_settings", curBrightnessValue);
        brightness = TrafficStatsProbe.readIntegerFile("/sys/class/backlight/panel/brightness");
        data.addProperty("Brightness_file", brightness);
        configOrientation = getContext().getResources().getConfiguration().orientation;
        Log.d(StatusDetailsProbe.class.toString(), "orientation: " + String.valueOf(configOrientation));
        if (configOrientation == 1) {
            data.addProperty("Orientation", "Portrait");
        } else {
            data.addProperty("Orientation", "Landscape");
        }
        AudioManager audio;
        Activity activity;

        int a;
        audio = (AudioManager) this.getContext().getSystemService(Context.AUDIO_SERVICE);
        a = audio.getRingerMode();
        if (a == AudioManager.RINGER_MODE_SILENT)
            data.addProperty("RingerMode", "Silent");
        else if (a == AudioManager.RINGER_MODE_VIBRATE)
            data.addProperty("RingerMode", "Vibrate");
        else if (a == AudioManager.RINGER_MODE_NORMAL) ;
        data.addProperty("RingerMode", "Normal");
        a = audio.getStreamVolume(AudioManager.STREAM_RING);
        data.addProperty("RingtoneVol", a);
        a = audio.getStreamVolume(AudioManager.STREAM_ALARM);
        data.addProperty("AlarmVol", a);
        a = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        data.addProperty("MusicVol", a);
        a = audio.getStreamVolume(AudioManager.STREAM_DTMF);
        data.addProperty("DtmfVol", a);
        a = audio.getStreamVolume(AudioManager.STREAM_SYSTEM);
        data.addProperty("SystemVol", a);
        a = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        data.addProperty("NotificationVol", a);
        a = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        data.addProperty("VoiceCallVol", a);
        sendData(data);
    }

}
