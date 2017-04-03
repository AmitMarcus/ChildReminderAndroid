package org.childreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateActivity();
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        updateActivity();
    }

    // Updates the UI with updated information
    public void updateActivity() {
        TextView lastUpdatedText = (TextView) findViewById(R.id.lastUpdated);
        lastUpdatedText.setText((new SimpleDateFormat("HH:mm:ss dd/MM/yyyy")).format(((ChildReminder) getApplicationContext()).lastUpdated));

        TextView statusText = (TextView) findViewById(R.id.status);
        switch (((ChildReminder) getApplicationContext()).status) {
            case NO_CONNECTION:
                statusText.setText("No connection.");
                break;
            case NO_CHILD_SITTING:
                statusText.setText("No child sitting.");
                break;
            case CHILD_SITTING:
                statusText.setText("Child is sitting!");
                break;
        }

        TextView rssiText = (TextView) findViewById(R.id.rssi);
        rssiText.setText(Integer.toString(((ChildReminder) getApplicationContext()).lastRssi));

        Button turnAlertOffButton = (Button) findViewById(R.id.turnAlertOffButton);
        if (((ChildReminder) getApplicationContext()).isAlert) {
            turnAlertOffButton.setVisibility(View.VISIBLE);
        } else {
            turnAlertOffButton.setVisibility(View.INVISIBLE);
        }
    }

    // Creates the UI & Runs the MainService
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Message handling - from service:
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainService.NOTIFY_CHANGE);
        registerReceiver(mReceiver, intentFilter);

        Intent mServiceIntent = new Intent(this, MainService.class);
        startService(mServiceIntent);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.turnAlertOffButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ChildReminder) getApplicationContext()).isAlert = false;
                ((ChildReminder) getApplicationContext()).player.stop();
                updateActivity();
            }
        });

        updateActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}