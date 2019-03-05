package com.bgu.congeor.sherlockapp.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpVersion;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.params.HttpProtocolParams;
import com.bgu.agent.AgentDataService;
import com.bgu.agent.IUserDetails;
import com.bgu.agent.commons.IActivityProvider;
import com.bgu.agent.commons.IAppContextProvider;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.commons.utils.Utils;
import com.bgu.agent.data.manager.DataManager;
import com.bgu.agent.manager.AgentManager;
import com.bgu.agent.sensors.ErrorSensor;
import com.bgu.agent.sensors.manager.SensorManager;
import com.bgu.congeor.Constants;
import com.bgu.congeor.UserDetails;
import com.bgu.congeor.sherlockapp.MainActivity;
import com.bgu.congeor.sherlockapp.data.operations.SetUserDetails;
import com.bgu.congeor.sherlockapp.data.operations.SimpleLogin;
import com.bgu.congeor.sherlockapp.data.operations.UserLogin;
import com.bgu.congeor.sherlockapp.intentservices.CongeorDataService;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

//import com.kristijandraca.backgroundmaillibrary.Utils;

//import com.bgu.congeor.questionire.Questionire;
//import com.bgu.congeor.stats.UserCouponStats;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/2/13
 * Time: 12:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class CongeorManager implements IAppContextProvider, IActivityProvider, Application.ActivityLifecycleCallbacks
{
    Boolean initialized = null;
    String versionCode;
    String versionName;
    String version;
    protected WeakReference<Activity> mainActivity = null;
    protected boolean isActive = true;
    String gcmRegId = "";
    boolean fileListener;
    protected WeakReference<Context> applicationContext;
    protected IUserDetails userDetails;
    protected String androidId = null;
    protected String experimentId;
    boolean appInitialized = false;
    public static CongeorManager instance;
    long lastLoginTimestamp = 0;
    boolean refreshStats = true;

    public static CongeorManager getInstance()
    {
        return instance;
    }

    public static void setInstance(CongeorManager _instance)
    {
        instance = _instance;
    }

    public void setMainActivity(Activity activity)
    {
        if (mainActivity == null || mainActivity.get() != activity)
        {
            mainActivity = new WeakReference<Activity>(activity);
        }
    }

    public void init()
    {

        if (!appInitialized)
        {
            Logger.d(CongeorManager.class, "init started");
            androidId = Settings.Secure.getString(applicationContext.get().getContentResolver(), Settings.Secure.ANDROID_ID);
            try
            {
                fileListener = AgentManager.initInitialConfiguration(getAppContext());
//                userDetails = readUserDetails();
                Configuration privateCommConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_PRIVATE_COMM_FILENAME, false);
                experimentId = privateCommConfiguration.getKeyAsString(Constants.EXPERIMENT_ID);
                version = privateCommConfiguration.getKeyAsString(Constants.VERSION);
                DataManager.getInstance(getAppContext()).registerListener(AgentDataService.getDefaultListener(getAppContext(), privateCommConfiguration));
                PackageInfo pInfo = applicationContext.get().getPackageManager().getPackageInfo(applicationContext.get().getPackageName(), 0);
                versionName = pInfo.versionName;
                versionCode = "" + pInfo.versionCode;
                Intent congeorDataService = new Intent(getAppContext(), CongeorDataService.class);
                applicationContext.get().startService(congeorDataService);
                initialized = true;
                appInitialized = true;
            }
            catch (Exception e)
            {
                Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
                System.exit(0);
            }
        }
    }

    public Boolean getInitialized()
    {
        return initialized;
    }

    public void onRestart()
    {
        isActive = true;
        sendGoogleAnalyticsRequest();
    }

    private void sendGoogleAnalyticsRequest()
    {
        try
        {
            final Configuration publicConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_PUBLIC_COMM_FILENAME, false);
            final String url = publicConfiguration.getKeyAsString("googleUrl");
            if (url == null || url.isEmpty())
            {
                Logger.v(CongeorManager.class, "Google analytics not defined");
                return;
            }
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (url != null && !url.isEmpty())
                        {
                            Logger.v(CongeorManager.class, "Sending request to google analytics");
                            HttpParams params = new BasicHttpParams();
                            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                            HttpConnectionParams.setConnectionTimeout(params, Constants.CONNECTION_TIMEOUT);
                            HttpConnectionParams.setSoTimeout(params, Constants.SOCKET_TIMEOUT);
                            HttpClient client = new DefaultHttpClient(params);
                            HttpUriRequest request = new HttpGet(url);
                            HttpResponse response = client.execute(request);
                            Logger.v(CongeorManager.class, "Response from Google Analytics : " + response.getStatusLine());
                        }
                        else
                        {
                            Logger.v(CongeorManager.class, "Google analytics not defined");
                        }
                    }
                    catch (Throwable t)
                    {
                        Logger.e(CongeorManager.class, t.getLocalizedMessage(), t);
                    }
                }
            }).start();
        }
        catch (Throwable t)
        {
            Logger.e(CongeorManager.class, t.getLocalizedMessage(), t);
        }
    }

    public void onPause()
    {
        isActive = false;
    }

    public UserDetails getCongeorUserDetails()
    {
        return (UserDetails) this.userDetails;
    }

    public void updateUserDetailsInServer(final Runnable r)
    {
        updateUserDetailsInServer(r, 0);
    }

    public void updateUserDetailsInServer(final Runnable r, final int retries)
    {
        if (!isFileListener())
        {
            DataManager.getInstance(getAppContext()).performDataOperation(new SetUserDetails(getCongeorUserDetails()), new IRunnableWithParameters()
            {
                @Override
                public void run(Object... params)
                {
                    if (((Integer) params[1]).intValue() != 0)
                    {
                        new Handler().postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (retries <= 3)
                                {
                                    updateUserDetailsInServer(r, retries + 1);
                                }
                            }
                        }, Constants.UPDATE_RETRY_TIME);
                    }
                    else
                    {
                        if (r != null)
                        {
                            r.run();
                            r.run();
                        }
                    }
                }
            });
        }
    }

    private File getUserDetailsFile()
    {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/" + Utils.getConfigurationFolder(applicationContext.get()));
        return new File(dir, "userDetails");
    }

    public void writeUserDetails()
    {
        try
        {
            OutputStream w = new FileOutputStream(getUserDetailsFile());
            Kryo kryo = new Kryo();
            Output output = new Output(w);
            kryo.writeObject(output, userDetails);
            output.flush();
            output.close();
            w.close();
        }
        catch (FileNotFoundException e)
        {
            Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
        }
        catch (IOException e)
        {
            Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
        }
    }

    public IUserDetails readUserDetails()
    {
        try
        {
            File userDetailsFile = getUserDetailsFile();
//            Added "userDetailsFile.length()<2" to avoid crashes if the file is empty
            if (!userDetailsFile.exists() || userDetailsFile.length()<2)
            {
                return null;
            }
            InputStream r = new FileInputStream(userDetailsFile);
            Kryo kryo = new Kryo();
            Input input = new Input(r);
            userDetails = kryo.readObject(input, UserDetails.class);
            UserDetails us = (UserDetails) userDetails;
            if (us.getPhoneID() == null || us.getPhoneID().isEmpty())
            {
                us.setPhoneID(androidId);
            }
            writeUserDetails();
            input.close();
            r.close();
        }
        catch (FileNotFoundException e)
        {
            Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
            return null;
        }
        catch (IOException e)
        {
            Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
            return null;
        }
        return userDetails;
    }


    public void login(final IRunnableWithParameters runnable)
    {
        Log.e(CongeorManager.class.toString(), "login has been called");
        lastLoginTimestamp = System.currentTimeMillis();
        if (isFileListener())
        {
            if (userDetails == null)
            {
                userDetails = new UserDetails("Danny");
            }
            CongeorManager.getInstance().setUserDetails(userDetails);
            CongeorManager.getInstance().writeUserDetails();
            if (runnable != null)
            {
                runnable.run();
            }
            return;
        }
        else
        {
            if (!Utils.isOnline(getAppContext(), false))
            {
                if (userDetails == null)
                { // Configuration does not exist.
                    runnable.run(Constants.ENUM_LOGIN_STATUS.NO_COMMUNICATION, "Phone offline", Constants.NO_INTERNET_EXCEPTION);
                }
                else
                { // Configuration exists.
                    runnable.run(Constants.ENUM_LOGIN_STATUS.CONFIG_NOT_CHANGED);
                }
                return;
            }
        }
        Integer agentConfigurationHash = null;
        Integer publicCommConfigurationHash = null;
        try
        {
            Configuration agentConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_AGENT_FILENAME, false);
            Configuration publicCommConfiguration = Configuration.loadLocalConfiguration(getAppContext(), Constants.CONF_PUBLIC_COMM_FILENAME, false);
            if (userDetails != null)
            {
                if (agentConfiguration != null)
                {
                    Logger.d(CongeorManager.class, "Login - Agent configuration found on phone.");
                    agentConfigurationHash = agentConfiguration.getConfigurationHash();
                }
                else
                {
                    Logger.d(CongeorManager.class, "Login - Agent configuration was not found on phone.");
                }
                if (publicCommConfiguration != null)
                {
                    Logger.d(CongeorManager.class, "Login - Public configuration found on phone.");
                    publicCommConfigurationHash = publicCommConfiguration.getConfigurationHash();
                }
                else
                {
                    Logger.d(CongeorManager.class, "Login - Public configuration was not found on phone.");
                }
            }
            else
            {
                Logger.d(CongeorManager.class, "Login - Configuration was not found on phone.");
            }
        }
        catch (IOException e)
        {
            Logger.d(CongeorManager.class, "Login - Configuration was not found on phone.");
        }
        final Integer agentConfig = agentConfigurationHash;
        final Integer publicCommConfig = publicCommConfigurationHash;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                DataManager.getInstance(getAppContext()).performDataOperation(new UserLogin(getPhoneId(), experimentId, agentConfig, publicCommConfig,
                        versionCode, versionName, SensorManager.getSensorsModuleVersion()), new IRunnableWithParameters()
                {
                    @Override
                    public void run(Object... params)
                    {
                        if (((Integer) params[1]).intValue() == 0)
                        {
                            Map<String, Object> returnVal = (Map<String, Object>) params[2];
                            loginResult(returnVal, runnable);
                        }
                        else
                        {
                            Constants.ENUM_LOGIN_STATUS loginStatus = Constants.ENUM_LOGIN_STATUS.NO_COMMUNICATION;
                            runnable.run(loginStatus, params[2], params[1]);
                        }
                    }
                });
            }
        });
    }

    private void loginResult(Map<String, Object> returnVal, final IRunnableWithParameters runnable)
    {
        Constants.ENUM_LOGIN_STATUS loginStatus = (Constants.ENUM_LOGIN_STATUS) returnVal.get(Constants.LOGIN_STATUS);
        if (loginStatus == Constants.ENUM_LOGIN_STATUS.VERSION_ERROR)
        {
            runnable.run(loginStatus);
            return;
        }
        if (loginStatus == Constants.ENUM_LOGIN_STATUS.NEW_USER || loginStatus == Constants.ENUM_LOGIN_STATUS.CONFIG_LOST)
        {
            loginStatus = Constants.ENUM_LOGIN_STATUS.NEW_USER;
            runnable.run(loginStatus);
        }
        else
        {
            if (loginStatus != Constants.ENUM_LOGIN_STATUS.NO_COMMUNICATION)
            {
                if (loginStatus == Constants.ENUM_LOGIN_STATUS.CONFIG_CHANGED || loginStatus == Constants.ENUM_LOGIN_STATUS.CONFIG_LOST)
                {
                    if (returnVal.containsKey(Constants.CONF_AGENT))
                    {
                        try
                        {
                            Configuration agentConfiguration = Configuration.loadConfiguration(false, Constants.CONF_AGENT_FILENAME,
                                    returnVal.get(Constants.CONF_AGENT).toString());
                            AgentManager.storeConfiguration(getAppContext(), agentConfiguration);
                            Logger.d(CongeorManager.class, "Login - Received new Agent configuration from server: " + agentConfiguration.toString());
                            AgentManager.agentConfigurationChanged(getAppContext());
                        }
                        catch (Throwable e)
                        {
                            Intent error = ErrorSensor.generateErrorIntent(CongeorManager.class, e);
                            getAppContext().sendBroadcast(error);
                            Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
                        }
                    }
                    else
                    {
                        Logger.d(CongeorManager.class, "Login - There was no change in Agent configuration.");
                    }
                    if (returnVal.containsKey(Constants.CONF_PUBLIC_COMM))
                    {
                        try
                        {
                            Configuration publicCommConfiguration = Configuration.loadConfiguration(false, Constants.CONF_PUBLIC_COMM_FILENAME,
                                    returnVal.get(Constants.CONF_PUBLIC_COMM).toString());
                            AgentManager.storeConfiguration(getAppContext(), publicCommConfiguration);
                            Logger.d(CongeorManager.class, "Login - Received new Public configuration from server: " + publicCommConfiguration.toString());
                            AgentManager.publicCommConfigurationChanged(getAppContext());
                        }
                        catch (Throwable e)
                        {
                            Intent error = ErrorSensor.generateErrorIntent(CongeorManager.class, e);
                            getAppContext().sendBroadcast(error);
                            Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
                        }
                    }
                    else
                    {
                        Logger.d(CongeorManager.class, "Login - There was no change in public configuration.");
                    }
                    if (loginStatus == Constants.ENUM_LOGIN_STATUS.CONFIG_LOST)
                    {
                        Logger.d(CongeorManager.class, "Login - Received user details from server.");
                        boolean gcmChanged = setUserDetails((UserDetails) returnVal.get(Constants.USER_DETAILS));
                        writeUserDetails();
                        if (gcmChanged)
                        {
                            updateUserDetailsInServer(null);
                        }
                    }
                }
            }
            runnable.run(loginStatus);
        }
    }


    private void registerUserResult(UserDetails userDetails, Map<String, Object> returnVal, final IRunnableWithParameters runnable)
    {
        final Constants.ENUM_REGISTER_STATUS register_status = (Constants.ENUM_REGISTER_STATUS) returnVal.get(Constants.REGISTRATION_STATUS);
        if (register_status == Constants.ENUM_REGISTER_STATUS.USER_EXISTS_WITH_DIFF_PHONE_ID)
        {
            runnable.run(register_status);
        }
        else if (register_status == Constants.ENUM_REGISTER_STATUS.USER_NOT_IN_EXP)
        {
            runnable.run(register_status);
        }
        else if (register_status == Constants.ENUM_REGISTER_STATUS.REGISTRATION_FAILED)
        {
            runnable.run(register_status);
        }
        else
        {
            if (returnVal.containsKey(Constants.CONF_AGENT))
            {
                try
                {
                    Configuration agentConfiguration = Configuration.loadConfiguration(false, Constants.CONF_AGENT_FILENAME,
                            returnVal.get(Constants.CONF_AGENT).toString());
                    AgentManager.storeConfiguration(getAppContext(), agentConfiguration);
                    AgentManager.agentConfigurationChanged(getAppContext());
                    Logger.d(CongeorManager.class, "Registration - New agent configuration received from server " + agentConfiguration.toString());
                }
                catch (IOException e)
                {
                    Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
                }
            }
            else
            {
                Logger.d(CongeorManager.class, "Registration - The server and client have identical agent configuration, no change");
            }
            if (returnVal.containsKey(Constants.CONF_PUBLIC_COMM))
            {
                try
                {
                    Configuration publicCommConfiguration = Configuration.loadConfiguration(false, Constants.CONF_PUBLIC_COMM_FILENAME,
                            returnVal.get(Constants.CONF_PUBLIC_COMM).toString());
                    AgentManager.storeConfiguration(getAppContext(), publicCommConfiguration);
                    AgentManager.publicCommConfigurationChanged(getAppContext());
                    Logger.d(CongeorManager.class, "Registration - New public configuration received from server " + publicCommConfiguration.toString());
                }
                catch (IOException e)
                {
                    Logger.e(CongeorManager.class, e.getLocalizedMessage(), e);
                }
            }
            else
            {
                Logger.d(CongeorManager.class, "Registration - The server and client have identical public configuration, no change");
            }
            setUserDetails(userDetails);
            writeUserDetails();
            runnable.run(register_status);
        }
    }

    @Override
    public Activity getActivity()
    {
        return mainActivity.get();
    }

    @Override
    public boolean isActive()
    {
        return isActive;
    }

    public void deleteUserDetails()
    {
        File userDetailsFile = getUserDetailsFile();
        if (userDetailsFile.exists())
        {
            userDetailsFile.delete();
        }
    }


    public void assignUserToMessagingProviders(IUserDetails userDetails)
    {
        // TODO: Improve code for more providers
        if (userDetails != null)
        {
            userDetails.setGcmRegId(gcmRegId);
        }
    }

    public boolean setUserDetails(IUserDetails userDetails)
    {
        boolean gcmChanged = false;
        this.userDetails = userDetails;
        if (userDetails != null)
        {
            UserDetails oldUserDetails = (UserDetails) userDetails;
            if (gcmRegId != null && !gcmRegId.isEmpty() && !oldUserDetails.getGcmRegId().equalsIgnoreCase(gcmRegId))
            {
                gcmChanged = true;
            }
        }
        assignUserToMessagingProviders(userDetails);
        ((UserDetails) userDetails).setPhoneID(androidId);
        return gcmChanged;
    }

    public void LoginUser(final IRunnableWithParameters runnable , final String userMail)
    {
        String hashedMail=null;
        IRunnableWithParameters irunnable = new IRunnableWithParameters() {
            @Override
            public void run(Object... params) {

                if(params[0]!=null)
                {
                    if( ((String) params[0]).equals(Constants.VALID_HASH) )
                    {

                        JSONObject g = new JSONObject();
                        try {
                            g.put(Constants.HASHED_MAIL,(String)params[1]);
                            Configuration configuration = Configuration.loadConfiguration(false, Constants.CONF_USER_DATA_HASHED ,g.toString());
                            AgentManager.storeConfiguration(getAppContext(), configuration);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                       
                        login(new IRunnableWithParameters() {
                            @Override
                            public void run(Object... params) {
                                AgentManager.startSampling(getAppContext());
                            }
                        });
                    }
                    runnable.run(params[0]);

                }
            }
        };
        byte[] data = userMail.getBytes();

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(data);
        byte[] hash = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<hash.length;i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        hashedMail=hexString.toString();
        SimpleLogin simpleLogin = new SimpleLogin(irunnable , hexString.toString() ,getAppContext());
        simpleLogin.execute();
    }

    private void writeHashToFile(String i) {
        try {
            File file = Configuration.getConfigurationLocation(getAppContext(), Constants.CONF_USER_DATA_HASHED);
            JSONObject g = new JSONObject();
            g.put(Constants.HASHED_MAIL,i);
            FileWriter fileWriter = new FileWriter(file,true);
            fileWriter.write(g.toString());
            fileWriter.flush();
            fileWriter.close();


        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static public boolean isFirstRun(Context appContext){
        boolean firstrun = appContext.getSharedPreferences("PREFERENCE", Context.MODE_PRIVATE).getBoolean("firstrun", true);
        if (firstrun){
            // Save the state
            appContext.getSharedPreferences("PREFERENCE", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstrun", false)
                    .commit();

        }
        return firstrun;
    }

    public boolean isPlayServicesAvailable(Context context)
    {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    public boolean isLocationServicesAvailable(Context context)
    {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsProviderEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkProviderEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gpsProviderEnabled && networkProviderEnabled;
    }

    public boolean isFileListener()
    {
        return fileListener;
    }

    public String getPhoneId()
    {
        return androidId;
    }

    public IUserDetails getUserDetails()
    {
        return userDetails;
    }

    public String getUserId()
    {
        return userDetails.getUserId();
    }

    @Override
    public Context getAppContext()
    {
        return applicationContext.get();
    }

    public boolean isInitialized()
    {
        return (appInitialized == true);
    }

    public void destroy()
    {
        DataManager.getInstance(getAppContext()).Dispose(getAppContext());
    }

    public void setApplicationContext(Context appContext)
    {
        this.applicationContext = new WeakReference<Context>(appContext);
    }

    public boolean shouldLogin()
    {
        long timeFromLastLogin = System.currentTimeMillis() - lastLoginTimestamp;
        //if (!initialized || lastLoginTimestamp == 0 || (timeFromLastLogin >= Constants.MAX_TIME_BETWEEN_LOGINS * 1000) || userDetails == null)
        if (!initialized  || userDetails == null)
        {
            return true;
        }
        return false;

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState)
    {

    }

    @Override
    public void onActivityStarted(Activity activity)
    {
    }

    @Override
    public void onActivityResumed(Activity activity)
    {
        isActive = true;
        if (activity.getClass().equals(MainActivity.class))
        {
            CongeorManager.getInstance().setMainActivity(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity)
    {
        isActive = false;
        CongeorManager.getInstance().setMainActivity(null);
    }

    @Override
    public void onActivityStopped(Activity activity)
    {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState)
    {

    }

    @Override
    public void onActivityDestroyed(Activity activity)
    {

    }
}
