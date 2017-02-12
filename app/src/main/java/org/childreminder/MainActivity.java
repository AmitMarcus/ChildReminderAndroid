package org.childreminder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private boolean isChildSitted = false;
    int lastUpdated = 0;
    boolean shouldDisableBT = false;

    private void startScan() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        // scan for devices
        scanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // get the discovered device as you wish
                // this will trigger each time a new device is found

                BluetoothDevice device = result.getDevice();
                TextView tv1 = (TextView)findViewById(R.id.textView1);

                String deviceName = device.getName();

                if (deviceName != null && deviceName.equals("Child Reminder")) {
                    if (result.getScanRecord().getBytes()[22] == 0) {
                        tv1.setText("# Child is sitting in the car! " + Integer.toString(result.getRssi()));
                    } else {
                        tv1.setText("# No child is sitting in the car! " + Integer.toString(result.getRssi()));
                    }
                }
            }
        });
    }
//
//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
//            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//                switch (bluetoothState) {
//                    case BluetoothAdapter.STATE_ON:
//                        startScan();
//                        break;
//                }
//            }
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent mServiceIntent = new Intent(this, MainService.class);
//        mServiceIntent.setData(Uri.parse(dataUrl));
        startService(mServiceIntent);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Intent intent = getIntent();
        TextView tv1 = (TextView)findViewById(R.id.textView1);
        tv1.setText(Long.toString(intent.getLongExtra("lastUpdated", 0)));
//
//        System.exit(1);

//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter != null) {
//            IntentFilter filter = new IntentFilter();
//
//            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//
//            registerReceiver(mReceiver, filter);
//
//            if (mBluetoothAdapter.isEnabled()) {
//                startScan();
//            } else {
//                shouldDisableBT = true;
//                mBluetoothAdapter.enable();
//            }
//        } else {
//            TextView tv1 = (TextView)findViewById(R.id.textView1);
//            tv1.setText("NULL");
//        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
//        // TODO: disable only if we enabled the BT
//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        mBluetoothAdapter.disable();
//
//        unregisterReceiver(mReceiver);

        super.onDestroy();
    }
}
