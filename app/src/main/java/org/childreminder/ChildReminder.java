package org.childreminder;

import android.app.Application;


public class ChildReminder extends Application {
    public enum Status {
        CHILD_SITTING,
        NO_CHILD_SITTING,
        NO_CONNECTION
    }

    public Status status = Status.NO_CONNECTION;
    public long lastUpdated = System.currentTimeMillis();
    public int lastRssi = 0;
    public boolean isAlert = false;
}
