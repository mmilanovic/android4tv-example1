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

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.iwedia.adapters.ChannelListAdapter;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.zapp.R;

/**
 * Channel List Activity.
 */
public class ChannelListActivity extends DTVActivity implements
        OnItemClickListener {
    public static final String TAG = "ChannelListActivity";
    private GridView mChannelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_list_activity);
        /** Initialize GridView. */
        initializeChannelList();
        mChannelList.setAdapter(new ChannelListAdapter(this, mDVBManager
                .getChannelNames()));
        mChannelList.setSelection(mDVBManager.getCurrentChannelNumber());
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
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        try {
            mDVBManager.changeChannelByNumber(position);
        } catch (InternalException e) {
            /** Error with service connection. */
            finishActivity();
        } catch (IllegalArgumentException e) {
            Log.e(TAG,
                    "Error with adapter data when trying to change channel!", e);
            ((ChannelListAdapter) parent.getAdapter()).notifyDataSetChanged();
        }
    }
}
