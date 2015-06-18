package com.matthijswillems.cervicapp;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    //Boolean that checks if CervicGear is found
    boolean foundCervicGear = false;
    public static boolean sampleCountWasReset = true;
    public boolean sendData = true;

    //Adjust if needed
    int secondsForWarning = 5000;
    int colorAnimationDuration = 5000;
    int alphaIncrement = 1;
    int storeAverageCount = 7200; //7200 samples is 3600seconds - 1 hour
    int storeCurrentSample = 119; //119 is 120 samples, is 1 minuut


    // UUIDs for UAT service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    //Data holders S1, S2, FLX
    public static int s1Value = 0;
    public static int s2Value = 0;
    public static int flxValue = 0;
    public static int s1ValueCalibrated = 0;
    public static int s2ValueCalibrated = 0;
    public static int flxValueCalibrated = 0;
    public static int direction;
    public static int angleS1Difference = 0;
    public static int angleS2Difference = 0;
    public static int flxDifference = 0;
    public static String dataString = "";

    //Preferences
    public static boolean soundEnabled = true;
    public static boolean incomingDataEnabled = false;
    public static int defaultForwardAngleValue = 10;
    public static int defaultBackwardAngleValue = -5;
    public static int defaultForwardHeadPostureValue = 6;

    //Leafs + Bush +Stem
    public static int leafnumber = 1;
    int alphaValue;
    int shakeBushCounter = 0;
    int newYPosition;
    int plant_complete_count = 0;


    //GUI objects
    private TextView messages;
    private ImageButton settings_button;
    private ImageButton calibrate_button;
    private ImageButton overview_button;
    ValueAnimator colorAnimation;


    // Request Codes
    private final static int REQUEST_ENABLE_BT = 1;

    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private BluetoothGattCallback storeCallback;

    //Saving data + logging
    String[] oneMinuteData = new String[120];
    int dataSample = 0;
    String currentDateTimeString;
    public static String calibratedString = ",";
    boolean calibratedBoolean = false;
    //int's are enough for 4085years of logging.
    long millisInGoodPosition;
    String timeInGoodPosition;
    boolean resetAverageFile = false;


    //Averages + Percentages
    //array list .add / .remove
    float avg = 0;
    long sampleCount = 1; //long max value 9,223,372,036,854,775,807
    int currentSampleCount = 1;
    //in my case, calling it each 500ms it will take 1.46e11 years, so no need to worry.
    long currentTime;
    boolean storeCurrentBadTime = true;
    boolean storeCurrentGoodTime = true;

    //sound
    MediaPlayer mediaPlayer;

    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                foundCervicGear = true;
                writeLine("Connected!");
                // Discover services.
                if (!gatt.discoverServices()) {
                    writeLine("Failed to start discovering services!");
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                writeLine("Disconnected!");
                foundCervicGear = false;
            } else {
                writeLine("Connection state changed.  New state: " + newState);
            }
        }

        // Called when services have been discovered on the remote device.
        // It seems to be necessary to wait for this discovery to occur before
        // manipulating any services or characteristics.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            System.out.println("onServicesDiscovered");
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeLine("Service discovery completed!");
            } else {
                writeLine("Service discovery failed with status: " + status);
            }
            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                writeLine("Couldn't set notifications for RX characteristic!");
            }
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    writeLine("Couldn't write RX client descriptor value!");
                }
            } else {
                writeLine("Couldn't get RX client descriptor!");
            }
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            writeLine("Received: " + characteristic.getStringValue(0));
            String receivedData = characteristic.getStringValue(0);
            processIncomingData(receivedData);


        }
    };

    private void processIncomingData(String receivedData) {


        String identifier = receivedData.substring(0, 3);
        String value = receivedData.substring(3, receivedData.length());
        //in case I'm not receiving all values (not both batteries are connected)
        //do nothing
        if (value == null || value.isEmpty()) {
            sendData = false;

        }

        if (sendData) {
            switch (identifier) {
                case "S1=":
                    s1Value = Integer.parseInt(value);
                    break;
                case "S2=":
                    s2Value = Integer.parseInt(value);

                    break;
                case "FLX":
                    flxValue = Integer.parseInt(value);
                    break;

            }
        }


    }

    private void displayData(final int s1Value, final int s2Value, final int flxValue) {
        final TextView textView = (TextView) findViewById(R.id.textView);
        final TextView textView2 = (TextView) findViewById(R.id.textView2);
        final TextView textView3 = (TextView) findViewById(R.id.textView3);

        final String s1ValueString = String.valueOf(s1Value);
        final String s2ValueString = String.valueOf(s2Value);
        final String flxValueString = String.valueOf(flxValue);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(s1ValueString);
                textView2.setText(s2ValueString);
                textView3.setText(flxValueString);

            }
        });


    }

    private void calculatePosition() {
        if (calibratedBoolean && foundCervicGear && sendData) {
            //S1Difference and S2Difference are reversed, when moving forward S1 will have a negative difference
            //while S2 will have a positive different, hence angleS1Difference+angleS2Difference below;
            angleS2Difference = (s2Value - s2ValueCalibrated);
            angleS1Difference = (s1Value - s1ValueCalibrated);
            flxDifference = Math.abs(flxValue - flxValueCalibrated);

            if ((angleS1Difference + angleS2Difference) > defaultForwardAngleValue || (angleS1Difference + angleS2Difference) < defaultBackwardAngleValue || flxDifference > defaultForwardHeadPostureValue) {
                //allow goodtime reset
                storeCurrentGoodTime = true;
                //check for how many seconds someone is in the wrong position
                if (storeCurrentBadTime) {
                    currentTime = SystemClock.uptimeMillis();
                    storeCurrentBadTime = false;
                }

                if (SystemClock.uptimeMillis() - currentTime > secondsForWarning && !storeCurrentBadTime) {
                    System.out.println("warning");
                    storeCurrentBadTime = true;

                    currentTime = SystemClock.uptimeMillis();
                    //shakeBush()
                    shakeBushCounter++;
                    System.out.println("shakeBushCounter: " + shakeBushCounter);

                    switch (shakeBushCounter) {

                        case 2:
                            shakeBush(0); //don't vibrate
                            break;
                        case 3:
                            shakeBush(1); //vibrate
                            shakeBushCounter = 0;
                            break;
                    }

                }

                changeBackgroundColor(1);
                direction = 1;
                //add negative sample to moving average;
                getMovingAverage(0);


            } else {
                //allow badtime reset
                storeCurrentBadTime = true;
                shakeBushCounter = 0;
                //add positive sample to moving average;
                getMovingAverage(1);

                //if long enough in good position, changeBGcolor back
                if (storeCurrentGoodTime) {
                    currentTime = SystemClock.uptimeMillis();
                    storeCurrentGoodTime = false;
                }

                if (SystemClock.uptimeMillis() - currentTime > secondsForWarning && !storeCurrentGoodTime) {
                    storeCurrentGoodTime = true;

                    currentTime = SystemClock.uptimeMillis();
                }
                changeBackgroundColor(2);
                direction = 2;
                //show time in good position,
                //first in millis, than convert to hours, minutes, seconds
                millisInGoodPosition += 500;

                long second = (millisInGoodPosition / 1000) % 60;
                long minute = (millisInGoodPosition / (1000 * 60)) % 60;
                long hour = (millisInGoodPosition / (1000 * 60 * 60)) % 24;


                timeInGoodPosition = String.format("%02d:%02d:%02d", hour, minute, second);


            }
            //create String to send to saveData function
            dataString = Integer.toString(angleS1Difference) + "," + Integer.toString(angleS2Difference) + "," + Integer.toString(flxDifference);
            saveDataExternal(dataString, calibratedString);

        }


    }

    // BTLE device scanning callback.
    private LeScanCallback scanCallback = new LeScanCallback() {
        // Called when a device is found.
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            writeLine("Found device: " + bluetoothDevice.getAddress());
            // Check if the device has the UART service.
            if (parseUUIDs(bytes).contains(UART_UUID)) {

                // Found a device, stop the scan.
                adapter.stopLeScan(scanCallback);
                writeLine("Found UART service!");

                // Connect to the device.
                // Control flow will now go to the callback functions when BTLE events occur.
                //In handler gestopt, werkt ook zonder handler op lollipop en meeste andere android
                //telefoons met 4.4.*, alleen de Samsung S4 waarop ik ook test heeft om een aparte
                //reden een handler nodig. Gelukkig werkt handler ook weer op lollipop, dus hoef
                //geen verschillende versie te maken. Gevonden op:
                //http://stackoverflow.com/questions/20839018/while-connecting-to-ble113-from-android-4-3-is-logging-client-registered-waiti
                //en http://stackoverflow.com/questions/20069507/solved-gatt-callback-fails-to-register
                Handler mHandler = new Handler(getApplicationContext().getMainLooper());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        gatt = bluetoothDevice.connectGatt(getApplicationContext(), false, callback);
                    }
                });


            }

        }


    };

    // OnCreate, called once to initialize the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = BluetoothAdapter.getDefaultAdapter();

        //keep screen awake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayData(angleS1Difference, angleS2Difference, flxDifference);
                calculatePosition();

                handler.postDelayed(this, 500);
            }
        });


        //Grab references to UI elements;
        messages = (TextView) findViewById(R.id.messages);
        settings_button = (ImageButton) findViewById(R.id.settings_button);
        calibrate_button = (ImageButton) findViewById(R.id.calibrate_button);
        overview_button = (ImageButton) findViewById(R.id.overview_button);


        //do once on startup
        sampleCountWasReset = true;
        if (MyApp.doOnce) {
            //calibrate
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    showDialog();
                    System.out.println("Showdialog");
                }
            };
            handler.postDelayed(r, 4000);
            //Hide all Leafs

            for (int i = 1; i < 16; i++) {
                String leafstring = "leaf";
                leafstring = leafstring + Integer.toString(i);
                int resourceId = getResources().getIdentifier(leafstring, "id", getPackageName());
                ImageView leaf = (ImageView) findViewById(resourceId);
                leaf.getBackground().setAlpha(0);
            }

            //updateLeafs runnable
            updateLeafs();
            MyApp.doOnce = false;


            //huidige datum en tijd en naar string parsen
            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        }


        //not used, only for debugging so:
        messages.setVisibility(View.GONE); //invisible AND doesn't take up any space


        //check bluetooth, else vraag user om bluetooth
        //te enablen met Intent
        if (adapter == null || !adapter.isEnabled()) {
            final Intent enableBtIntent = new Intent(adapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //activity result onActivityResult aanroepen voor resultaat
        }

        //set preference file, als false, dan zal ie nooit the user preferences overriden
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //read Preferences
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        alphaIncrement = Integer.parseInt(sharedPref.getString("alphaIncrement", "1"));
        defaultForwardAngleValue = Integer.parseInt(sharedPref.getString("defaultForwardAngleValue", "10"));
        defaultBackwardAngleValue = Integer.parseInt(sharedPref.getString("defaultBackwardAngleValue", "5"));
        //invert backwardangle for calculation later
        defaultBackwardAngleValue *= -1;
        defaultForwardHeadPostureValue = Integer.parseInt(sharedPref.getString("defaultForwardHeadPostureValue", "6"));


        //Settings button listener
        settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        //Calibrate button listener
        calibrate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();

            }
        });

        //Overview3 button listener
        overview_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Overview.class); //Overview.class
                startActivity(intent);
            }
        });


    }


    public void showDialog() {
        final FragmentManager fragmentManager = getFragmentManager();
        final DialogFragment newFragment = new CalibrateDialog();


        // FullScreen (met gezette margins)
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        //anim
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(android.R.id.content, newFragment)
                .commitAllowingStateLoss();
        animateCalibrating();
    new CountDownTimer(5000, 1000) {

            public void onTick(long millisUntilFinished) {
                //reset shakeCounter
                shakeBushCounter = 0;

            }
            public void onFinish() {
                s1ValueCalibrated = s1Value;
                s2ValueCalibrated = s2Value;
                flxValueCalibrated = flxValue;
                shakeBushCounter = 0;
                direction = 2;
                //create String to show if calibrated in logfile
                //resetting this after dataSample gathered
                calibratedBoolean = true;
                calibratedString = ",calibrated";
                newFragment.dismissAllowingStateLoss();
            }
        }.start();
    }

    Handler handler = new Handler();
//create runnables
    Runnable myFirstTask = new Runnable() {
        @Override
        public void run() {
            ImageView sitStraight = (ImageView) findViewById(R.id.sitStraightView);
            TextView tv = (TextView) findViewById(R.id.calibratingTextView);
            if (!foundCervicGear) {
                tv.setText("CervicGear not found..");
                sitStraight.setBackgroundResource(R.drawable.cervicgear_notfound);
            } else {
                tv.setText("Calibrating..");
                //animate ImageView sitStraight
                sitStraight.setBackgroundResource(R.drawable.sit_straight);
            }
            AlphaAnimation animation1 = new AlphaAnimation(1.0f, 0.5f);
            animation1.setDuration(1000);
            sitStraight.startAnimation(animation1);
        }
    };
    Runnable mySecondTask = new Runnable() {
        @Override
        public void run() {
            ImageView sitStraight = (ImageView) findViewById(R.id.sitStraightView);
            TextView tv = (TextView) findViewById(R.id.calibratingTextView);
            if (!foundCervicGear) {
                tv.setText("CervicGear not found...");
                sitStraight.setBackgroundResource(R.drawable.cervicgear_notfound);
            } else {

                tv.setText("Calibrating...");
                //animate ImageView sitStraight
                sitStraight.setBackgroundResource(R.drawable.sit_straight);
            }
            AlphaAnimation animation1 = new AlphaAnimation(0.5f, 1.0f);
            animation1.setDuration(1000);
            sitStraight.startAnimation(animation1);
        }
    };

    Runnable myThirdTask = new Runnable() {
        @Override
        public void run() {
            ImageView sitStraight = (ImageView) findViewById(R.id.sitStraightView);
            TextView tv = (TextView) findViewById(R.id.calibratingTextView);
            if (!foundCervicGear) {
                tv.setText("CervicGear not found.");
                sitStraight.setBackgroundResource(R.drawable.cervicgear_notfound);
            } else {
                tv.setText("Calibrating.");
                //animate ImageView sitStraight
                sitStraight.setBackgroundResource(R.drawable.sit_straight);
            }
            AlphaAnimation animation1 = new AlphaAnimation(1.0f, 0.5f);
            animation1.setDuration(1000);
            sitStraight.startAnimation(animation1);
        }
    };

    Runnable myFourthTask = new Runnable() {
        @Override
        public void run() {
            ImageView sitStraight = (ImageView) findViewById(R.id.sitStraightView);
            TextView tv = (TextView) findViewById(R.id.calibratingTextView);
            if (!foundCervicGear) {
                tv.setText("CervicWear not found..");
                sitStraight.setBackgroundResource(R.drawable.cervicgear_notfound);
            } else {
                tv.setText("Calibration Completed!");
                //animate ImageView sitStraight
                sitStraight.setBackgroundResource(R.drawable.sit_straight);
            }
            AlphaAnimation animation1 = new AlphaAnimation(0.5f, 1.0f);
            animation1.setDuration(1000);
            sitStraight.startAnimation(animation1);
        }
    };

    public void animateCalibrating() {
        handler.postDelayed(myFirstTask, 1000);
        handler.postDelayed(mySecondTask, 2000);
        handler.postDelayed(myThirdTask, 3000);
        handler.postDelayed(myFourthTask, 4000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                writeLine("Bluetooth Enabled, Continuing");
            } else {
                writeLine("Without Bluetooth the App won't work, please enable Bluetooth");
            }
        }
    }

    // OnResume, called right before UI is displayed.  Start the BTLE connection.
    @Override
    protected void onResume() {
        super.onResume();
        // Scan for all BTLE devices.
        // The first one with the UART service will be chosen--see the code in the scanCallback.
        writeLine("Scanning for devices...");
        adapter.startLeScan(scanCallback);
        //adapter.startDiscovery();
        System.out.println("onResume");
        //read Preferences
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        alphaIncrement = Integer.parseInt(sharedPref.getString("alphaIncrement", "1"));
        defaultForwardAngleValue = Integer.parseInt(sharedPref.getString("defaultForwardAngleValue", "10"));
        defaultBackwardAngleValue = Integer.parseInt(sharedPref.getString("defaultBackwardAngleValue", "5"));
        //invert backwardangle for calculation later
        defaultBackwardAngleValue *= -1;
        defaultForwardHeadPostureValue = Integer.parseInt(sharedPref.getString("defaultForwardHeadPostureValue", "6"));
        soundEnabled = sharedPref.getBoolean("soundEnabled", true); //+vibration
        incomingDataEnabled = sharedPref.getBoolean("incomingDataEnabled", false);
        //show/hide incomingData
        TextView textView = (TextView) findViewById(R.id.textView);
        TextView textView2 = (TextView) findViewById(R.id.textView2);
        TextView textView3 = (TextView) findViewById(R.id.textView3);
        if (incomingDataEnabled) {
            textView.setVisibility(View.VISIBLE);
            textView2.setVisibility(View.VISIBLE);
            textView3.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
            textView2.setVisibility(View.GONE);
            textView3.setVisibility(View.GONE);
        }

    }

    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
        if (handler != null) {
            //handlers removen, anders crasht app als je
            //weg navigeert
            handler.removeCallbacks(myFirstTask);
            handler.removeCallbacks(mySecondTask);
            handler.removeCallbacks(myThirdTask);
            handler.removeCallbacks(myFourthTask);
            //handler.removeCallbacks(saveData);
        }

    }
    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.append(text);
                messages.append("\n");
            }
        });
    }
    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    // http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the above thread to fix it.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_about) {
            Intent intent = new Intent(getApplicationContext(), About.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateLeafs() {
        System.out.println(foundCervicGear);
        runOnUiThread(new Runnable() {
           @Override
            public void run() {

                final TextView connectedTextView = (TextView) findViewById(R.id.connectedTextView);
                if (!sendData) {
                    connectedTextView.setText("Please restart the CervicWear, not receiving all data");
                }
                //Do nothing if disconnected, but keep runnable running.
                else if (foundCervicGear) {
                    //hide textView
                    connectedTextView.setText("Time in good position \n" + timeInGoodPosition);
                    //if good direction (=2);
                    //increase alpha
                    if (direction == 2) {
                        alphaValue += alphaIncrement;

                        if (alphaValue > 255) {
                            alphaValue = 0;
                            leafnumber++;
                            //animate stem to get larger
                            ImageView plant_stam = (ImageView) findViewById(R.id.plant_stam);
                            //get pixels relative to screen dpi
                            //System.out.println("pixels"+getPixelFromDip(5f));
                            TranslateAnimation plant_stam_animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, newYPosition, newYPosition - getPixelFromDip(5f));
                            newYPosition -= getPixelFromDip(5f);
                            if (newYPosition < -225) {
                                newYPosition = -225;
                            }
                            plant_stam_animation.setInterpolator(new DecelerateInterpolator());
                            plant_stam_animation.setDuration(1000);
                            plant_stam_animation.setFillAfter(true);
                            plant_stam.startAnimation(plant_stam_animation);
                            if (leafnumber > 15) {
                                TranslateAnimation plant_stam_animation2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, newYPosition, 0);
                                plant_stam.startAnimation(plant_stam_animation2);
                            }
                        }
                    }
                    if (direction == 1) {
                        alphaValue -= alphaIncrement;
                        if (alphaValue < 0) {
                            if (leafnumber > 1) {
                                alphaValue = 255;
                                leafnumber--;
                                ImageView plant_stam = (ImageView) findViewById(R.id.plant_stam);
                                TranslateAnimation plant_stam_animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, newYPosition, newYPosition + 15);
                                newYPosition += 15;
                                if (newYPosition > 0) {
                                    newYPosition = 0;
                                }
                                plant_stam_animation.setInterpolator(new DecelerateInterpolator());
                                plant_stam_animation.setDuration(1000);
                                plant_stam_animation.setFillAfter(true);
                                plant_stam.startAnimation(plant_stam_animation);
                            } else {
                                alphaValue = 0;
                            }
                        }
                    }


                    if (leafnumber == 16) {
                        //reset plant stam
                        newYPosition = 0;
                        //reset leafs
                        for (int i = 1; i < 16; i++) {
                            String leafstring = "leaf";
                            leafstring = leafstring + Integer.toString(i);
                            int resourceId = getResources().getIdentifier(leafstring, "id", getPackageName());
                            ImageView leaf = (ImageView) findViewById(resourceId);
                            leaf.getBackground().setAlpha(0);
                        }
                        ImageView plant_complete1 = (ImageView) findViewById(R.id.plant_complete1);
                        ImageView plant_complete2 = (ImageView) findViewById(R.id.plant_complete2);
                        ImageView plant_complete3 = (ImageView) findViewById(R.id.plant_complete3);
                        //add plant
                        plant_complete_count++;
                        switch (plant_complete_count) {
                            case 1:
                                plant_complete1.setVisibility(View.VISIBLE);
                                break;
                            case 2:
                                plant_complete2.setVisibility(View.VISIBLE);
                                break;
                            case 3:
                                plant_complete3.setVisibility(View.VISIBLE);
                                plant_complete_count = 0;
                                break;
                        }
                        leafnumber = 1;
                    }
                    String leafstring = "leaf";
                    leafstring = leafstring + Integer.toString(leafnumber);
                    int resourceId = getResources().getIdentifier(leafstring, "id", getPackageName());
                    ImageView leaf = (ImageView) findViewById(resourceId);
                    leaf.getBackground().setAlpha(alphaValue);
                } else {
                    //show connect message
                    connectedTextView.setText("Not connected to CervicWear \n Please turn it on");
                }
                handler.postDelayed(this, 1000);
            }
        });
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void saveDataExternal(String alldatacombined, String calibratedString2) {
        if (foundCervicGear) {
            System.out.println(alldatacombined + calibratedString2);
            oneMinuteData[dataSample] = alldatacombined + calibratedString2;
            System.out.println("DataSample :" + dataSample);
            calibratedString = ",";
            //System.out.println(dataSample);
            //elke minuut data opslaan, elke 500ms wordt savedata gecalled, dus 120 samples na 1 minuut
            //begint bij 0 dus 119
            if (calibratedBoolean && isExternalStorageWritable() && dataSample > 118) { //119
                 String root = Environment.getExternalStorageDirectory().toString();
                String logfile = "logfile_cervapp_" + currentDateTimeString + ".txt";
                File cervicAppDir = new File(root + "/Cervicapp");
                File file = new File(cervicAppDir, logfile);
                try {
                    if (!cervicAppDir.exists()) {
                        //create folder
                        cervicAppDir.mkdirs();
                    }
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    //else append
                    for (int i = 0; i < oneMinuteData.length; i++) {
                        FileOutputStream fOut = new FileOutputStream(file, true);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                        myOutWriter.append(oneMinuteData[i]).append("\n");
                        myOutWriter.close();
                        fOut.close();
                    }

                } catch (IOException e) {

                    System.out.println(e.getMessage());
                }
                //reset dataSample
                dataSample = -1;
            }
            dataSample++;
        }

    }


    public void changeBackgroundColor(int direction) {
        boolean colorRedDone;
        boolean colorBlueDone;
        //System.out.println("updating");
        View mainActivityView = (View) findViewById(R.id.mainActivityView);
        int currentColor = ((ColorDrawable) mainActivityView.getBackground()).getColor();
        Integer colorBlue = getResources().getColor(R.color.BlueCervicApp);
        Integer colorRed = getResources().getColor(R.color.RedCervicApp);
        colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorBlue, colorRed);
        if (direction == 1) {
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorBlue, colorRed);
        } else if (direction == 2) {
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorRed, colorBlue);
        }
        //check if animation is done and if color should be updated
        colorRedDone = currentColor == colorRed;
        colorBlueDone = currentColor == colorBlue;
        if (!colorAnimation.isRunning()) {
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    View mainActivityView = (View) findViewById(R.id.mainActivityView);
                    mainActivityView.setBackgroundColor((Integer) animator.getAnimatedValue());
                }
            });

            colorAnimation.setDuration(colorAnimationDuration);
            if (colorRedDone && direction == 2) {
                //System.out.println("animationstarted");
                colorAnimation.start();
            }
            if (colorBlueDone && direction == 1) {
                // System.out.println("animationstarted");
                colorAnimation.start();
            }
        } else {
            //System.out.println("animationnotstarted");
        }

    }

    private void shakeBush(final int vibrate) {
        //animation
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //sound
                if (soundEnabled) {
                    mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.bush_shake2);
                    mediaPlayer.start();
                    if (vibrate == 1) {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        // Vibrate for 500 milliseconds
                        v.vibrate(1000);
                    }
                }
                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim);
                ImageView plant_bak = (ImageView) findViewById(R.id.plant_bak);
                ImageView plant_stam = (ImageView) findViewById(R.id.plant_stam);
                for (int i = 1; i < 16; i++) {
                    String leafstring = "leaf";
                    leafstring = leafstring + Integer.toString(i);
                    int resourceId = getResources().getIdentifier(leafstring, "id", getPackageName());
                    ImageView leaf = (ImageView) findViewById(resourceId);
                    leaf.startAnimation(animation);
                }
                plant_bak.startAnimation(animation);
                plant_stam.startAnimation(animation);
            }
        });

    }

    //create function movingAverage
    //partly copied from M21 Flash application
    private void getMovingAverage(int sample1) {
        String currentAvgAndSampleCount = "";
        //Check for existingMovingAverage
        checkExistingMovingAverage();
        //calculate a so called 'Moving Average'
        avg = (avg + (sample1 - avg) / sampleCount);
        //multiply by 100 when retrieving
        // System.out.println("sample = "+sample1 +", avg = "+avg+", sampleCount = "+sampleCount);
        currentAvgAndSampleCount = Float.toString(avg) + "," + Long.toString(sampleCount);
        sampleCount++;
        currentSampleCount++;
        //if one minute passed update sampleCount //119
        if (currentSampleCount > storeCurrentSample || resetAverageFile) {
            String root = Environment.getExternalStorageDirectory().toString();
            File cervicAppDir = new File(root + "/Cervicapp");

            System.out.println("writing averages file");
            File averagesFile = new File(cervicAppDir, "AveragesFile.txt");
            if (resetAverageFile) {
                try {
                    averagesFile.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(averagesFile, false);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    //resetting file
                    myOutWriter.append(Float.toString(0.f) + ",1").append("q").append("\n");
                    myOutWriter.close();
                    fOut.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
                resetAverageFile = false;
            } else {
                try {
                    averagesFile.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(averagesFile, false);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    ///using q to detect endofline as \n is discarded by bufferedreader
                    myOutWriter.append(currentAvgAndSampleCount).append("q").append("\n");
                    myOutWriter.close();
                    fOut.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
                currentSampleCount = 0;
            }
        }

    }

    private void checkExistingMovingAverage() {
        String root = Environment.getExternalStorageDirectory().toString();
        File cervicAppDir = new File(root + "/Cervicapp");
        if (sampleCountWasReset) {
            System.out.println("was reset");
            //sampleCount is reset in OnCreate, so if the activity is killed
            //it will check if there was already a samplecount.
            //Check if there's a movingAverage by looking at sampleCount
            File averagesFile = new File(cervicAppDir, "averagesFile.txt");
            StringBuilder averagesString = new StringBuilder();
            String line;
            if (averagesFile.exists()) {
                try {
                    FileInputStream fr = new FileInputStream(averagesFile);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fr));

                    while ((line = br.readLine()) != null) {
                        averagesString.append(line);

                    }
                    String lastline = averagesString.toString().trim();
                    fr.close();
                    System.out.println("lastline =" + lastline);
                    float tempAvg = Float.parseFloat(lastline.substring(0, lastline.indexOf(",")));
                    long tempSampleCount = Long.parseLong(lastline.substring(lastline.indexOf(",") + 1, lastline.length() - 1));
                    System.out.println(tempAvg + tempSampleCount);
                    //check if sampleCount < storeAverageCount, only then add to moving average
                    if (tempSampleCount < storeAverageCount) {
                        avg = tempAvg;
                        sampleCount = tempSampleCount;
                    }
                    System.out.println("average+samplecount =" + avg + ", " + sampleCount);
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
            sampleCountWasReset = false;

        }
        //System.out.println("SC ="+sampleCount);
        //sampleCount is called each time I receive data (each 500ms), so an hourly average will be complete when having
        //60*60*1000/500=7200 samples, starts at sampleCount 1; so leave 7200 as is
        if (sampleCount >= storeAverageCount) {
            //log hourly average
            try {
                System.out.println("writing hourly averages file");
                File averagesFile = new File(cervicAppDir, "hourlyAverageFile.txt");
                averagesFile.createNewFile();
                FileOutputStream fOut = new FileOutputStream(averagesFile, true);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                ///using q to detect endofline as \n is discarded by bufferedreader
                myOutWriter.append(Float.toString(avg)).append("q").append("\n");
                myOutWriter.close();
                fOut.close();
            } catch (IOException e) {
                System.out.println(e);
            }
            sampleCount = 1;
            resetAverageFile = true;
        }
    }

    public int getPixelFromDip(float dip) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }
}

