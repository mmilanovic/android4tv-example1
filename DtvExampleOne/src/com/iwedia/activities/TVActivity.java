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

import android.content.ContextWrapper;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.iwedia.dtv.ChannelInfo;
import com.iwedia.dtv.IPService;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.zapp.R;

import java.util.ArrayList;

/**
 * TVActivity - Activity for Watching Channels.
 */
public class TVActivity extends DTVActivity {
    public static final String TAG = "TVActivity";
    /** Channel Number/Name View Duration in Milliseconds. */
    private static final int CHANNEL_VIEW_DURATION = 3000;
    /** Numeric Channel Change 'Wait' Duration. */
    private static final int NUMERIC_CHANNEL_CHANGE_DURATION = 2000;
    /** Maximum Length of Numeric Buffer. */
    private static final int MAX_CHANNEL_NUMBER_LENGTH = 4;
    /** URI For VideoView. */
    public static final String TV_URI = "tv://";
    /** Views needed in activity. */
    private LinearLayout mChannelContainer = null;
    private TextView mChannelNumber = null;
    private TextView mChannelName = null;
    /** Handler for sending action messages to update UI. */
    private UiHandler mHandler = null;
    /** Buffer for Channel Index, Numeric Channel Change. */
    private StringBuilder mBufferedChannelIndex = null;
    /** Current Channel Info */
    private ChannelInfo mChannelInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity);
        /** Initialize VideoView. */
        initializeVideoView();
        /** Initialize Channel Container. */
        initializeChannelContainer();
        /** Load default IP channel list. */
        initIpChannels();
        /** Initialize Handler. */
        mHandler = new UiHandler();
        /** Initialize String Builder */
        mBufferedChannelIndex = new StringBuilder();
        /** Start DTV. */
        try {
            mChannelInfo = mDVBManager.startDTV(getLastWatchedChannelIndex());
        } catch (IllegalArgumentException e) {
            Toast.makeText(
                    this,
                    "Cant play service with index: "
                            + getLastWatchedChannelIndex(), Toast.LENGTH_SHORT)
                    .show();
        } catch (InternalException e) {
            /** Error with service connection. */
            finishActivity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /** Handle item selection. */
        switch (item.getItemId()) {
            case R.id.menu_scan_usb: {
                ArrayList<IPService> ipChannels = new ArrayList<IPService>();
                loadIPChannelsFromExternalStorage(ipChannels);
                sIpChannels = ipChannels;
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initialize IP.
     */
    private void initIpChannels() {
        ContextWrapper contextWrapper = new ContextWrapper(this);
        String path = contextWrapper.getFilesDir() + "/"
                + DTVActivity.IP_CHANNELS;
        sIpChannels = new ArrayList<IPService>();
        DTVActivity.readFile(this, path, sIpChannels);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * This is exit point of application so video playback must be stopped.
         */
        try {
            mDVBManager.stopDTV();
        } catch (InternalException e) {
            e.printStackTrace();
        }
        sIpChannels = null;
    }

    /**
     * Initialize VideoView and Set URI.
     * 
     * @return Instance of VideoView.
     */
    private VideoView initializeVideoView() {
        final VideoView videoView = ((VideoView) findViewById(R.id.videoview_tv));
        videoView.setVideoURI(Uri.parse(TV_URI));
        videoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });
        return videoView;
    }

    /**
     * Initialize LinearLayout and TextViews.
     */
    private void initializeChannelContainer() {
        mChannelContainer = (LinearLayout) findViewById(R.id.linearlayout_channel_container);
        mChannelContainer.setVisibility(View.GONE);
        mChannelNumber = (TextView) findViewById(R.id.textview_channel_number);
        mChannelName = (TextView) findViewById(R.id.textview_channel_name);
    }

    /**
     * Show Channel Name and Number of Current Channel on Channel Change.
     * 
     * @param channelInfo
     */
    private void showChannelInfo(ChannelInfo channelInfo) {
        if (channelInfo != null) {
            mChannelInfo = channelInfo;
            mChannelNumber.setText(String.valueOf(channelInfo.getNumber()));
            mChannelName.setText(channelInfo.getName());
            mChannelContainer.setVisibility(View.VISIBLE);
            mHandler.removeMessages(UiHandler.HIDE_VIEW_MESSAGE);
            mHandler.sendEmptyMessageDelayed(UiHandler.HIDE_VIEW_MESSAGE,
                    CHANNEL_VIEW_DURATION);
        }
    }

    /**
     * Show Channel Number.
     * 
     * @param channelInfo
     */
    private void showChannelNumber(int channel) {
        String lChannelIndex = String.valueOf(channel);
        /** Buffer Channel Index */
        if (mBufferedChannelIndex.length() >= MAX_CHANNEL_NUMBER_LENGTH) {
            mBufferedChannelIndex.delete(0, mBufferedChannelIndex.length());
        }
        mBufferedChannelIndex.append(lChannelIndex);
        /** Show Index and Change Channel */
        mChannelNumber.setText(mBufferedChannelIndex.toString());
        mChannelName.setText("");
        mChannelContainer.setVisibility(View.VISIBLE);
        mHandler.removeMessages(UiHandler.NUMERIC_CHANNEL_CHANGE);
        mHandler.sendEmptyMessageDelayed(UiHandler.NUMERIC_CHANNEL_CHANGE,
                NUMERIC_CHANNEL_CHANGE_DURATION);
    }

    /**
     * Listener For Keys.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "KEY PRESSED " + keyCode);
        switch (keyCode) {
        /** Open Channel List. */
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: {
                startActivity(new Intent(getApplicationContext(),
                        ChannelListActivity.class));
                return true;
            }
            /**
             * Change Channel Up (Using of KEYCODE_F4 is just workaround because
             * KeyEvent.KEYCODE_CHANNEL_UP is not mapped on remote control).
             */
            case KeyEvent.KEYCODE_F4:
            case KeyEvent.KEYCODE_CHANNEL_UP: {
                try {
                    showChannelInfo(mDVBManager.changeChannelUp());
                } catch (InternalException e) {
                    /** Error with service connection. */
                    Log.e(TAG,
                            "Error with service connection, killing application...!",
                            e);
                    finishActivity();
                }
                return true;
            }
            /**
             * Change Channel Down (Using of KEYCODE_F3 is just workaround
             * because KeyEvent.KEYCODE_CHANNEL_DOWN is not mapped on remote
             * control).
             */
            case KeyEvent.KEYCODE_F3:
            case KeyEvent.KEYCODE_CHANNEL_DOWN: {
                try {
                    showChannelInfo(mDVBManager.changeChannelDown());
                } catch (InternalException e) {
                    /** Error with service connection. */
                    Log.e(TAG,
                            "Error with service connection, killing application...!",
                            e);
                    finishActivity();
                }
                return true;
            }
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9: {
                showChannelNumber(generateChannelNumber(keyCode));
                return true;
            }
            default: {
                return super.onKeyDown(keyCode, event);
            }
        }
    }

    /**
     * Convert key code from remote control to appropriate number.
     * 
     * @param keycode
     *        Entered key code from RCU.
     * @return Converted number.
     */
    private int generateChannelNumber(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_0:
                return 0;
            case KeyEvent.KEYCODE_1:
                return 1;
            case KeyEvent.KEYCODE_2:
                return 2;
            case KeyEvent.KEYCODE_3:
                return 3;
            case KeyEvent.KEYCODE_4:
                return 4;
            case KeyEvent.KEYCODE_5:
                return 5;
            case KeyEvent.KEYCODE_6:
                return 6;
            case KeyEvent.KEYCODE_7:
                return 7;
            case KeyEvent.KEYCODE_8:
                return 8;
            case KeyEvent.KEYCODE_9:
                return 9;
            default:
                return 0;
        }
    }

    /**
     * Handler for sending action messages to update UI.
     */
    private class UiHandler extends Handler {
        /** Message ID for Hiding Channel Number/Name View. */
        public static final int HIDE_VIEW_MESSAGE = 0;
        public static final int NUMERIC_CHANNEL_CHANGE = 1;

        /** Channel Index */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_VIEW_MESSAGE: {
                    mChannelContainer.setVisibility(View.INVISIBLE);
                    break;
                }
                case NUMERIC_CHANNEL_CHANGE: {
                    int lChannelNumber = Integer.valueOf(mBufferedChannelIndex
                            .toString());
                    ChannelInfo lChannelInfo = mChannelInfo;
                    if (lChannelNumber > 0
                            && lChannelNumber <= mDVBManager
                                    .getChannelListSize()) {
                        lChannelNumber--;
                        try {
                            lChannelInfo = mDVBManager
                                    .changeChannelByNumber(lChannelNumber);
                        } catch (InternalException e) {
                            Log.e(TAG,
                                    "There was an Internal Execption on Change Channel.",
                                    e);
                        }
                    }
                    showChannelInfo(lChannelInfo);
                    /** Flush Channel Buffer */
                    mBufferedChannelIndex.delete(0,
                            mBufferedChannelIndex.length());
                    break;
                }
            }
        }
    }
}
