package org.childreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// BroadcastReceiver to start MainService on boot and on alarm
public class MainBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startServiceIntent = new Intent(context, MainService.class);
            context.startService(startServiceIntent);
        }
}
