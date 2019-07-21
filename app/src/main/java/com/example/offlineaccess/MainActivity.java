package com.example.offlineaccess;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.SEND_SMS;

public class MainActivity extends AppCompatActivity {
    GPSTracker gps;
    double latitude;
    double longitude;
    private static final int REQUEST_SMS = 0;
    private static final int REQ_PICK_CONTACT = 2;
    public static String phn;
    Button mButton;
    EditText mEdit;
    public static final String OTP_REGEX = "[0-9]{1,6}";
    String keyword = null;
    private Button sendButton;

    private Context context;

    private static final int ADMIN_INTENT = 15;
    private static final String description = "ADMIN";
    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;

    public void startService(View view) {
        startService(new Intent(getBaseContext(), MyService.class));
    }
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), MyService.class));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_CONTACT) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                Cursor cursor = managedQuery(contactData, null, null, null, null);
                cursor.moveToFirst();

                String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                mEdit.setText(number);
            }
        }
        if (requestCode == ADMIN_INTENT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Registered As Admin", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to register as Admin", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);

        context = getApplicationContext();

        sendButton = findViewById(R.id.send_button);

        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, MyAdminReceiver.class);
        mButton = findViewById(R.id.button1);
        mEdit = findViewById(R.id.editText1);

        mButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        phn = mEdit.getText().toString();
                        Toast.makeText(MainActivity.this, "number" + mEdit.getText(), Toast.LENGTH_SHORT).show();
                    }
                });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int hasSMSPermission = checkSelfPermission(SEND_SMS);
                if (hasSMSPermission != PackageManager.PERMISSION_GRANTED) {
                    if (!shouldShowRequestPermissionRationale(SEND_SMS)) {
                        showMessageOKCancel("You need to allow access to Send SMS",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(new String[]{SEND_SMS},
                                                    REQUEST_SMS);
                                        }
                                    }
                                });
                        return;
                    }
                    requestPermissions(new String[]{SEND_SMS},
                            REQUEST_SMS);
                    return;
                }
                sendMySMS();
            }
                gps = new GPSTracker(MainActivity.this);

                if (gps.canGetLocation()) {

                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();

                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                } else {
                    gps.showSettingsAlert();
                }
            }
        });

        SmsReceiver.bindListener(new SmsListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void messageReceived(String messageText) {
                AudioManager am;
                am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);

                Log.e("Message", messageText);
                Toast.makeText(MainActivity.this, "Message: " + messageText, Toast.LENGTH_LONG).show();

                Pattern pattern = Pattern.compile(OTP_REGEX);
                Matcher matcher = pattern.matcher(messageText);
                while (matcher.find()) {
                    keyword = matcher.group();
                }

                if (keyword.equals("703130")) {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, description);
                    startActivityForResult(intent, ADMIN_INTENT);

                    boolean isAdmin = mDevicePolicyManager.isAdminActive(mComponentName);
                    if (isAdmin) {
                        mDevicePolicyManager.lockNow();
                    } else {
                        Toast.makeText(getApplicationContext(), "Not Registered as admin", Toast.LENGTH_SHORT).show();
                    }
                }
                if (keyword.equals("703111")) {
                    Toast.makeText(MainActivity.this, "WIFI ON", Toast.LENGTH_LONG).show();
                    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    wifi.setWifiEnabled(true);
                }

                if (keyword.equals("703110")) {
                    Toast.makeText(MainActivity.this, "WIFI OFF", Toast.LENGTH_LONG).show();
                    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    wifi.setWifiEnabled(false);
                }
                if (keyword.equals("703120")) {
                    Toast.makeText(MainActivity.this, "PROFILER SELECTED", Toast.LENGTH_LONG).show();
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
                        } catch (Exception e) {
                            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                context.startActivity(intent);
                            }
                        }
                    } else {
                        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
                    }
                }
                if (keyword.equals("703140")) {
                    Toast.makeText(MainActivity.this, "GPS SELECTED", Toast.LENGTH_LONG).show();
                 sendButton.performClick();
                }
            }
        });
    }

    public void sendMySMS() {

        String phone = mEdit.getText().toString();
        String message = "http://maps.google.com/maps?q=loc:" + latitude + "," + longitude;
        if (phone.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Enter a Valid Phone Number", Toast.LENGTH_SHORT).show();
        } else {
            SmsManager sms = SmsManager.getDefault();
            List<String> messages = sms.divideMessage(message);
            for (String msg : messages) {

                PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
                PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);
                sms.sendTextMessage(phone, null, msg, sentIntent, deliveredIntent);

            }
        }
    }

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), SEND_SMS) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{SEND_SMS}, REQUEST_SMS);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private BroadcastReceiver sentStatusReceiver, deliveredStatusReceiver;
    public void onResume() {
        super.onResume();
        if (GPSTracker.isFromSetting){
            finish();
            startActivity(getIntent());
            GPSTracker.isFromSetting=false;
        }
        sentStatusReceiver=new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String s = "Unknown Error";
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        s = "Message Sent Successfully !!";
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        s = "Generic Failure Error";
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        s = "Error : No Service Available";
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        s = "Error : Null PDU";
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        s = "Error : Radio is off";
                        break;
                    default:
                        break;
                }
                Toast.makeText(getBaseContext(),s,Toast.LENGTH_SHORT).show();
            }
        };
        deliveredStatusReceiver=new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String s = "Message Not Delivered";
                switch(getResultCode()) {
                    case Activity.RESULT_OK:
                        s = "Message Delivered Successfully";
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
            }
        };
        registerReceiver(sentStatusReceiver, new IntentFilter("SMS_SENT"));
        registerReceiver(deliveredStatusReceiver, new IntentFilter("SMS_DELIVERED"));
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(sentStatusReceiver);
        unregisterReceiver(deliveredStatusReceiver);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (GPSTracker.isFromSetting){
            finish();
            startActivity(getIntent());
            GPSTracker.isFromSetting=false;
        }
    }
}