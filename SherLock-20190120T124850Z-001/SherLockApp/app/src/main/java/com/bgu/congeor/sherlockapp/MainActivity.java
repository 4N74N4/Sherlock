package com.bgu.congeor.sherlockapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.*;
import android.widget.*;
import com.bgu.agent.commons.config.Configuration;
import com.bgu.congeor.Constants;
import com.bgu.congeor.sherlockapp.intentservices.SendToServerService2;
import com.bgu.congeor.sherlockapp.manager.CongeorManager;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.kristijandraca.backgroundmaillibrary.BackgroundMail;
import edu.mit.media.funf.time.TimeUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

//import com.bgu.congeor.sherlockapp.intentservices.SendToServerService3;

//public class MainActivity extends BaseFragmentedActivity implements ActionBar.TabListener
public class MainActivity extends Activity {

    boolean finishing = false;
    Button sendButton;
    Switch uploadSwitch;
    Button restartButton;

    private String[] instructions = {"על האפליקציה לרוץ בכל זמן", "יש לוודא כי בכל עת מופיע אייקון האפליקציה בשורת ההודעות", "על הwifi & gps להיות דלוקים כל הזמן", "במידה והאפליקציה קורסת יש לוודא כי היא חוזרת לעבוד (הופעת האייקון בשורת ההודעות) או להפעילה ידנית לאחר אתחול המכשיר"};

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeUtil.secondsOffset = BigDecimal.valueOf(0);
        long currentNanos = System.nanoTime();
        long s = System.currentTimeMillis();

        if (CongeorManager.getInstance().shouldLogin()) {
            finishing = true;
            finish();
            Intent intent = new Intent(MainActivity.this.getApplicationContext(), WelcomeScreenTry.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(Constants.FAST_LOGIN, "TRUE");
            startActivity(intent);
        } else {
            CongeorManager.getInstance().setMainActivity(this);
            setLayout(savedInstanceState);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void initCoupons() {
    }

    @Override
    protected void onResume() {


        super.onResume();
        if (!finishing) {
            CongeorManager.getInstance().onRestart();
            if (CongeorManager.getInstance().shouldLogin()) {
                finish();
                Intent intent = new Intent(MainActivity.this.getApplicationContext(), WelcomeScreenTry.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(Constants.FAST_LOGIN, "TRUE");
                startActivity(intent);
            } else {
                String experimentId;
                if (CongeorManager.isFirstRun(getApplicationContext())) {
                    Configuration privateCommConfiguration = null;
                    Configuration hashedUserData = null;
                    try {
                        hashedUserData = Configuration.loadLocalConfiguration(getApplicationContext(), Constants.CONF_USER_DATA_HASHED, false);
                        privateCommConfiguration = Configuration.loadLocalConfiguration(getApplicationContext(), Constants.CONF_PRIVATE_COMM_FILENAME, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (hashedUserData != null) {
                        experimentId = hashedUserData.getKeyAsString(Constants.HASHED_MAIL);
                    } else {
                        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
                        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
                        String possibleEmail = null;
                        for (Account account : accounts) {
                            if (emailPattern.matcher(account.name).matches()) {
                                possibleEmail = account.name;
                            }
                        }
                        experimentId = possibleEmail;
                    }
                    String version = privateCommConfiguration.getKeyAsString(Constants.VERSION);
                    BackgroundMail bm = new BackgroundMail(getApplicationContext());
                    final BackgroundMail sendA;
                    bm.setFormSubject("New install: User " + experimentId.substring(0, 9));
                    bm.setFormBody("User " + experimentId + "\n" + "Installed version: " + version);
                    bm.setProcessVisibility(false);
                    sendA = bm;
                    Runnable runnable = new Runnable() {

                        public void run() {
                            sendA.secureSend(sendA);

                        }
                    };
                    Thread thread = new Thread(runnable);
                    thread.start();
                    Log.d(WelcomeScreenTry.class.toString(), "Mail has been sent!");

                    Intent i = new Intent(Intent.ACTION_VIEW );
                    i.setData(Uri.parse("https://play.google.com/apps/testing/com.bgu.sherlock.Moriarty"));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setPackage("com.android.chrome");
                    startActivity(i);
                    finish();
                }
            }
        }
    }

    protected void firstLogin() {

    }

    @Override
    protected void onPause() {

        CongeorManager.getInstance().onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
//            context.getPackageManager().getPackageInfo(packageName,0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    public void setLayout(Bundle savedInstanceState) {
        setContentView(R.layout.instructions);
        ListView instList = (ListView) findViewById(R.id.instructionList);
        instList.setAdapter(new InstructionListAdapter(this, R.id.instructionList, instructions));
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });
        uploadSwitch = (Switch) findViewById(R.id.autoupload);
        uploadSwitch.setChecked(false);
        if(!isAppInstalled(getApplicationContext(),"com.bgu.sherlock.Moriarty")){
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            Button moriarty = new Button(this);
            moriarty.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
            moriarty.setText("Install Moriarty");
            moriarty.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(300,0,0,100);
            row.addView(moriarty);
            LinearLayout layout = (LinearLayout) findViewById(R.id.root_layout);
            layout.addView(row);
            moriarty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW );
                    i.setData(Uri.parse("https://play.google.com/apps/testing/com.bgu.sherlock.Moriarty"));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setPackage("com.android.chrome");
                    startActivity(i);
                }
            });
        }
//        restartButton = (Button) findViewById(R.id.ServiceReset);
//        restartButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Intent i = new Intent(getBaseContext(), CongeorDataService.class);
////                getBaseContext().stopService(i);
////                getBaseContext().startService(i);
//                Intent intent = new Intent();
//                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
//                intent.setAction("com.bgu.congeor.sherlockapp.RestartFunfReciver");
//                sendBroadcast(intent);
//            }
//        });
        uploadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                if (isChecked) {
                    editor.putBoolean(Constants.WIFI_TOGGLE, true);

                } else {
                    editor.putBoolean(Constants.WIFI_TOGGLE, false);
                }
                editor.apply();
            }
        });
//
    }

    protected void send() {
        Intent itnt = new Intent(MainActivity.this.getApplicationContext(), SendToServerService2.class);
        String timeToSend = "18:00";
        itnt.putExtra("send_time", timeToSend);
        itnt.putExtra("wifiTries", 4);
        itnt.putExtra("force_upload", true);
        Toast.makeText(MainActivity.this.getApplicationContext(), "Request has been accepted", Toast.LENGTH_LONG).show();
        sendBroadcast(itnt);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    private class InstructionListAdapter extends ArrayAdapter<String> {

        private Context context;
        String[] data;

        public InstructionListAdapter(Context context, int textViewResourceId, String[] data) {
            super(context, textViewResourceId, data);
            this.data = data;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView version = (TextView) findViewById(R.id.versionView);
            String ver;
            try {
                Configuration conf = Configuration.loadLocalConfiguration(context, Constants.CONF_PRIVATE_COMM_FILENAME, false);
                ver = conf.getKeyAsString("version");
                version.setText("Version: " + ver);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.instruction_item, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.instText)).setText(this.data[position]);
            return convertView;
        }

    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You didn't sent data for more then 3 days.\nPlease connect to wifi and do a force upload.").setCancelable(
                false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
//                        unregisterReceiver(reciever);
                    }
                }).setNegativeButton("Remained me later",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                        finishAffinity();
                    }
                }).setIcon(R.drawable.sherlock_icon).setTitle("Sherlock");
        AlertDialog alert = builder.create();
        alert.show();
    }

//    private void updateAndroidSecurityProvider(Activity callingActivity) {
//        try {
//            ProviderInstaller.installIfNeeded(this);
//        } catch (GooglePlayServicesRepairableException e) {
//            // Thrown when Google Play Services is not installed, up-to-date, or enabled
//            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
//            GooglePlayServicesUtil.getErrorDialog(e.getConnectionStatusCode(), callingActivity, 0);
//        } catch (GooglePlayServicesNotAvailableException e) {
//            Log.e("SecurityException", "Google Play Services not available.");
//        }
//    }

}