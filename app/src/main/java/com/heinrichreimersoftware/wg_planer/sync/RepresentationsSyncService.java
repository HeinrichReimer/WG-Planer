package com.heinrichreimersoftware.wg_planer.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RepresentationsSyncService extends Service {

    private static final Object syncAdapterLock = new Object();
    private static RepresentationsSyncAdapter syncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                syncAdapter = new RepresentationsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}