package org.childreminder;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.util.Log;

public class MainService extends IntentService {
    boolean shouldDisableBT = false;
    boolean isConnected = false;

    final static String NOTIFY_CHANGE = "NOTIFY_CHANGE";
    final int TIME_OF_SCAN_IN_SEC = 2;
    final int RSSI_TOO_FAR = -90;
    final int TIME_TO_SLEEP_BETWEEN_ITERATIONS_IN_SEC = 20;

    public MainService() {
        super("MainService");
    }

    // Receiver for capturing BT state change to ON and start BLE scan
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        startScan();
                        break;
                }
            }
        }
    };

    private void changeStatus(ChildReminder.Status status) {
        ((ChildReminder)getApplicationContext()).status = status;
    }

    // Receiver for BLE scan result - if the result belong to our device, stop the scan and parse the results
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            if (deviceName != null && deviceName.equals("Child Reminder")) {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

                if (null != scanner) {
                    scanner.stopScan(mScanCallback);
                }

                if (shouldDisableBT) {
                    disableBT();
                }

                ((ChildReminder) getApplicationContext()).preLastRssi = ((ChildReminder) getApplicationContext()).lastRssi;
                ((ChildReminder) getApplicationContext()).lastRssi = result.getRssi();

                if (result.getScanRecord().getBytes()[22] == 1) {
                    changeStatus(ChildReminder.Status.CHILD_SITTING);
                } else {
                    changeStatus(ChildReminder.Status.NO_CHILD_SITTING);
                }

                isConnected = true;
            }
        }
    };

    // Start BLE scan
    private void startScan() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (null != scanner) {
            scanner.startScan(mScanCallback);
        }
    }

    private void disableBT() {
        shouldDisableBT = false;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.disable();
    }

    private void enableBTandStartScan() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                startScan();
            } else {
                shouldDisableBT = true;
                mBluetoothAdapter.enable();
            }
        }
    }

    // Set a recurring alarm to launch the service again in TIME_TO_SLEEP_BETWEEN_ITERATIONS_IN_SEC
    private void setRecurringAlarm() {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainBroadcastReceiver.class);
        intent.putExtra("alarmAlreadySet", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1234, intent, 0);

        AlarmManager alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + TIME_TO_SLEEP_BETWEEN_ITERATIONS_IN_SEC * 1000, pendingIntent);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d("Service", "onHandleIntent");

        // Set next service launch
        setRecurringAlarm();

        try {
            isConnected = false;

            // Register the receiver for BluetoothAdapter.ACTION_STATE_CHANGED
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);

            enableBTandStartScan();

            // Wait for scan to finish
            Thread.sleep(TIME_OF_SCAN_IN_SEC * 1000);

            // Disable BT if needed
            if (shouldDisableBT) {
                disableBT();
            }

            // Unregister the receiver for BluetoothAdapter.ACTION_STATE_CHANGED
            unregisterReceiver(mReceiver);

            // Decided if need to change status and alert alert the user of a forgotten child
            if (!isConnected) {
                if (((ChildReminder)getApplicationContext()).status == ChildReminder.Status.CHILD_SITTING) {
                    alert();
                }

                changeStatus(ChildReminder.Status.NO_CONNECTION);
            } else if (((ChildReminder)getApplicationContext()).status == ChildReminder.Status.CHILD_SITTING &&
                    ((ChildReminder)getApplicationContext()).lastRssi < RSSI_TOO_FAR &&
                    ((ChildReminder)getApplicationContext()).preLastRssi >= RSSI_TOO_FAR) {
                alert();
            } else {
                ((ChildReminder)getApplicationContext()).isAlert = false;
            }

            // Update lastUpdated global
            ((ChildReminder) getApplicationContext()).lastUpdated = System.currentTimeMillis();

            // Notify the UI of new data
            Intent intent = new Intent();
            intent.setAction(NOTIFY_CHANGE);
            sendBroadcast(intent);

            // Try to disable BT again if failed
            Thread.sleep(1000);
            if (shouldDisableBT) {
                disableBT();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Alerts the user by notification and sound alert
    public void alert() {
        ((ChildReminder)getApplicationContext()).isAlert = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Notify the user of missing child
        Notification n  = new Notification.Builder(this)
                .setContentTitle("CHILD ALERT")
                .setContentText("Is your child with you?")
                .setContentIntent(pIntent)
                .setSmallIcon(R.drawable.icon)
                .setAutoCancel(true).build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, n);

        // Alert sound
        ((ChildReminder)getApplicationContext()).player = MediaPlayer.create(this, R.raw.alarm);
        ((ChildReminder)getApplicationContext()).player.setVolume(60,60);
        ((ChildReminder)getApplicationContext()).player.start();
    }
}