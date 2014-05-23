package com.iwedia.callbacks;

import android.util.Log;

import com.iwedia.dtv.DVBManager;
import com.iwedia.dtv.epg.IEpgCallback;
import com.iwedia.dtv.types.InternalException;

public class EPGCallBack implements IEpgCallback {
    private static final String TAG = "EPGCallBack";
    private DVBManager mDVBManager = null;

    public EPGCallBack() {
        try {
            mDVBManager = DVBManager.getInstance();
        } catch (InternalException e) {
            Log.w(TAG, "There was an Internal Error.", e);
        }
    }

    @Override
    public void pfAcquisitionFinished(int arg0, int arg1) {
        if (mDVBManager != null) {
            mDVBManager.udpateNowNext();
        }
    }

    @Override
    public void pfEventChanged(int arg0, int arg1) {
        if (mDVBManager != null) {
            mDVBManager.udpateNowNext();
        }
    }

    @Override
    public void scAcquisitionFinished(int arg0, int arg1) {
    }

    @Override
    public void scEventChanged(int arg0, int arg1) {
    }
}
