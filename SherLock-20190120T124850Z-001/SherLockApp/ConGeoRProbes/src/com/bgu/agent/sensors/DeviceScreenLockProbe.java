package com.bgu.agent.sensors;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.os.Environment;
import android.provider.Settings;
import com.bgu.congeor.Constants;
import com.google.gson.JsonObject;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.DisplayName;

import java.io.File;

/**
 * Created by BittonRon on 2/20/14.
 */

// com.bgu.agent.sensors.DeviceScreenLockProbe

@DisplayName("DeviceScreenLockProbe")
@Schedule.DefaultSchedule(interval = Constants.DAY)
public class DeviceScreenLockProbe extends SecureBase
{

    private final static String PASSWORD_TYPE_KEY = "lockscreen.password_type";

    /**
     * This constant means that android using some unlock method not described here.
     * Possible new methods would be added in the future releases.
     */
    public final static String SOMETHING_ELSE = "Else";

    /**
     * Android using "None" or "Slide" unlock method. It seems there is no way to determine which method exactly used.
     * In both cases you'll get "PASSWORD_QUALITY_SOMETHING" and "LOCK_PATTERN_ENABLED" == 0.
     */
    public final static String NONE_OR_SLIDER = "NoneOrSlider";

    /**
     * Android using "Face Unlock" with "Pattern" as additional unlock method. Android don't allow you to select
     * "Face Unlock" without additional unlock method.
     */
    public final static String FACE_WITH_PATTERN = "FaceWithPattern";

    /**
     * Android using "Face Unlock" with "PIN" as additional unlock method. Android don't allow you to select
     * "Face Unlock" without additional unlock method.
     */
    public final static String FACE_WITH_PIN = "FaceWithPin";

    /**
     * Android using "Face Unlock" with some additional unlock method not described here.
     * Possible new methods would be added in the future releases. Values from 5 to 8 reserved for this situation.
     */
    public final static String FACE_WITH_SOMETHING_ELSE = "FaceWithElse";

    /**
     * Android using "Pattern" unlock method.
     */
    public final static String PATTERN = "Pattern";

    /**
     * Android using "PIN" unlock method.
     */
    public final static String PIN = "Pin";

    /**
     * Android using "Password" unlock method with password containing only letters.
     */
    public final static String PASSWORD_ALPHABETIC = "PasswordAlphabetic";

    /**
     * Android using "Password" unlock method with password containing both letters and numbers.
     */
    public final static String PASSWORD_ALPHANUMERIC = "PasswordAlphabetic";

    @Override
    protected void secureOnStart()
    {
        super.secureOnStart();
        JsonObject data = new JsonObject();
        data.addProperty(Constants.SCREEN_LOCK_TYPE, getCurrent(getContext().getContentResolver()));
        sendData(data);
        stop();
    }

    private String getCurrent(ContentResolver contentResolver)
    {
        long mode = android.provider.Settings.Secure.getLong(contentResolver, PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        if (mode == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING)
        {
            if (android.provider.Settings.Secure.getInt(contentResolver, Settings.Secure.LOCK_PATTERN_ENABLED, 0) == 1)
            {
                return PATTERN;
            }
            else
            {
                return NONE_OR_SLIDER;
            }
        }
        else if (mode == DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK)
        {
            String dataDirPath = Environment.getDataDirectory().getAbsolutePath();
            if (nonEmptyFileExists(dataDirPath + "/system/gesture.key"))
            {
                return FACE_WITH_PATTERN;
            }
            else if (nonEmptyFileExists(dataDirPath + "/system/password.key"))
            {
                return FACE_WITH_PIN;
            }
            else
            {
                return FACE_WITH_SOMETHING_ELSE;
            }
        }
        else if (mode == DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC)
        {
            return PASSWORD_ALPHANUMERIC;
        }
        else if (mode == DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC)
        {
            return PASSWORD_ALPHABETIC;
        }
        else if (mode == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
        {
            return PIN;
        }
        else
        {
            return SOMETHING_ELSE;
        }
    }

    private static boolean nonEmptyFileExists(String filename)
    {
        File file = new File(filename);
        return file.exists() && file.length() > 0;
    }

}
