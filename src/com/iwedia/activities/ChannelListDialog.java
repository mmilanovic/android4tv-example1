/*
 * Copyright (C) 2014 iWedia S.A. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.iwedia.activities;

import android.app.Dialog;
import android.graphics.PixelFormat;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;

import com.iwedia.adapters.ChannelListAdapter;
import com.iwedia.dtv.DVBManager;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.zapp.R;

/**
 * Channel List Activity.
 */
public class ChannelListDialog extends Dialog implements OnItemClickListener {
    public static final String TAG = "ChannelListActivity";
    private GridView mChannelList = null;
    private ZappActivity mActivity = null;
    private DVBManager mDVBManager = null;

    public ChannelListDialog(ZappActivity activity, DVBManager dvbManager,
            int width, int height) {
        super(activity, R.style.DialogTransparent);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Hide Status Bar of Android.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        setContentView(R.layout.channel_list_dialog);
        getWindow().getAttributes().width = width;
        getWindow().getAttributes().height = height;
        mActivity = activity;
        mDVBManager = dvbManager;
        /** Initialize GridView. */
        initializeChannelList();
        mChannelList.setAdapter(new ChannelListAdapter(activity, dvbManager
                .getChannelNames()));
        mChannelList.setSelection(dvbManager.getCurrentChannelNumber());
    }

    /**
     * Initialize GridView (Channel List) and set click listener to it.
     * 
     * @throws RemoteException
     *         If connection error happens.
     */
    private void initializeChannelList() {
        mChannelList = (GridView) findViewById(R.id.gridview_channellist);
        mChannelList.setOnItemClickListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mActivity.showChannelInfo(mDVBManager.getChannelInfo(
                mDVBManager.getCurrentChannelNumber(), true));
        ((ChannelListAdapter) mChannelList.getAdapter())
                .setInChannelLockedState(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.channel_lock, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.menu_channel_lock) {
            ((ChannelListAdapter) mChannelList.getAdapter())
                    .setInChannelLockedState(!((ChannelListAdapter) mChannelList
                            .getAdapter()).isInChannelLockedState());
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (((ChannelListAdapter) parent.getAdapter()).isInChannelLockedState()) {
            CheckBox checkBox = (CheckBox) v
                    .findViewById(R.id.check_box_locked);
            try {
                DVBManager.getInstance().getParentalManager()
                        .setChannelLock(position, !checkBox.isChecked());
            } catch (InternalException e) {
                e.printStackTrace();
            }
            checkBox.setChecked(!checkBox.isChecked());
            v.invalidate();
        } else {
            try {
                cancel();
                mActivity.showChannelInfo(mDVBManager
                        .changeChannelByNumber(position));
            } catch (InternalException e) {
                /** Error with service connection. */
                mActivity.finishActivity();
            } catch (IllegalArgumentException e) {
                Log.e(TAG,
                        "Error with adapter data when trying to change channel!",
                        e);
                ((ChannelListAdapter) parent.getAdapter())
                        .notifyDataSetChanged();
            }
        }
    }
}
