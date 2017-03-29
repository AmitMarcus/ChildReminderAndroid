package org.childreminder;

import android.app.Application;
import android.media.MediaPlayer;

public class ChildReminder extends Application {
    public enum Status {
        CHILD_SITTING,
        NO_CHILD_SITTING,
        NO_CONNECTION
    }

    // Globals for updating the intent from the service
    public Status status = Status.NO_CONNECTION;
    public long lastUpdated = System.currentTimeMillis();
    public int lastRssi = 0;
    public int preLastRssi = 0;
    public boolean isAlert = false;
    public MediaPlayer player;
}
