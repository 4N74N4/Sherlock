/**
 * Created by IntelliJ IDEA.
 * User: Aviram Dayan
 * Date: 29/04/13
 * Time: 18:36
 *
 *<a href=mailto:avdayan@cs.bgu.ac.il>avdayan@cs.bgu.ac.il</a>
 */
package com.bgu.congeor;

import java.util.Arrays;
import java.util.List;

/**
 * A set of REST-related constants. The paths, requests-paths and parameters names are set here.
 *
 * @author Aviram Dayan
 */
public interface Constants {

    // Configuration dbConstants
    public static final String CONF_SEC_FUNF_SENSORS = "funfSensors";

    // Configuration FileNames
    public static final String CONF_AGENT_FILENAME          = "AgentConfig.json";
    public static final String CONF_PRIVATE_COMM_FILENAME   = "PrivateCommunicationConfig.json";
    public static final String CONF_PUBLIC_COMM_FILENAME    = "PublicCommunicationConfig.json";
    public static final String CONF_USER_DATA_HASHED        = "userMailHashed.json";
	public static final String MODEL_BUILDER_CONFIGURATION = "C:\\Users\\michael\\Documents\\Java Projects\\LocationPredication\\configuration\\ModelBuilder.properties";



	// Communication dbConstants
    public static final String APP_VERSION                  ="appVersion";
    public static final long UPDATE_RETRY_TIME              = 10000;
    public static final String LOGIN_STATUS                 = "loginStatus";
    public static final String REGISTRATION_STATUS          = "registrationStatus";
    public static final String INFORMATION_FOR_USER         = "informationForUser";
    public static String       SEND_DATA_STATUS             = "sendDataStatus";
    public static final String LANG_ENGLISH                 = "english";
    public static final String LANG_HEBREW                  = "hebrew";
    public static final String VALID_HASH                   = "hash is valid";
    public static final String INVALID_HASH                 = "hash is invalid";
    public static final String SERVER_ERROR                 = "error";
    public static final String LOGIN_HEADER                 = "hashedData";
    public static final String CONNECTION_ERROR             = "no network";
    String REQ_GREETING = "greeting";
    String APP_SERVER_COUPONS_SERVLET_URI = "/AppServer/CouponsServlet/";
    public static final String COUPONS_PACKAGE              = "com.bgu.congeor.couponspouch";
    public static final String DYNAMIC_LAYOUT               = "com.bgu.congeor.dynamiclayout";
    public static final String USER_STATS_LAYOUT            = "com.bgu.congeor.userstats";
    public static final String QUESTIONIRE                  = "com.bgu.congeor.dynamiclayout.questionire";
    String CURRENCY_SIGN = " ₪";
    public static final String USED_COUPONS_FETCHED_GUI     = "USED_COUPONS_FETCHED_GUI";
    public static final String USER_MESSAGES_FETCHED_GUI    = "USER_MESSAGES_FETCHED_GUI";
    String REQ_GET_CONSUMED_COUPONS                         = "getConsumedCoupons";
    public static final String ERROR_MESSAGE                = "ERROR_MESSAGE";
    public static final String FAST_LOGIN                   = "FAST_LOGIN";
    public static final String MANUAL_SEND_DATA             = "MANUAL_SEND_DATA";
    public static final String FUNF_KEEP_ALIVE_ACTION       = "FUNF_KEEP_ALIVE_ACTION";
    public static final String FUNF_AGENT_PACKAGE           = "com.bgu.agent.funf";
    public static final String COUPON_STATUS_DISABLED       = "DISABLED";
    public static final String COUPON_SET_STATUS_SUCCESS = "SUCCESS";
    public static final String COUPON_SET_STATUS_FAIL = "FAIL";
    public static final String REQ_REGISTER_USER_ENC        = "registerUserEnc";
    public static final String ENC_SYM_KEY_REQ_ARG_NAME     = "symKey"   ;
    public static final String PHONE_ID_ENCRYPTED           = "phoneIDEnc"    ;
    public static final String CONFIG_FOLDER_PATH           = "/usr/share/Congeor/";
    public static final String ENC_REQ_UPDATE_USER_DETAILS  = "encUpdateUserDetails";
    public static String REQ_INSERT_ZIPPED_SER_SENSORS_DATA_ENC           = "insertZippedSerSensorsDataEnc";
    public static final String USER_COUPON_STATS_DAILY      = "DAILY";
    public static final String USER_COUPON_STATS_WEEKLY     = "WEEKLY";
    public static final String USER_COUPON_STATS_MONTHLY    = "MONTHLY";
    public static final String USER_COUPON_STATS_BEGIN      = "BEGIN";
    public static final String USERS_STATS_FETCHED_GUI      = "USERS_STATS_FETCHED_GUI";
    public        final String GET_COUPON_STATS_REQ         = "getCouponStats";
    public static final String GET_POIS_REQ                 = "getPOIs";
    public static final String QUESTION_ID                  = "questID";
    public static final String ALG_ITERATION                = "algIteration";
    public static final String ANSWER                       = "answer";
    public static final String SEND_ANSWER_TO_QUESTION      = "sendAnswer";
    public static final String QUESTIONS                    = "questions";
    public static final String QUESTIONS_TITLE              = "title"   ;
    public static final String QUESTIONS_LANGUAGE           = "language";
    public static final String SERVER_SOURCE                = "server" ;
    public static final String LOCATION_SERVICE_ARG         = "location";
    public static final String GET_CLOSE_POIS_REQ           = "getClosePOIs";
    public static final String LONGITUDE_ARG                = "longitude";
    public static final String LATITUDE_ARG                 = "latitude";
    public static final String UUID                         = "UUID";
    public static final String TAKE_AWAY_USAGE_QUERY_PARAM  = "takeAway";
    public static final String REQ_CANCEL_USED_COUPON       = "cancelUsedCoupon";
    public static final String PLACE_ID_FIELD               = "placeID";
    public static final String HAS_COUPON                   = "hasCoupon";
    public static final String STATS_REPORT_TYPE            = "statsReportType";
    public static final String REPORT_TYPE                  = "reportType";
    public static final String FACEBOOK_BUTTON              = "facebook";
    public static final String CONGEOR_EMAIL                = "bgu.congeor@gmail.com";


    public static enum ENUM_REGISTER_STATUS                 {USER_EXISTS_WITH_DIFF_PHONE_ID, USER_REGISTERED_SUCCESSFULLY , REGISTRATION_FAILED, USER_NOT_IN_EXP};
    public static enum ENUM_LOGIN_STATUS                    {CONFIG_CHANGED, CONFIG_NOT_CHANGED, CONFIG_LOST, NEW_USER, NO_COMMUNICATION, VERSION_ERROR};
    public static enum ENUM_SEND_DATA_STATUS                {FAILED, CONFIG_CHANGED, CONFIG_NOT_CHANGED, VERSION_ERROR};
    public static final String STATUS                       = "STATUS";
    public static final String REQ_REGISTER_USER            = "registerUser";
    public static final String REQ_LOGIN                    = "login";
    public static final String USER_DETAILS                 = "userDetails";
    public static final String EXISTING_BUSINESSES          = "existingBusinesses";
    public static final String REQ_STORE_DATA               = "insertZippedSerSensorsData";
    public static final String REQ_GET_COUPONS              = "getCoupons";
    public static final String REQ_COUPON_SET_STATUS        = "setCouponStatus";
    public static final String REQ_GET_QUESTIONS            = "getQuestions";
    public static final String REQ_UPDATE_USER_DETAILS      = "updateUserDetails";
    public static final String SEND_TO_SERVER_TIME          = "sendToServerTime";
    public static final String CONF_AGENT_HASH              = "publicAgentConfigHash";
    public static final String CONF_AGENT                   = "publicAgentConfig";
    public static final String CONF_PUBLIC_COMM             = "publicCommConfig";
    public static final String CONF_COMM_HASH               = "publicCommConfigHash";
    public static final String PRIV_COMM_HASH               = "privateCommConfigHash";
    public static final String PHONE_ID                     = "phoneId";
    public static final String EXPERIMENT_ID                = "experimentId";
    public static final String VERSION                      = "version";
    public static final String QUERY_PARAM_ACCOUNT_ID       = "aid";
    public static final String QUERY_PARAM_USER             = "usr";
    public static final String QUERY_PARAM_PASSWORD         = "pwd";
    public static final String QUERY_AMOUNT                 = "amt";
    public static final String LAST_SYNC_TIME               = "lastSyncTime";
    public static final String SENSORS_DATA                 = "sensorsData";
    public static final String COMM_SERVER_IP               = "server_ip";
    public static final String COMM_SERVER_PORT             = "server_port";
    public static final String COMM_BASE_URL                = "services_base_url";
    public static final String COMM_OUTPUT                  = "output";
    public static final String COMM_SEND_TO_SERVER_INTERVAL = "sendToServerInterval";
    public static final String USER_ID                      = "userId";
    public static final int MAX_SYNC_TIME                   = 1000 * 60 * 60 * 24;
    public static final String UI_MESSAGE                   = "UI_MESSAGE";
    public static final String SEND_ON_3G                   = "sendOn3G";
    public static final String PHONE_ID_EXISTS              = "phoneIdExists";
    public static final String TIMESTAMP                    = "timestamp";
    public static final String USER_MESSAGE                 = "userMessage";
    public static final String HASHED_MAIL                  = "mail_hashed";
    // Dynamic Dialog constants
    public static final String QUESTIONIRE_TIMESTAMP        = "questionConstructionTimestamp";

    // Error codes
    public static final int ERROR_BUILD_REQUEST = 999;
    public static final int REST_SEND_EXCEPTION = 998;
    public static final int UPDATE_TIMESTAMP_EXCEPTION = 997;
    public static final int GET_TIMESTAMP_EXCEPTION = 996;
    public static final int OPERATION_CREATION_EXCEPTION = 995;
    public static final int THREEG_NO_SYNC = 994;
    public static final int NO_INTERNET_EXCEPTION = 993;


    // Main Layout Parameters
    public static final String[]    TABS_STRINGS_CONGEOR = {"My Coupons", "Used", "Messages", "Statistics", "Distance from Restaurants", "Settings", "Instructions"};
    public static final String[]    TABS_STRINGS_NETMONITOR = {"Instructions"};
    public static final int         MAIN_FRAGMENT_POSITION = 0;
    public static final int         MESSAGES_FRAGMENT_POSITION = 2;
    public static final int         INSTRUCTIONS_FRAGMENT_POSITION = 6;
    public static final int         SETTINGS_FRAGMENT_POSITION = 5;
    public static final int         CLOSEST_RESTS_FRAGMENT_POSITION = 4;



    //accelerometer and gyro statistic constants

    // x = PITCH
    // y = ROLL
    // z = AZIMUTH
    // xy = PITCH_ROLL
    // xz = PITCH_AZIMUTH
    // yz = ROLL_AZIMUTH

    public static final String SUM_PITCH                    = "SumPITCH";
    public static final String SUM_ROLL                     = "SumROLL";
    public static final String SUM_AZIMUTH                  = "SumAZIMUTH";
    public static final String SUM_PITCH_ROLL               = "SumPITCH_ROLL";
    public static final String SUM_PITCH_AZIMUTH            = "SumPITCH_AZIMUTH";
    public static final String SUM_ROLL_AZIMUTH             = "SumROLL_AZIMUTH";

    public static final String AVG_PITCH                    = "AvgPITCH";
    public static final String AVG_ROLL                     = "AvgROLL";
    public static final String AVG_AZIMUTH                  = "AvgAZIMUTH";

    public static final String VAR_PITCH                    = "VarPITCH";
    public static final String VAR_ROLL                     = "VarROLL";
    public static final String VAR_AZIMUTH                  = "VarAZIMUTH";

    public static final String COV_PITCH_ROLL               = "CovPITCH_ROLL";
    public static final String COV_PITCH_AZIMUTH            = "CovPITCH_AZIMUTH";
    public static final String COV_ROLL_AZIMUTH             = "CovROLL_AZIMUTH";

    public static final String R_PITCH_ROLL                 = "R_PITCH_ROLL";
    public static final String R_PITCH_AZIMUTH              = "R_PITCH_AZIMUTH";
    public static final String R_ROLL_AZIMUTH               = "R_ROLL_AZIMUTH";

    public static final String SUM_ABS_PITCH                = "SumAbsPITCH";
    public static final String SUM_ABS_ROLL                 = "SumAbsROLL";
    public static final String SUM_ABS_AZIMUTH              = "SumAbsAZIMUTH";
    public static final String SUM_ABS_PITCH_ROLL           = "SumAbsPITCH_ROLL";
    public static final String SUM_ABS_PITCH_AZIMUTH        = "SumAbsPITCH_AZIMUTH";
    public static final String SUM_ABS_ROLL_AZIMUTH         = "SumAbsROLL_AZIMUTH";

    public static final String AVG_ABS_PITCH                = "AvgAbsPITCH";
    public static final String AVG_ABS_ROLL                 = "AvgAbsROLL";
    public static final String AVG_ABS_AZIMUTH              = "AvgAbsAZIMUTH";

    public static final String VAR_ABS_PITCH                = "VarAbsPITCH";
    public static final String VAR_ABS_ROLL                 = "VarAbsROLL";
    public static final String VAR_ABS_AZIMUTH              = "VarAbsAZIMUTH";

    public static final String COV_ABS_PITCH_ROLL           = "CovAbsPITCH_ROLL";
    public static final String COV_ABS_PITCH_AZIMUTH        = "CovAbsPITCH_AZIMUTH";
    public static final String COV_ABS_ROLL_AZIMUTH         = "CovAbsROLL_AZIMUTH";

    public static final String R_ABS_PITCH_ROLL             = "RAbsPITCH_ROLL";
    public static final String R_ABS_PITCH_AZIMUTH          = "RAbsPITCH_AZIMUTH";
    public static final String R_ABS_ROLL_AZIMUTH           = "RAbsROLL_AZIMUTH";

    //accelerometer and gyro statistic constants

    public static final String SUM_X                        = "SumX";
    public static final String SUM_Y                        = "SumY";
    public static final String SUM_Z                        = "SumZ";
    public static final String SUM_XY                       = "SumXY";
    public static final String SUM_XZ                       = "SumXZ";
    public static final String SUM_YZ                       = "SumYZ";

    public static final String AVG_X                        = "AvgX";
    public static final String AVG_Y                        = "AvgY";
    public static final String AVG_Z                        = "AvgZ";

    public static final String VAR_X                        = "VarX";
    public static final String VAR_Y                        = "VarY";
    public static final String VAR_Z                        = "VarZ";

    public static final String COV_XY                       = "CovXY";
    public static final String COV_XZ                       = "CovXZ";
    public static final String COV_YZ                       = "CovYZ";

    public static final String R_XY                         = "RXY";
    public static final String R_XZ                         = "RXZ";
    public static final String R_YZ                         = "RYZ";

    public static final String SUM_ABS_X                    = "SumAbsX";
    public static final String SUM_ABS_Y                    = "SumAbsY";
    public static final String SUM_ABS_Z                    = "SumAbsZ";
    public static final String SUM_ABS_XY                   = "SumAbsXY";
    public static final String SUM_ABS_XZ                   = "SumAbsXZ";
    public static final String SUM_ABS_YZ                   = "SumAbsYZ";

    public static final String AVG_ABS_X                    = "AvgAbsX";
    public static final String AVG_ABS_Y                    = "AvgAbsY";
    public static final String AVG_ABS_Z                    = "AvgAbsZ";

    public static final String VAR_ABS_X                    = "VarAbsX";
    public static final String VAR_ABS_Y                    = "VarAbsY";
    public static final String VAR_ABS_Z                    = "VarAbsZ";

    public static final String COV_ABS_XY                   = "CovAbsXY";
    public static final String COV_ABS_XZ                   = "CovAbsXZ";
    public static final String COV_ABS_YZ                   = "CovAbsYZ";

    public static final String R_ABS_XY                     = "RAbsXY";
    public static final String R_ABS_XZ                     = "RAbsXZ";
    public static final String R_ABS_YZ                     = "RAbsYZ";

    public static final String START_SAMPLE_TIME            = "sTime";
    public static final String END_SAMPLE_TIME              = "eTime";

    //light sensor statistic
    public static final String SUM_LUX                        = "SumLux";
    public static final String AVG_LUX                        = "AvgLux";
    public static final String VAR_LUX                        = "VarLux";

    // AppChanged dbConstants

    public static final String APP_NAME                     = "App_Name";
    public static final String ACTION                       = "Action";
    public static final String ACTION_AT                    = "actionAt";
    public static final String INTENT_ACTION_APPINFO        = "com.bgu.congeor.sensors.AppInfo";
    public static final String INTENT_ACTION_WIFI           = "com.bgu.congeor.sensors.Wifi";
    public static final String INTENT_ACTION_BLUETOOTH      = "com.bgu.congeor.sensors.Bluetooth";
    public static final String INTENT_ACTION_INCOMING_CALL  = "com.bgu.congeor.sensors.INCOMING_CALL";
    public static final String INTENT_ACTION_OUTGOING_CALL  = "com.bgu.congeor.sensors.OUTGOING_CALL";
    public static final String INTENT_ACTION_END_OF_CALL    = "com.bgu.congeor.sensors.END_OF_CALL";
    public static final String INTENT_ACTION_RINGING        = "com.bgu.congeor.sensors.RINGING";
    public static final String INTENT_ACTION_USER_PRESENT   = "com.bgu.congeor.sensors.UserPresent";

    public static final String INTENT_ACTION_INCOMING_MAIL = "com.bgu.congeor.sensors.INCOMING_MAIL";
    public static final String INTENT_ACTION_OUTGOING_MAIL = "com.bgu.congeor.sensors.OUTGOING_MAIL";

    public static final String CALL_TYPE                    = "CALL_TYPE";
    public static final String CALL_TYPE_INCOMING           = "INCOMING";
    public static final String CALL_TYPE_OUTGOING           = "OUTGOING";
    public static final String CALL_TYPE_RINGING            = "RINGING";
    public static final String CALL_TYPE_ENDED              = "ENDED";

    //ConnectedWifi dbConstants
    public static final String APP_ADDED                    = "Added";
    public static final String APP_UPDATED                  = "Updated";
    public static final String APP_REMOVED                  = "Removed";
    public static final String APP_CHANGED                  = "Changed";
    public static final String SSID                         =  "SSID";
    public static final String BSSID                        =  "BSSID";
    public static final String INSTALL_SOURCE               = "InstallSrc";
    public static final String INSTALL_TIME                 = "InstallTime";
    public static final String PERMISSIONS                  = "Permissions";
    public static final String PACKAGE_HASH                 = "PackageHASH";
    public static final String IP_ADDRESS                   =  "IpAddress";
    public static final String MAC_ADDRESS                  =  "MacAddress";
    public static final String NETWORK_ID                   =  "NetworkID";
    public static final String NETWORK_SEC                  =  "NetworkSecurity";
    public static final String PROC_NAME                    =  "ProcName";
    public static final String PROC_ID                      =  "ProcID";
    public static final String PROC_UID                     =  "ProcUID";
    public static final String APP_PACKAGE                  =  "AppPackage";
    public static final String APP_UID                      =  "AppUID";

    public static final String WIFI                         =  "Wifi";
    public static final String RUNNING_PROC                 =  "RunningProcesses";
    public static final String RUNNING_APPS                 =  "RunningApps";

    //ConnectedBluetooth dbConstants
    public static final String DEVICE_NAME                 =  "DeviceName";
    public static final String DEVICE_ADDRESS              =  "DeviceAddress";
    public static final String DEVICE_ACTION               =  "DeviceAction";
    public static final String DEVICE_CONNECTED            =  "DeviceConnected";
    public static final String DEVICE_DISCONNECTED         =  "DeviceDisconnected";

    public static final String SCREEN_LOCK_TYPE            =  "ScreenLockType";
    //GooglePlayServices Constant

    public static final String LATITUDE                    = "mLatitude";
    public static final String LONGITUDE                   = "mLongitude";
    public static final String ALTITUDE                    = "mAltitude";
    public static final String ACCURACY                    = "mAccuracy";
    public static final String PROVIDER                    = "mProvider";
    public static final String SPEED                       = "Speed";
    public static final String TIME                        = "mTime";

    //Sound Probe
    public static final String L1_NORM                     = "L1_NORM";
    public static final String L2_NORM                     = "L2_NORM";
    public static final String LINF_NORM                   = "LINF_NORM";
    public static final String Vol_LVL                     = "volumeLevel";
    public static final String MFCCS                       = "MFCCS";
    public static final String DIFF_SECS                   = "DIFF_SECS";
    public static final String PSD_ACROSS_FREQUENCY        = "PSD_ACROSS_FREQUENCY_BANDS";


    //ActivityRecognition dbConstants
    public static final String ACTIVITY                     =  "Activity";
    public static final String CONFIDENCE                   =  "Confidence";

    //Rooted Checking
    public static final String IS_ROOTED                    =  "IsRooted";

    public static final String VALUE                        = "value";
    public static final String NOT_AVAILABLE                = "N/A";
    public static final String ERROR                        = "error";


    //Time
    public static final long SECOND                         = 1 ;
    public static final long MINUTE                         = 60 * SECOND ;
    public static final long HOUR                           = 60 * MINUTE ;
    public static final long DAY                            = 24 * HOUR ;
    public static final long WEEK                           = 7  * DAY ;
    public static final long MONTH                          = 4  * WEEK;

    //traffic Table - coulmn names
    public static final String PACKAGE_NAME                 = "PackageName";
    public static final String UID                          = "UID";
    public static final String UPLOAD_RATE_BYTE             = "ByteUploadRate";
    public static final String UPLOAD_RATE_PACKET           = "PacketUploadRate";
    public static final String DOWNLOAD_RATE_BYTE           = "ByteDownloadRate";
    public static final String DOWNLOAD_RATE_PACKET         = "PacketDownloadRate";
    public static final String BANDWIDTH_TABLE              = "TrafficStats";
    public static final String APPS_DATA_DB                 = "AppsDataDB";
    public static final String TEXT_TYPE                    = "TEXT";
    public static final String INT_TYPE                     = "INTEGER";
    /*sharedPrefences FIle for Apps Bandwdith data
    public static final String APPS_BANDWIDTH_DATA          ="AppsBandwidthData";*/

    public static final String Key                          = "key.bks";

    public static final String WIFI_TOGGLE                  = "wifiToggle";





    // Facebook dbConstants
//    public static final List<String> FACEBOOK_USER_PERMS_TO_REQUEST = Arrays.asList("read_stream", "export_stream", "friends_location", "user_checkins", "friends_checkins",
//                                                                                "user_about_me", "user_actions.news", "user_birthday", "user_events", "user_groups", "user_likes", "user_online_presence",
//                                                                                "user_questions", "user_religion_politics", "user_videos", "user_actions.books", "user_actions.video",
//                                                                                "user_friends", "user_hometown", "user_location", "user_photo_video_tags", "user_relationship_details",
//                                                                                "user_status", "user_website", "user_actions.music", "user_activities", "user_education_history",
//                                                                                "user_games_activity", "user_interests", "user_notes", "user_photos", "user_relationships", "user_subscriptions",
//                                                                                "user_work_history", "friends_about_me", "friends_actions.news", "friends_birthday", "friends_events", "friends_hometown",
//                                                                                "friends_location", "friends_photo_video_tags", "friends_relationship_details", "friends_status", "friends_website",
//                                                                                "friends_actions.books", "friends_actions.video", "friends_checkins", "friends_games_activity", "friends_interests",
//                                                                                "friends_notes", "friends_photos", "friends_relationships", "friends_subscriptions", "friends_work_history",
//                                                                                "friends_actions.music", "friends_activities", "friends_education_history", "friends_groups", "friends_likes",
//                                                                                "friends_online_presence", "friends_questions", "friends_religion_politics", "friends_videos",
//                                                                                "read_friendlists", "email", "read_requests");
    public static final List<String> FACEBOOK_USER_PERMS_TO_REQUEST = Arrays.asList("read_stream", "export_stream", "user_groups","user_friends", "friends_groups","read_friendlists","read_requests");
    public static final String FACEBOOK_TOKEN = "FACEBOOK_TOKEN";
    public static final String FACEBOOK_TOKEN_EXPIRE = "FACEBOOK_TOKEN_EXPIRE";
    public static final String RECOMMENDATIONS = "RECOMMENDATIONS";

    public static final int CONNECTION_TIMEOUT = 20000;
    public static final int SOCKET_TIMEOUT = 20000;
    public static final String VERSION_CODE                 = "versionCode";
    public static final String VERSION_NAME                 = "versionName";
    public static final String SENSORS_MODULE_VERSION       = "sensorsModuleVersion";
    public static final String PREVIOUSLY_USED = "history";
    public static final long MAX_TIME_BETWEEN_LOGINS = 1800;
    public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    public static final String EXCLUDED_SENSORS             = "excludedSensors";
    public static final String RESTURANT_DISTANCES_FETCHED_GUI = "RESTURANT_DISTANCES_FETCHED_GUI";
    public static final String RESTURANT_DISTANCES_LAYOUT = "com.bgu.congeor.DistanceFromResturants";
    public static final String USER_MESSAGES_LAYOUT = "com.bgu.congeor.usermessages";
    public static final String RESTAURANT_NAME_FIELD = "restName";
    public static final String RESTAURANT_DISTANCE_FIELD = "restDist";
    public static final String RESTAURANT_COUPON_AVAILABLE_FIELD = "couponAvailable";
    public static final String RESTAURANT_LOCATION          = "location";
    public static final String REPORT_DATE                  = "REPORT_DATE";
    public static final String WEEKS                        = "WEEKS";
    public static final String DAYS                        = "DAYS";    
    public static final String UI_ACT_QUESTIONS_ANS = "questionsAnswered";
    public static final String UI_ACT_QUESTIONS_NOT_ANS = "questionsNotAnswered";
    public static final String UI_ACT_ACTIVITY_RESUMED = "activityResumed";
    public static final String UI_ACT_ACTIVITY_PAUSED = "activityPaused";
    public static final String UI_ACT_ACTIVITY_DESTROYED = "activityDestroyed";
    public static final String UI_ACT_CURRENT_SCREEN = "currentScreen";
    public static final String UI_ACT_NOTIFICATION_SHOWN = "notificationShown";
    public static final String UI_ACT_BUTTON_PRESSED = "buttonPressed";
    public static final String PASSWORD                     = "PASSWORD";
    public static final String FILTER_LEVEL                 = "FILTER_LEVEL";
    public static final String LOCATION_RETRIVED_GUI        = "LOCATION_RETRIVED_GUI";
    public static final String GET_USER_MESSAGES            = "getUserMessages";
    public static final String USER_MESSAGES_SCREEN_TIME    = "UserMessagesScreenTime";
    public static final String REFRESH_BUTTON               = "refreshButton";
    public static final String COUPON_CONSUME_BUTTON        = "couponConsumeButton";
    public static final String COUPON_CANCEL_BUTTON         = "cancelCouponButton";
    public static final String LINK_BUTTON                  = "linkButton";
    public static final String MAPS_BUTTON                  = "mapsButton";
    public static final String WAZE_BUTTON                  = "wazeButton";
    public static final String UI_BUTTON                    = "UI_BUTTON";
    public static final String MINS                         = "MINS";
    public static final String SETTING_SHOW_NOTIFICATIONS   = "showNotifications";
    public static final String NOTIFICATIONS_NO             = "noNotifications";
    public static final String NOTIFICATIONS_SHOW           = "showNotifications";
    public static final String NOTIFICATIONS_FULL           = "fullNotifications";
    public static final String COMM_ARCHIVE_INTERVAL        = "archiveInterval";
    public static final String ACTION_FROM                  = "actionFrom";
    public static final String APPLICATION_NAME             = "SherLock";

}
