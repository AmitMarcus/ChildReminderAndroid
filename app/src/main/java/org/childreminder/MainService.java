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

public class MainService extends IntentService {
    boolean shouldDisableBT = false;

    final static String MY_ACTION = "MY_ACTION";

    public MainService() {
        super("MainService");
    }

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

        switch (((ChildReminder)getApplicationContext()).status) {
            case NO_CONNECTION:
                goForeground("No connection.");
                break;
            case NO_CHILD_SITTING:
                goForeground("No child is sitting in the car!");
                break;
            case CHILD_SITTING:
                goForeground("Child is sitting in the car!");
                break;
        }
    }

    private void startScan() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        // scan for devices
        scanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();

                if (shouldDisableBT) {
                    disableBT();
                }

                String deviceName = device.getName();

                if (deviceName != null && deviceName.equals("Child Reminder")) {
                    ((ChildReminder)getApplicationContext()).lastUpdated = System.currentTimeMillis();

                    ((ChildReminder)getApplicationContext()).lastRssi = result.getRssi();

                    if (result.getScanRecord().getBytes()[22] == 0) {
                        changeStatus(ChildReminder.Status.NO_CHILD_SITTING);
                    } else {
                        changeStatus(ChildReminder.Status.CHILD_SITTING);
                    }

                    Intent intent = new Intent();
                    intent.setAction(MY_ACTION);
                    sendBroadcast(intent);
                }
            }
        });
    }

    private void disableBT() {
        shouldDisableBT = false;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.disable();
    }

    private void checkChildStatus() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

            registerReceiver(mReceiver, filter);

            if (mBluetoothAdapter.isEnabled()) {
                startScan();
            } else {
                shouldDisableBT = true;
                mBluetoothAdapter.enable();
            }
        } else {
            // TODO:
        }
//
//        try {
//            Thread.sleep(2);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//         TODO: should check if already done from startScan...
//        if (shouldDisableBT) {
//            disableBT();
//        }
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // TODO: check what happen when BT already ON
        while (true) {
            checkChildStatus();

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // TODO: change time to > than sleep time (e.g. 15000)
            if ((System.currentTimeMillis() - ((ChildReminder)getApplicationContext()).lastUpdated) > 5000) {
                ChildReminder.Status status = ((ChildReminder)getApplicationContext()).status;

                changeStatus(ChildReminder.Status.NO_CONNECTION);

                if (status == ChildReminder.Status.CHILD_SITTING) {
                    ((ChildReminder)getApplicationContext()).isAlert = true;
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // Notify the user of missing child
                    Notification n  = new Notification.Builder(this)
                            .setContentTitle("Forgot something?")
                            .setContentText("Where is your child?")
                            .setContentIntent(pIntent)
                            .setSmallIcon(R.drawable.icon)
                            .setAutoCancel(true).build();

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(0, n);

                    // Alert sound
                    MediaPlayer player = MediaPlayer.create(this, R.raw.alarm);
                    player.setVolume(100,100);
//                player.start();
                }
            }
        }

        // TODO: move to destroy service if exist smtng like that
//        unregisterReceiver(mReceiver);
    }
}

// Notification's button
//                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.icon, "NO", pIntent).build();
//                        .addAction(action)