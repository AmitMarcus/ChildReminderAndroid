package org.childreminder;

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
    final int TIME_OF_SCAN_IN_SEC = 5;
    final int RSSI_TOO_FAR = -90;

    public MainService() {
        super("MainService");
    }

    // TODO: when BT on the receiver is TOCHEN the phone, maybe register and unregister every iteration of the main loop, or with a "should work" flag
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

    private void goForeground(String msg) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Child Reminder Service")
                .setContentText(msg)
                .setSmallIcon(R.drawable.baby)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void changeStatus(ChildReminder.Status status) {
        ((ChildReminder)getApplicationContext()).status = status;

//        switch (((ChildReminder)getApplicationContext()).status) {
//            case NO_CONNECTION:
//                goForeground("No connection.");
//                break;
//            case NO_CHILD_SITTING:
//                goForeground("No child is sitting in the car!");
//                break;
//            case CHILD_SITTING:
//                goForeground("Child is sitting in the car!");
//                break;
//        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            if (deviceName != null && deviceName.equals("Child Reminder")) {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
                // scan for devices
                scanner.stopScan(mScanCallback);

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

    private void startScan() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        // scan for devices
        scanner.startScan(mScanCallback);
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

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d("Service", "onHandleIntent");

        isConnected = false;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return;
        }

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(mReceiver, filter);

        enableBTandStartScan();

        try {
            Thread.sleep(TIME_OF_SCAN_IN_SEC * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        unregisterReceiver(mReceiver);

        if (shouldDisableBT) {
            disableBT();
        }

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

        ((ChildReminder) getApplicationContext()).lastUpdated = System.currentTimeMillis();

        Intent intent = new Intent();
        intent.setAction(NOTIFY_CHANGE);
        sendBroadcast(intent);
    }

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