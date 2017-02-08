package org.childreminder;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.widget.TextView;

public class MainService extends IntentService {
    private boolean isChildSitted = false;
    long lastUpdated = 0;
    boolean shouldDisableBT = false;
    int lastRssi = 0;

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
                .setContentTitle("Child Remainder Service")
                .setContentText(msg)
                .setSmallIcon(R.drawable.baby)
                .setContentIntent(pendingIntent)
                .setTicker("TICKER")
                .build();

        startForeground(1, notification);
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
                    lastUpdated = System.currentTimeMillis();
                    lastRssi = result.getRssi();

                    if (result.getScanRecord().getBytes()[22] == 0) {
                        isChildSitted = false;
                        goForeground("No child is sitting in the car!");
                    } else {
                        isChildSitted = true;
                        goForeground("Child is sitting in the car!");
                    }
                }
            }
        });
    }

    private void disableBT() {
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
        goForeground("No connection.");

        while (true) {
            checkChildStatus();

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // TODO: change time to > than sleep time (e.g. 15000)
            if (isChildSitted && (System.currentTimeMillis() - lastUpdated) > 5000) {
                Intent intent = new Intent(this, MainActivity.class);
                PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

                // build notif  ication
                // the addAction re-use the same intent to keep the example short
                Notification n  = new Notification.Builder(this)
                        .setContentTitle("New mail from " + "test@gmail.com")
                        .setContentText("Subject")
                        .setContentIntent(pIntent)
                        .setSmallIcon(R.drawable.icon)
                        .setAutoCancel(true).build();

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(0, n);

                Intent dialogIntent = new Intent(this, MainActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);

                MediaPlayer player = MediaPlayer.create(this, R.raw.alarm);
                player.setVolume(100,100);
                player.start();
            }
        }

        // TODO: move to destroy service if exist smtng like that
//        unregisterReceiver(mReceiver);
    }
}


//        while (true) {
//                try {
//                Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                e.printStackTrace();
//                }
//
//                // prepare intent which is triggered if the
//                // notification is selected
//
//                Intent intent = new Intent(this, MainActivity.class);
//        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
//
//        // build notif  ication
//        // the addAction re-use the same intent to keep the example short
//        Notification n  = new Notification.Builder(this)
//        .setContentTitle("New mail from " + "test@gmail.com")
//        .setContentText("Subject")
//        .setContentIntent(pIntent)
//        .setSmallIcon(R.drawable.icon)
//        .setAutoCancel(true).build();
//
//
//        NotificationManager notificationManager =
//        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//
//        notificationManager.notify(0, n);
//        }