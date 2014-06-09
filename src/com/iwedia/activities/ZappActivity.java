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
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.iwedia.dtv.ChannelInfo;
import com.iwedia.dtv.DVBManager;
import com.iwedia.dtv.IPService;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.dtv.types.TimeDate;
import com.iwedia.zapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * TVActivity - Activity for Watching Channels.
 */
public class ZappActivity extends DTVActivity {
    public static final String TAG = "TVActivity";
    /** Channel Number/Name View Duration in Milliseconds. */
    private static final int CHANNEL_VIEW_DURATION = 5000;
    /** Numeric Channel Change 'Wait' Duration. */
    private static final int NUMERIC_CHANNEL_CHANGE_DURATION = 2000;
    /** Maximum Length of Numeric Buffer. */
    private static final int MAX_CHANNEL_NUMBER_LENGTH = 4;
    /** Volume States */
    private static final int VOLUME_UP = 0;
    private static final int VOLUME_DOWN = 1;
    private static final int VOLUME_MUTE = 2;
    /** URI For VideoView. */
    public static final String TV_URI = "tv://";
    /** Time and Date Format */
    private SimpleDateFormat mTimeFormat = null;
    private SimpleDateFormat mDateFormat = null;
    /** Views needed in activity. */
    private RelativeLayout mChannelInfoContainer = null;
    private RelativeLayout mNowNextContainer = null;
    private TextView mChannelNumber = null;
    private TextView mChannelName = null;
    private TextView mEPGNow = null;
    private TextView mEPGNext = null;
    private TextView mEPGStartTime = null;
    private TextView mEPGEndTime = null;
    private TextView mEPGTime = null;
    private TextView mEPGDate = null;
    private TextView mEPGParental = null;
    private ProgressBar mProgressBarNow = null;
    /** Handler for sending action messages to update UI. */
    private UiHandler mHandler = null;
    /** Buffer for Channel Index, Numeric Channel Change. */
    private StringBuilder mBufferedChannelIndex = null;
    /** Current Channel Info. */
    // private ChannelInfo mChannelInfo = null;
    /** Channel List Dialog. */
    private ChannelListDialog mChannelListDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity);
        mWarningLayout = (RelativeLayout) findViewById(R.id.relativelayout_warning);
        /** Initialize VideoView. */
        initializeVideoView();
        /** Initialize Channel Container. */
        initializeChannelContainer();
        /** Initialize Present/Following View. */
        initializeEPGNowNextView();
        /** Load default IP channel list. */
        initIpChannels();
        /** Initialize Handler. */
        mHandler = new UiHandler();
        /** Initialize String Builder */
        mBufferedChannelIndex = new StringBuilder();
        /** Initialize Time and Date Format. */
        mTimeFormat = new SimpleDateFormat("HH:mm");
        mDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        /** Start DTV. */
        try {
            mDVBManager.startDTV(getLastWatchedChannelIndex());
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
        }
        return super.onOptionsItemSelected(item);
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
        mChannelInfoContainer = (RelativeLayout) findViewById(R.id.linearlayout_channel_info_container);
        mChannelInfoContainer.setVisibility(View.INVISIBLE);
        mChannelNumber = (TextView) findViewById(R.id.textview_channel_number);
        mChannelName = (TextView) findViewById(R.id.textview_channel_name);
    }

    /**
     * Initialize View for Present/Following.
     */
    private void initializeEPGNowNextView() {
        mNowNextContainer = (RelativeLayout) findViewById(R.id.relativelayout_now_next);
        mEPGNow = (TextView) findViewById(R.id.textview_now);
        mEPGNext = (TextView) findViewById(R.id.textview_next);
        mEPGStartTime = (TextView) findViewById(R.id.textview_start_time);
        mEPGEndTime = (TextView) findViewById(R.id.textview_end_time);
        mEPGTime = (TextView) findViewById(R.id.textview_time);
        mEPGDate = (TextView) findViewById(R.id.textview_date);
        mEPGParental = (TextView) findViewById(R.id.textview_parental);
        mProgressBarNow = (ProgressBar) findViewById(R.id.progressbar_now);
    }

    /** Initialize Channel List Dialog. */
    private ChannelListDialog getChannelListDialog() {
        if (mChannelListDialog == null) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mChannelListDialog = new ChannelListDialog(this, mDVBManager,
                    size.x, size.y);
        }
        return mChannelListDialog;
    }

    /**
     * Show Channel Name and Number of Current Channel on Channel Change.
     * 
     * @param channelInfo
     */
    @Override
    public void showChannelInfo() {
        mChannelInfoContainer.setVisibility(View.VISIBLE);
        /** Handle Messages. */
        mHandler.removeMessages(UiHandler.HIDE_VIEW_MESSAGE);
        mHandler.sendEmptyMessageDelayed(UiHandler.HIDE_VIEW_MESSAGE,
                CHANNEL_VIEW_DURATION);
    }

    @Override
    public void setChannelInfo(ChannelInfo channelInfo) {
        if (channelInfo != null) {
            /** Prepare Views. */
            mChannelNumber.setText(String.valueOf(channelInfo.getNumber()));
            mChannelName.setText(channelInfo.getName());
            if (channelInfo.getParental().equals("")) {
                mEPGParental.setVisibility(View.INVISIBLE);
            } else {
                mEPGParental.setText(getResources().getString(
                        R.string.parental, channelInfo.getParental()));
                mEPGParental.setVisibility(View.VISIBLE);
            }
            if (!channelInfo.getEPGNow().equals("")
                    || !channelInfo.getEPGNext().equals("")) {
                if (channelInfo.getProgressPercentPassed() != -1) {
                    mProgressBarNow.setProgress(channelInfo
                            .getProgressPercentPassed());
                    mProgressBarNow.setVisibility(View.VISIBLE);
                } else {
                    mProgressBarNow.setVisibility(View.INVISIBLE);
                }
                mEPGNow.setText(getResources().getString(R.string.epg_now,
                        channelInfo.getEPGNow()));
                mEPGNext.setText(getResources().getString(R.string.epg_next,
                        channelInfo.getEPGNext()));
                mEPGStartTime.setText(getTime(channelInfo.getStartTime()));
                mEPGEndTime.setText(getTime(channelInfo.getEndTime()));
                mNowNextContainer.setVisibility(View.VISIBLE);
            } else {
                mNowNextContainer.setVisibility(View.INVISIBLE);
            }
            try {
                TimeDate lCurrentTime = DVBManager.getInstance()
                        .getCurrentTimeDate();
                mEPGDate.setText(getDate(lCurrentTime.getCalendar().getTime()));
                mEPGTime.setText(getTime(lCurrentTime.getCalendar().getTime()));
            } catch (InternalException e) {
                Log.w(TAG, "There was an Internal Error.", e);
            }
        } else {
            mChannelInfoContainer.setVisibility(View.INVISIBLE);
            mHandler.removeMessages(UiHandler.HIDE_VIEW_MESSAGE);
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
        mNowNextContainer.setVisibility(View.INVISIBLE);
        mEPGParental.setVisibility(View.INVISIBLE);
        mChannelInfoContainer.setVisibility(View.VISIBLE);
        mHandler.removeMessages(UiHandler.NUMERIC_CHANNEL_CHANGE);
        mHandler.sendEmptyMessageDelayed(UiHandler.NUMERIC_CHANNEL_CHANGE,
                NUMERIC_CHANNEL_CHANGE_DURATION);
    }

    /**
     * Show Volume.
     * 
     * @param volume
     */
    private void showVolume(int volume) {
        mProgressBarNow.setProgress(volume);
        mProgressBarNow.setVisibility(View.VISIBLE);
        mEPGNow.setText(R.string.volume);
        mEPGNext.setText("");
        mEPGStartTime.setText(getResources().getString(R.string.volume_percent,
                "" + volume));
        mEPGEndTime.setText("");
        mNowNextContainer.setVisibility(View.VISIBLE);
        mChannelInfoContainer.setVisibility(View.VISIBLE);
        /** Handle Messages. */
        mHandler.removeMessages(UiHandler.HIDE_VIEW_MESSAGE);
        mHandler.sendEmptyMessageDelayed(UiHandler.HIDE_VIEW_MESSAGE,
                CHANNEL_VIEW_DURATION);
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
                getChannelListDialog().show();
                return true;
            }
            /**
             * Change Channel Up (Using of KEYCODE_F4 is just workaround because
             * KeyEvent.KEYCODE_CHANNEL_UP is not mapped on remote control).
             */
            case KeyEvent.KEYCODE_F4:
            case KeyEvent.KEYCODE_CHANNEL_UP: {
                try {
                    setChannelInfo(mDVBManager.changeChannelUp());
                    showChannelInfo();
                } catch (InternalException e) {
                    /** Error with service connection. */
                    Log.e(TAG,
                            "Error with service connection, killing application...!",
                            e);
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
                    setChannelInfo(mDVBManager.changeChannelDown());
                    showChannelInfo();
                } catch (InternalException e) {
                    /** Error with service connection. */
                    Log.e(TAG,
                            "Error with service connection, killing application...!",
                            e);
                }
                return true;
            }
            /** Change Volume and Show It. */
            case KeyEvent.KEYCODE_VOLUME_UP: {
                calculateVolume(VOLUME_UP);
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                calculateVolume(VOLUME_DOWN);
                return true;
            }
            case 91:
            case KeyEvent.KEYCODE_VOLUME_MUTE: {
                calculateVolume(VOLUME_MUTE);
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
            /** Open Channel Info. */
            case KeyEvent.KEYCODE_INFO: {
                try {
                    setChannelInfo(mDVBManager.getChannelInfo(
                            mDVBManager.getCurrentChannelNumber(), false));
                    showChannelInfo();
                } catch (InternalException e) {
                    e.printStackTrace();
                }
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

    private void calculateVolume(int state) {
        int lCurrentVolume = mDVBManager.getCurrentVolume();
        switch (state) {
            case VOLUME_UP: {
                lCurrentVolume++;
                if (lCurrentVolume > 100) {
                    lCurrentVolume = 100;
                }
                mDVBManager.setVolume(lCurrentVolume);
                break;
            }
            case VOLUME_DOWN: {
                lCurrentVolume--;
                if (lCurrentVolume < 0) {
                    lCurrentVolume = 0;
                }
                mDVBManager.setVolume(lCurrentVolume);
                break;
            }
            case VOLUME_MUTE: {
                lCurrentVolume = mDVBManager.setVolumeMute();
                break;
            }
        }
        showVolume(lCurrentVolume);
    }

    /**
     * Convert Formated Time in String.
     */
    private String getTime(Date date) {
        if (date != null) {
            return mTimeFormat.format(date);
        }
        return "";
    }

    /**
     * Convert Formated Date in String.
     */
    private String getDate(Date date) {
        if (date != null) {
            return mDateFormat.format(date);
        }
        return "";
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
                    mChannelInfoContainer.setVisibility(View.INVISIBLE);
                    break;
                }
                case NUMERIC_CHANNEL_CHANGE: {
                    int lChannelNumber = Integer.valueOf(mBufferedChannelIndex
                            .toString());
                    ChannelInfo lChannelInfo = null;
                    try {
                        lChannelInfo = mDVBManager.getChannelInfo(
                                mDVBManager.getCurrentChannelNumber(), false);
                    } catch (InternalException e1) {
                        e1.printStackTrace();
                    }
                    if (lChannelNumber > 0
                            && lChannelNumber <= mDVBManager
                                    .getChannelListSize()) {
                        lChannelNumber--;
                        /** Check for Same Channel. */
                        try {
                            lChannelInfo = mDVBManager
                                    .changeChannelByNumber(lChannelNumber);
                        } catch (InternalException e) {
                            Log.e(TAG,
                                    "There was an Internal Execption on Change Channel.",
                                    e);
                        }
                    } else {
                        Toast.makeText(ZappActivity.this,
                                R.string.non_existing_channel,
                                Toast.LENGTH_SHORT).show();
                    }
                    setChannelInfo(lChannelInfo);
                    showChannelInfo();
                    /** Flush Channel Buffer */
                    mBufferedChannelIndex.delete(0,
                            mBufferedChannelIndex.length());
                    break;
                }
            }
        }
    }
}
